package service.tmdbApi;

import config.AppConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import model.tmdb.Movie;

import java.util.List;

/**
* TMDB movie service class
* Business layer service, responsible for connecting cache service and API service
*/
@Slf4j
public class TMDBMovieService {
    public static final int MAX_MOVIES = 10;
    private static final AppConfig CONFIG = AppConfig.getInstance();
    @Getter
    private static String baseUrl = CONFIG.getProperty("tmdb.api.base-url", "https://api" +
            ".themoviedb.org/3");

    /**
    * Set the base URL (for testing only)
    */
    public static void setBaseUrl(String url) {
        baseUrl = url;
        TMDBApiService.setBaseUrl(url);
    }

    /**
    * Get the top 5000 most popular movies
    *
    * @return movie list
    */
    public static List<Movie> getTop5000PopularMovies() {
        return TMDBMovieCacheService.getPopularMovies(MAX_MOVIES);
    }

    /**
    * Get the specified number of most popular movies (for testing only)
    *
    * @param count The number of movies to be obtained
    * @return Movie list
    */
    public static List<Movie> getPopularMovies(int count) {
        return TMDBMovieCacheService.getPopularMovies(count);
    }
}
