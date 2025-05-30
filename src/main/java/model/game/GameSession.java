package model.game;

import lombok.Data;
import model.tmdb.Movie;
// Edit: Import WinCondition
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Game Session Model
 */
@Data
public class GameSession {
    // Session ID
    private String sessionId;

    // List of used movies
    private List<Movie> usedMovies;

    // List of used connections (including usage count)
    private List<Connection> usedConnections;

    // Connection usage count (by personId)
    private Map<Integer, Integer> connectionUsageCount;

    // Current Movies
    private Movie currentMovie;

    // Current game steps
    private int currentStep;

    // Setting the stage (first 3 steps) flag
    private boolean inSetupPhase;
    
    // Edit: Win condition for the current game
//    private WinCondition winCondition;
    private WinCondition player1WinCondition;
    private WinCondition player2WinCondition;
    
    private String player1Name;
    private String player2Name;
    private boolean isPlayer1Turn;
    
    private final List<HistoryRecord> recentHistory = new LinkedList<>();


    /**
     * Constructor
     */
    public GameSession(String sessionId, Movie startMovie, WinCondition player1WinCondition, WinCondition player2WinCondition, String player1Name, String player2Name) {
        this.sessionId = sessionId;
        this.currentMovie = startMovie;
        this.usedMovies = new ArrayList<>();
        this.usedMovies.add(startMovie);
        this.usedConnections = new ArrayList<>();
        this.connectionUsageCount = new HashMap<>();
        this.currentStep = 1;
        this.inSetupPhase = true;
        this.player1WinCondition = player1WinCondition;
        this.player2WinCondition = player2WinCondition;
        this.player1Name = player1Name;
        this.player2Name = player2Name;
        this.isPlayer1Turn = true;
    }

    /**
     * Movies registered for use
     */
    public void registerUsedMovie(Movie movie) {
        if (!isMovieAlreadyUsed(movie)) {
            this.usedMovies.add(movie);
        }
        this.currentMovie = movie;
        this.currentStep++;

        // Check if the setup phase has been exceeded (3 steps)
        if (this.currentStep > 3) {
            this.inSetupPhase = false;
        }
    }

    /**
     * Register the connection used
     */
    public void registerUsedConnection(Connection connection) {
        // Increase the number of connections used
        connection.incrementUsage();
        this.usedConnections.add(connection);

        // Update personId usage count
        int personId = connection.getPersonId();
        int count = this.connectionUsageCount.getOrDefault(personId, 0);
        this.connectionUsageCount.put(personId, count + 1);
    }

    /**
     * Check if the movie is already used
     */
    public boolean isMovieAlreadyUsed(Movie movie) {
        return usedMovies
                .stream()
                .anyMatch(m -> m.getId() == movie.getId());
    }

    /**
     * Check if the connection has been used three times
     */
    public boolean isConnectionUsedThreeTimes(int personId) {
        return this.connectionUsageCount.getOrDefault(personId, 0) >= 3;
    }
        
    public WinCondition getCurrentPlayerWinCondition() {
        return isPlayer1Turn ? player1WinCondition : player2WinCondition;
    }
    

    public boolean hasWon() {
        return getCurrentPlayerWinCondition().isAchieved();
    }
    
    public String getCurrentPlayerName() {
        return isPlayer1Turn ? player1Name : player2Name;
    }

    public void switchTurn() {
        this.isPlayer1Turn = !isPlayer1Turn;
    }
    
    public void addInitialMovieToHistory(Movie movie) {
        recentHistory.add(new HistoryRecord(movie, null));
    }
    
    public void addToHistory(Movie movie, Connection connection) {
        if (recentHistory.size() == 5) {
            recentHistory.remove(0);  // Keep at most 5 history records
        }
        recentHistory.add(new HistoryRecord(movie, connection));
    }

    public List<HistoryRecord> getRecentHistory() {
        return new ArrayList<>(recentHistory);
    }
    public WinCondition getPlayer1WinCondition() {
        return player1WinCondition;
    }

    public WinCondition getPlayer2WinCondition() {
        return player2WinCondition;
    }



}
