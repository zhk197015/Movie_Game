package service.movie;

import lombok.extern.slf4j.Slf4j;
import model.tmdb.CastMember;
import model.tmdb.CrewMember;
import model.tmdb.Movie;
import model.tmdb.MovieCredits;
import service.tmdbApi.TMDBApiService;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
* Movie index service
* Responsible for building and maintaining various movie indexes and providing fast query functions
 */
@Slf4j
public class MovieIndexService {
    // Singleton instance
    private static MovieIndexService instance;

    //Title index (prefix -> movie list)
    private final Map<String, Set<Integer>> titlePrefixIndex = new ConcurrentHashMap<>();

    //ID Index (ID -> Movie)
    private final Map<Integer, Movie> idIndex = new ConcurrentHashMap<>();

    // Actor index (actor ID -> movie list)
    private final Map<Integer, Set<Integer>> actorIndex = new ConcurrentHashMap<>();

    // Actor name index (actor name -> actor ID)
    private final Map<String, Integer> actorNameIndex = new ConcurrentHashMap<>();

    // Director index (director ID -> movie list)
    private final Map<Integer, Set<Integer>> directorIndex = new ConcurrentHashMap<>();

    //Director name index (director name -> director ID)
    private final Map<String, Integer> directorNameIndex = new ConcurrentHashMap<>();

    // Movie details cache
    private final Map<Integer, Movie> movieDetailsCache = new ConcurrentHashMap<>();

    // Movie cast and crew cache
    private final Map<Integer, MovieCredits> movieCreditsCache = new ConcurrentHashMap<>();

    /**
     * Private Constructor
     */
    private MovieIndexService() {
    }

    /**
     * Get a singleton instance
     */
    public static synchronized MovieIndexService getInstance() {
        if (instance == null) {
            instance = new MovieIndexService();
        }
        return instance;
    }

    /**
     * Initialize index
     *
     * @param movies Movie List
     */
    public void initializeIndexes(List<Movie> movies) {
        log.info("Start initializing the movie index, the number of movies: {}", movies.size());

        for (Movie movie : movies) {
            // Add to ID Index
            idIndex.put(movie.getId(), movie);

            // Add to title index
            indexMovieTitle(movie);
        }

        log.info("Movie index initialization completed");
    }

    /**
     * Indexing movie titles
     */
    private void indexMovieTitle(Movie movie) {
        String title = movie
                .getTitle()
                .toLowerCase();
        for (int i = 1; i <= title.length(); i++) {
            String prefix = title.substring(0, i);
            titlePrefixIndex
                    .computeIfAbsent(prefix, k -> new HashSet<>())
                    .add(movie.getId());
        }
    }

    /**
     * Index Movie Cast
     */
    public void indexMovieCredits(int movieId, MovieCredits credits) {
        // Cache cast and crew information
        movieCreditsCache.put(movieId, credits);

        // Index Actor
        for (CastMember cast : credits.getCast()) {
            // Add actor name index
            actorNameIndex.putIfAbsent(cast
                                               .getName()
                                               .toLowerCase(), cast.getId());

            // Add actor-movie association
            actorIndex
                    .computeIfAbsent(cast.getId(), k -> new HashSet<>())
                    .add(movieId);
        }

        // Index Director
        for (CrewMember crew : credits.getCrew()) {
            if ("Director".equals(crew.getJob())) {
                // Add director name index
                directorNameIndex.putIfAbsent(crew
                                                      .getName()
                                                      .toLowerCase(), crew.getId());

                // Adding Director-Movie Associations
                directorIndex
                        .computeIfAbsent(crew.getId(), k -> new HashSet<>())
                        .add(movieId);
            }
        }
    }

    /**
     * Search movies by prefix
     */
    public List<Movie> searchByPrefix(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return Collections.emptyList();
        }

        prefix = prefix.toLowerCase();
        Set<Integer> movieIds = titlePrefixIndex.getOrDefault(prefix, Collections.emptySet());

        return movieIds
                .stream()
                .map(idIndex::get)
                .filter(Objects::nonNull)
                .sorted(Comparator
                                .comparing(Movie::getPopularity)
                                .reversed())
                .collect(Collectors.toList());
    }

    /**
     * Get movies by ID
     */
    public Movie getMovieById(int movieId) {
        // First query from the details cache
        Movie movie = movieDetailsCache.get(movieId);
        if (movie != null) {
            return movie;
        }

        // Then query from the ID index
        movie = idIndex.get(movieId);
        if (movie != null) {
            return movie;
        }

        // If none is found, call the API to obtain
        movie = TMDBApiService.getMovieDetails(movieId);
        if (movie != null) {
            // Cache movie details
            movieDetailsCache.put(movieId, movie);
            // Indexing movie titles
            indexMovieTitle(movie);
            // Cache movie ID
            idIndex.put(movieId, movie);

            // Get and index movie cast and crew
            MovieCredits credits = TMDBApiService.getMovieCredits(movieId);
            if (credits != null) {
                indexMovieCredits(movieId, credits);
            }
        }

        return movie;
    }

    /**
     * 获取电影演职人员
     */
    public MovieCredits getMovieCredits(int movieId) {
        // Query from cache first
        MovieCredits credits = movieCreditsCache.get(movieId);
        if (credits != null) {
            return credits;
        }

        // Call API if cache misses
        credits = TMDBApiService.getMovieCredits(movieId);
        if (credits != null) {
            // Index Movie Cast
            indexMovieCredits(movieId, credits);
        }

        return credits;
    }

    /**
     * Get movie list by actor ID
     */
    public List<Movie> getMoviesByActor(int actorId) {
        Set<Integer> movieIds = actorIndex.getOrDefault(actorId, Collections.emptySet());

        return movieIds
                .stream()
                .map(this::getMovieById)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Get movie list by director ID
     */
    public List<Movie> getMoviesByDirector(int directorId) {
        Set<Integer> movieIds = directorIndex.getOrDefault(directorId, Collections.emptySet());

        return movieIds
                .stream()
                .map(this::getMovieById)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Get the actor ID based on the actor name
     */
    public Integer getActorIdByName(String name) {
        return actorNameIndex.get(name.toLowerCase());
    }

    /**
     * Get the director ID based on the director name
     */
    public Integer getDirectorIdByName(String name) {
        return directorNameIndex.get(name.toLowerCase());
    }

    /**
     * Set movie cast and crew information (for testing only)
     *
     * @param movieId Movie ID
     * @param credits Cast and crew information
     */
    public void setMovieCreditsForTest(int movieId, MovieCredits credits) {
        movieCreditsCache.put(movieId, credits);
        indexMovieCredits(movieId, credits);
    }

    /**
     * Clear all indexes
     */
    public void clearIndexes() {
        titlePrefixIndex.clear();
        idIndex.clear();
        actorIndex.clear();
        actorNameIndex.clear();
        directorIndex.clear();
        directorNameIndex.clear();
        movieDetailsCache.clear();
        movieCreditsCache.clear();
    }
}
