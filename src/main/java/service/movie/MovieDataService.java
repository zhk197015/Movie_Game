package service.movie;

import model.game.Connection;
import model.game.GameSession;
import model.game.WinCondition;
import model.tmdb.Movie;
import model.tmdb.MovieCredits;

import java.util.List;

/**
* Movie data service interface
* Provides all movie data related functions required by the game
 */
public interface MovieDataService {
    /**
     * Get the initial list of movies
     *
     * @return Top 5000 popular movies list
     */
    List<Movie> getInitialMoviesList();

    /**
     * Get a random starting movie
     *
     * @return A randomly selected starting movie
     */
    Movie getRandomStarterMovie();

    /**
     * Search movies by prefix
     *
     * @param selectedTitle Movie title prefix
     * @return List of matching movies
     */
    List<Movie> searchMoviesByPrefix(String selectedTitle);

    /**
     * Get movie details by ID
     *
     * @param movieId Movie ID
     * @return Movie Details
     */
    Movie getMovieById(int movieId);

    /**
     * Verify that there is a valid connection between two movies
     *
     * @param previousMovie Previous Movie
     * @param currentMovie  Current Movies
     * @return Is there a valid connection?
     */
    boolean validateConnection(Movie previousMovie, Movie currentMovie);

    /**
     * Get all connections between two movies
     *
     * @param previousMovie Previous Movie
     * @param currentMovie  Current Movies
     * @return Connection List
     */
    List<Connection> getConnections(Movie previousMovie, Movie currentMovie);

    /**
    * Check if the connection has been used three times
    *
    * @param connection connection
    * @param session game session
    * @return whether it has been used three times
     */
    boolean isConnectionUsedThreeTimes(Connection connection, GameSession session);

    /**
    * Check if the movie has been used in the game
    *
    * @param movie movie
    * @param session game session
    * @return whether it has been used
     */
    boolean isMovieAlreadyUsed(Movie movie, GameSession session);

    /**
    * Check if the movie meets the victory condition
    *
    * @param movie movie
    * @param condition victory condition
    * @return whether the condition is met
     */
    boolean matchesWinCondition(Movie movie, WinCondition condition);

    /**
    * Register the used movie
    *
    * @param movie movie
    * @param session game session
    */
    void registerUsedMovie(Movie movie, GameSession session);

    /**
    * Register the used connection
    *
    * @param connection connection
    * @param session game session
    */
    void registerUsedConnection(Connection connection, GameSession session);

    /**
    * Initialize data index
    */
    void initializeDataIndexes();
    
    MovieCredits getMovieCredits(int movieId);

}
