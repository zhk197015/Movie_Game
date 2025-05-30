package model.game;

import model.tmdb.Movie;

public class HistoryRecord {
    private final Movie movie;
    private final Connection connection;

    public HistoryRecord(Movie movie, Connection connection) {
        this.movie = movie;
        this.connection = connection;
    }

    public Movie getMovie() {
        return movie;
    }

    public Connection getConnection() {
        return connection;
    }
}
