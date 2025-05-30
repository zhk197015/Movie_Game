package model.game;

import lombok.Data;
import model.tmdb.Movie;

/**
 * Movie connection model
 * Indicates the common relationship between two movies (actors, directors, etc.)
 */
@Data
public class Connection {
    // Two movies connected
    private Movie movie1;
    private Movie movie2;

    // Type of connection (actor, director, screenwriter, etc.)
    private String connectionType;

    // Connection value (actor name, director name, etc.)
    private String connectionValue;

    // Connection usage times
    private int usageCount;

    // Connector ID
    private int personId;

    /**
     * Constructor
     */
    public Connection(Movie movie1, Movie movie2, String connectionType, String connectionValue,
                      int personId) {
        this.movie1 = movie1;
        this.movie2 = movie2;
        this.connectionType = connectionType;
        this.connectionValue = connectionValue;
        this.personId = personId;
        this.usageCount = 0;
    }

    /**
     * Increase the number of times used
     */
    public void incrementUsage() {
        this.usageCount++;
    }

    /**
     * Check if the maximum number of uses (3 times) has been reached
     */
    public boolean isMaxUsed() {
        return this.usageCount >= 3;
    }
}
