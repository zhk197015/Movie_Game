package service.tmdbApi;

import config.AppConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import model.tmdb.Genre;
import model.tmdb.Movie;
import model.tmdb.MovieCredits;
import model.tmdb.MovieList;
import utils.HttpUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
* TMDB API service
* Only encapsulates TMDB's original API request
 */
@Slf4j
public class TMDBApiService {
    private static final AppConfig CONFIG = AppConfig.getInstance();

    // Test mode flag
    private static boolean testMode = false;
    // Test data
    private static MovieList testMovieList;
    private static Movie testMovieDetails;
    private static MovieCredits testMovieCredits;

    @Getter
    private static String baseUrl = CONFIG.getProperty("tmdb.api.base-url", "https://api" +
            ".themoviedb.org/3");
    private static String discoverMovieUrl = baseUrl + "/discover/movie";
    private static String searchMovieUrl = baseUrl + "/search/movie";
    private static String movieDetailsUrl = baseUrl + "/movie/%d";
    private static String movieCreditsUrl = baseUrl + "/movie/%d/credits";
    private static String genresUrl = baseUrl + "/genre/movie/list";

    /**
     * Set the base URL (for testing only)
     */
    public static void setBaseUrl(String url) {
        baseUrl = url;
        discoverMovieUrl = baseUrl + "/discover/movie";
        searchMovieUrl = baseUrl + "/search/movie";
        movieDetailsUrl = baseUrl + "/movie/%d";
        movieCreditsUrl = baseUrl + "/movie/%d/credits";
        genresUrl = baseUrl + "/genre/movie/list";
    }

    /**
     * Set test mode
    *
    * @param isTestMode Whether it is test mode
    * @param movieList Movie list data for testing
     */
    public static void setTestMode(boolean isTestMode, MovieList movieList) {
        testMode = isTestMode;
        testMovieList = movieList;
    }

    /**
     * Set test details data
    *
    * @param movieDetails movie details
    * @param movieCredits movie cast and crew
     */
    public static void setTestDetailData(Movie movieDetails, MovieCredits movieCredits) {
        testMovieDetails = movieDetails;
        testMovieCredits = movieCredits;
    }

    /**
     * Movie discovery API
    *
    * @param page Page number
    * @param sortBy Sorting method
    * @return Movie list
     */
    public static MovieList discoverMovies(int page, String sortBy) {
        // Directly return test data in test mode
        if (testMode && testMovieList != null) {
            log.info("Test mode: return test data");
            return testMovieList;
        }

        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("accept", "application/json");

            // Constructing URLs with parameters
            String url =
                    String.format("%s?include_adult=false&include_video=false&language=%s" +
                                          "&page=%d&sort_by=%s&api_key=%s", discoverMovieUrl,
                                  CONFIG.getProperty("tmdb.api.language", "en-US"), page, sortBy,
                                  CONFIG.getProperty("tmdb.api.key"));

            String response = HttpUtil.get(url, headers);
            return HttpUtil.fromJson(response, MovieList.class);
        } catch (Exception e) {
            log.error("Getting movie list exception", e);
            return null;
        }
    }

    /**
     * Search movie API
    *
    * @param query Search keyword
    * @param page Page number
* @return Movie list
     */
    public static List<Movie> searchMovies(String query, int page) {
        // Directly return test data in test mode
        if (testMode && testMovieList != null) {
            log.info("Test mode: Returns search test data");
            return testMovieList.getResults();
        }

        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("accept", "application/json");

            // Constructing URLs with parameters
            String url = String.format("%s?query=%s&include_adult=false&language=%s&page=%d" +
                                               "&api_key=%s", searchMovieUrl,
                                       HttpUtil.urlEncode(query), CONFIG.getProperty("tmdb.api.language", "en-US"), page, CONFIG.getProperty("tmdb.api.key"));

            String response = HttpUtil.get(url, headers);
            MovieList movieList = HttpUtil.fromJson(response, MovieList.class);
            return movieList != null ? movieList.getResults() : Collections.emptyList();
        } catch (Exception e) {
            log.error("Search movie exception", e);
            return Collections.emptyList();
        }
    }

    /**
     * Get movie details API
    *
    * @param movieId movie ID
    * @return movie details
     */
    public static Movie getMovieDetails(int movieId) {
        // Directly return test data in test mode
        if (testMode && testMovieDetails != null) {
            log.info("Test mode: Return movie details test data");
            return testMovieDetails;
        }

        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("accept", "application/json");

            
            String url = String.format(movieDetailsUrl + "?language=%s&api_key=%s", movieId,
                                       CONFIG.getProperty("tmdb.api.language", "en-US"),
                                       CONFIG.getProperty("tmdb.api.key"));

            String response = HttpUtil.get(url, headers);
            return HttpUtil.fromJson(response, Movie.class);
        } catch (Exception e) {
            log.error("Exception in getting movie details", e);
            return null;
        }
    }

    /**
    * Get movie cast API
    *
    * @param movieId movie ID
    * @return movie cast
    */
    public static MovieCredits getMovieCredits(int movieId) {
        
        if (testMode && testMovieCredits != null) {
            log.info("Test mode: Returns movie cast and crew test data");
            return testMovieCredits;
        }

        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("accept", "application/json");

            String url = String.format(movieCreditsUrl + "?language=%s&api_key=%s", movieId,
                                       CONFIG.getProperty("tmdb.api.language", "en-US"),
                                       CONFIG.getProperty("tmdb.api.key"));

            String response = HttpUtil.get(url, headers);
            return HttpUtil.fromJson(response, MovieCredits.class);
        } catch (Exception e) {
            log.error("Get movie cast and crew exception", e);
            return null;
        }
    }

    /**
    * Get the movie type list API
    *
    * @return movie type list
    */
    public static List<Genre> getMovieGenres() {
        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("accept", "application/json");

            String url = String.format("%s?language=%s&api_key=%s", genresUrl,
                                       CONFIG.getProperty("tmdb.api.language", "en-US"),
                                       CONFIG.getProperty("tmdb.api.key"));

            String response = HttpUtil.get(url, headers);
            GenreList genreList = HttpUtil.fromJson(response, GenreList.class);
            return genreList != null ? genreList.getGenres() : Collections.emptyList();
        } catch (Exception e) {
            log.error("Exception when getting movie type list", e);
            return Collections.emptyList();
        }
    }

    @Getter
    private static class GenreList {
        private List<Genre> genres;
    }
}
