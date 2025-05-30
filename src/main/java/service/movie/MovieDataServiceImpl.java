package service.movie;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import model.game.Connection;
import model.game.GameSession;
import model.game.WinCondition;
import model.tmdb.CastMember;
import model.tmdb.CrewMember;
import model.tmdb.Movie;
import model.tmdb.MovieCredits;
import service.tmdbApi.TMDBApiService;
import service.tmdbApi.TMDBMovieCacheService;

import java.util.*;
import java.util.stream.Collectors;

/**
* Movie data service implementation class
* Implement all movie data related functions required by the game
 */
@Slf4j
public class MovieDataServiceImpl implements MovieDataService {
    // Number of test data movies
    private static final int TEST_MOVIES_LIMIT = 20;
    // Singleton Instance
    private static MovieDataServiceImpl instance;
    /**
     * -- SETTER --
     * Setting the test mode
     */
    // Test mode flag
    @Setter
    private static boolean testMode = false;
    // Movie Indexing Service
    private final MovieIndexService indexService;
    // Random Number Generator
    private final Random random = new Random();
    // Initial movie list (Top 5000)
    private List<Movie> initialMoviesList;
    // Starter Movie List (daily updated starter movies)
    private List<Movie> starterMovies;

    /**
     * Private Constructor
     */
    private MovieDataServiceImpl() {
        this.indexService = MovieIndexService.getInstance();
        // Loading the initial movie list
        loadInitialMovies();
    }

    /**
     * Get a singleton instance
     */
    public static synchronized MovieDataServiceImpl getInstance() {
        if (instance == null) {
            instance = new MovieDataServiceImpl();
        }
        return instance;
    }

    /**
     * Loading the initial movie list
     */
    private void loadInitialMovies() {
        // Determine the number of movies to load
        int moviesCount = testMode ? TEST_MOVIES_LIMIT : 5000;

        //Get popular movies from cache service
        initialMoviesList = TMDBMovieCacheService.getPopularMovies(moviesCount);
        log.info("{} initial movies loaded", initialMoviesList.size());

        // Initialize the movie index
        indexService.initializeIndexes(initialMoviesList);

        // Preload launcher movie list (currently uses first 20 movies or all as examples)
        int starterLimit = 20;
        starterMovies = initialMoviesList
                .stream()
                .limit(starterLimit)
                .collect(Collectors.toList());
    }

    @Override
    public List<Movie> getInitialMoviesList() {
        return Collections.unmodifiableList(initialMoviesList);
    }

    @Override
    public Movie getRandomStarterMovie() {
            // If the starter movie is empty, return the first movie in the initial list
            int start = random.nextInt(1000);
            return initialMoviesList.get(start);
        }

    @Override
    public List<Movie> searchMoviesByPrefix(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return Collections.emptyList();
        }

        // Search from index
        List<Movie> results = indexService.searchByPrefix(prefix);

        // If there are no results in the local index, call the API to search
        if (results.isEmpty()) {
            results = TMDBApiService.searchMovies(prefix, 1);

            // Add to Index
//            for (Movie movie : results) {
//                indexService.getMovieById(movie.getId());
//            }
        }

        return results;
    }

    @Override
    public Movie getMovieById(int movieId) {
        return indexService.getMovieById(movieId);
    }


    @Override
    public boolean validateConnection(Movie previousMovie, Movie currentMovie) {
        if (previousMovie == null || currentMovie == null) {
            return false;
        }

        // Get the connection between two movies
        List<Connection> connections = getConnections(previousMovie, currentMovie);

        // If there is a connection, the verification is successful
        return !connections.isEmpty();
    }

    @Override
    public List<Connection> getConnections(Movie previousMovie, Movie currentMovie) {
        if (previousMovie == null || currentMovie == null) {
            return Collections.emptyList();
        }

        List<Connection> connections = new ArrayList<>();

        // Get movie cast and crew information
        MovieCredits previousCredits = indexService.getMovieCredits(previousMovie.getId());
        MovieCredits currentCredits = indexService.getMovieCredits(currentMovie.getId());

        if (previousCredits == null || currentCredits == null) {
            return Collections.emptyList();
        }

        // Check co-actors
        Map<Integer, CastMember> previousCastMap = previousCredits
                .getCast()
                .stream()
                .collect(Collectors.toMap(CastMember::getId, cast -> cast));

        for (CastMember currentCast : currentCredits.getCast()) {
            if (previousCastMap.containsKey(currentCast.getId())) {
                CastMember previousCast = previousCastMap.get(currentCast.getId());
                connections.add(new Connection(previousMovie, currentMovie, "actor",
                                               previousCast.getName(), previousCast.getId()));
            }
        }

        // Check Co-Director
        Map<Integer, CrewMember> previousDirectorsMap = previousCredits
                .getCrew()
                .stream()
                .filter(crew -> "Director".equals(crew.getJob()))
                .collect(Collectors.toMap(CrewMember::getId, crew -> crew));

        for (CrewMember currentCrew : currentCredits.getCrew()) {
            if ("Director".equals(currentCrew.getJob()) && previousDirectorsMap.containsKey(currentCrew.getId())) {
                CrewMember previousDirector = previousDirectorsMap.get(currentCrew.getId());
                connections.add(new Connection(previousMovie, currentMovie, "director",
                                               previousDirector.getName(),
                                               previousDirector.getId()));
            }
        }

        // Check Co-Writers
        Map<Integer, CrewMember> previousWritersMap = previousCredits
                .getCrew()
                .stream()
                .filter(crew -> "Writer".equals(crew.getJob()) || "Screenplay".equals(crew.getJob()))
                .collect(Collectors.toMap(CrewMember::getId, crew -> crew, (a, b) -> a));

        for (CrewMember currentCrew : currentCredits.getCrew()) {
            if (("Writer".equals(currentCrew.getJob()) || "Screenplay".equals(currentCrew.getJob())) && previousWritersMap.containsKey(currentCrew.getId())) {
                CrewMember previousWriter = previousWritersMap.get(currentCrew.getId());
                connections.add(new Connection(previousMovie, currentMovie, "screenwriter",
                                               previousWriter.getName(), previousWriter.getId()));
            }
        }

        return connections;
    }

    @Override
    public boolean isConnectionUsedThreeTimes(Connection connection, GameSession session) {
        return session.isConnectionUsedThreeTimes(connection.getPersonId());
    }

    @Override
    public boolean isMovieAlreadyUsed(Movie movie, GameSession session) {
        return session.isMovieAlreadyUsed(movie);
    }

    @Override
    public boolean matchesWinCondition(Movie movie, WinCondition condition) {
        if (movie == null || condition == null) {
            return false;
        }

        String type = condition.getConditionType();
        String value = condition.getConditionValue();

        switch (type.toLowerCase()) {
            case "genre":
                // Check movie genre
                return checkMovieGenre(movie, value);

            case "actor":
                // Check the actors
                return checkMovieActor(movie, value);

            case "director":
                // Check Director
                return checkMovieDirector(movie, value);

            case "writer":
                // Check the screenwriter
                return checkMovieWriter(movie, value);

            default:
                return false;
        }
    }

    /**
     * Check if the movie genre matches
     */
    private boolean checkMovieGenre(Movie movie, String genreName) {
        return MovieGenreService
                .getInstance()
                .hasGenre(movie.getGenreIds(), genreName);
    }

    /**
     * Check if movie actors match
     */
    private boolean checkMovieActor(Movie movie, String actorName) {
        MovieCredits credits = indexService.getMovieCredits(movie.getId());
        if (credits == null) {
            return false;
        }

        return credits
                .getCast()
                .stream()
                .anyMatch(cast -> cast
                        .getName()
                        .equalsIgnoreCase(actorName));
    }

    /**
     * Check if the movie director matches
     */
    private boolean checkMovieDirector(Movie movie, String directorName) {
        MovieCredits credits = indexService.getMovieCredits(movie.getId());
        if (credits == null) {
            return false;
        }

        return credits
                .getCrew()
                .stream()
                .filter(crew -> "Director".equals(crew.getJob()))
                .anyMatch(director -> director
                        .getName()
                        .equalsIgnoreCase(directorName));
    }

    /**
     * Check if the movie screenwriter matches
     */
    private boolean checkMovieWriter(Movie movie, String writerName) {
        MovieCredits credits = indexService.getMovieCredits(movie.getId());
        if (credits == null) {
            return false;
        }

        return credits
                .getCrew()
                .stream()
                .filter(crew -> "Writer".equals(crew.getJob()) || "Screenplay".equals(crew.getJob()))
                .anyMatch(writer -> writer
                        .getName()
                        .equalsIgnoreCase(writerName));
    }

    @Override
    public void registerUsedMovie(Movie movie, GameSession session) {
        session.registerUsedMovie(movie);
    }

    @Override
    public void registerUsedConnection(Connection connection, GameSession session) {
        session.registerUsedConnection(connection);
    }

    @Override
    public void initializeDataIndexes() {
        // Reinitialize the index, which can be called when needed
        if (initialMoviesList != null && !initialMoviesList.isEmpty()) {
            log.info("Reinitializing movie index...");
            indexService.initializeIndexes(initialMoviesList);
        } else {
            log.warn("Failed to initialize index: Movie list is empty");
            // Reload the initial movie list
            loadInitialMovies();
        }
    }
    
    @Override
    public MovieCredits getMovieCredits(int movieId) {
        return indexService.getMovieCredits(movieId);
    }

}
