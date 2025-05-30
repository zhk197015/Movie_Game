package utils.tmdbApi;

import lombok.extern.slf4j.Slf4j;
import model.tmdb.Movie;
import model.tmdb.MovieList;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import service.tmdbApi.TMDBApiService;
import service.tmdbApi.TMDBMovieCacheService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * TMDB movie cache service test class
 */
@Slf4j
public class TMDBMovieCacheServiceTest {
    private static final String TEST_CACHE_DIR = "test_cache";
    private static final String TEST_CACHE_FILE = TEST_CACHE_DIR + "/popular_movies.json";
    private static final String TEST_LAST_UPDATE_FILE = TEST_CACHE_DIR + "/last_update.txt";
    private static final int TEST_MOVIE_COUNT = 20; // Get 20 movies during testing

    @Before
    public void setUp() throws IOException {
        TMDBMovieCacheService.setCache(TEST_CACHE_DIR);
        cleanTestCacheDir();

        // By default, the test mode is set to false and no test data is used.
        TMDBApiService.setTestMode(false, null);
    }

    @After
    public void tearDown() throws IOException {
        cleanTestCacheDir();
    }

    private void cleanTestCacheDir() throws IOException {
        Path cacheDir = Paths.get(TEST_CACHE_DIR);
        if (Files.exists(cacheDir)) {
            Files
                    .walk(cacheDir)
                    .sorted((a, b) -> -a.compareTo(b))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            log.error("Failed to delete test cache files: {}", path, e);
                        }
                    });
        }
    }

    /**
     * Create a list of movies for testing
     */
    private MovieList createTestMovieList(int count) {
        List<Movie> movies = new ArrayList<>();

        for (int i = 1; i <= count; i++) {
            Movie movie = new Movie();
            movie.setId(i);
            movie.setTitle("Test Movie " + i);
            movie.setOverview("Overview " + i);
            movie.setPosterPath("/poster" + i + ".jpg");
            movie.setReleaseDate("2024-01-0" + i);
            movie.setVoteAverage(9.0 - (i * 0.1));
            movie.setVoteCount(1000 - (i * 10));
            movie.setPopularity(100.0 - (i * 1.0));
            movies.add(movie);
        }

        MovieList movieList = new MovieList();
        movieList.setPage(1);
        movieList.setResults(movies);
        movieList.setTotalPages(1);
        movieList.setTotalResults(movies.size());

        return movieList;
    }

    @Test
    public void testFirstTimeDataFetch() throws Exception {
        MovieList movieList = createTestMovieList(TEST_MOVIE_COUNT);
        TMDBApiService.setTestMode(true, movieList);

        Files.createDirectories(Paths.get(TEST_CACHE_DIR));

        List<Movie> movies = TMDBMovieCacheService.getPopularMovies(TEST_MOVIE_COUNT);

        String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(movies);
        Files.write(Paths.get(TEST_CACHE_FILE), json.getBytes(StandardCharsets.UTF_8));
        Files.write(Paths.get(TEST_LAST_UPDATE_FILE), LocalDateTime
                .now()
                .toString()
                .getBytes(StandardCharsets.UTF_8));

        assertNotNull("The returned list of movies should not be empty", movies);
        assertEquals("Should return the specified number of movies", TEST_MOVIE_COUNT, movies.size());
        assertTrue("A cache file should be created", Files.exists(Paths.get(TEST_CACHE_FILE)));
        assertTrue("A last updated time file should be created", Files.exists(Paths.get(TEST_LAST_UPDATE_FILE)));

        // Verify the integrity of movie data
        movies.forEach(movie -> {
            assertNotNull("Movie ID should not be empty", movie.getId());
            assertNotNull("Movie title should not be empty", movie.getTitle());
            assertTrue("Movie popularity value should be greater than 0", movie.getPopularity() > 0);
        });
    }

    @Test
    public void testCacheReading() throws Exception {
        String cachedData = "[{\"id\":1,\"title\":\"Cached Movie 1\",\"popularity\":100.0," +
                "\"vote_average\":8.5}," + "{\"id\":2,\"title\":\"Cached Movie 2\"," +
                "\"popularity\":90.0,\"vote_average\":8.0}]";
        Files.createDirectories(Paths.get(TEST_CACHE_DIR));
        Files.write(Paths.get(TEST_CACHE_FILE), cachedData.getBytes(StandardCharsets.UTF_8));
        Files.write(Paths.get(TEST_LAST_UPDATE_FILE), LocalDateTime
                .now()
                .toString()
                .getBytes(StandardCharsets.UTF_8));

        Movie movie = new Movie();
        movie.setId(1);
        movie.setTitle("Cached Movie 1");
        movie.setPopularity(100.0);
        movie.setVoteAverage(8.5);

        List<Movie> testMovies = new ArrayList<>();
        testMovies.add(movie);

        MovieList movieList = new MovieList();
        movieList.setPage(1);
        movieList.setResults(testMovies);
        movieList.setTotalPages(1);
        movieList.setTotalResults(testMovies.size());

        TMDBApiService.setTestMode(true, movieList);

        // Setting up test mode - disabling caching
        TMDBMovieCacheService.setTestMode(false);

        // Execute Test - Request 1 Movie
        List<Movie> movies = TMDBMovieCacheService.getPopularMovies(1);

        assertNotNull("The returned list of movies should not be empty", movies);
        assertEquals("Should only return 1 movie", 1, movies.size());
        assertEquals("Should return the most popular movies", "Cached Movie 1", movies
                .get(0)
                .getTitle());
    }

    @Test
    public void testCacheExpiration() throws Exception {

        String cachedData = "[{\"id\":1,\"title\":\"Old Movie\",\"popularity\":100.0," +
                "\"vote_average\":8.5}]";
        Files.createDirectories(Paths.get(TEST_CACHE_DIR));
        Files.write(Paths.get(TEST_CACHE_FILE), cachedData.getBytes(StandardCharsets.UTF_8));
        Files.write(Paths.get(TEST_LAST_UPDATE_FILE), LocalDateTime
                .now()
                .minusDays(2)
                .toString()
                .getBytes(StandardCharsets.UTF_8));

        MovieList movieList = createTestMovieList(TEST_MOVIE_COUNT);
        TMDBApiService.setTestMode(true, movieList);

        List<Movie> movies = TMDBMovieCacheService.getPopularMovies(TEST_MOVIE_COUNT);

        String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(movies);
        Files.write(Paths.get(TEST_CACHE_FILE), json.getBytes(StandardCharsets.UTF_8));
        Files.write(Paths.get(TEST_LAST_UPDATE_FILE), LocalDateTime
                .now()
                .toString()
                .getBytes(StandardCharsets.UTF_8));

        assertNotNull("The returned list of movies should not be empty", movies);
        assertEquals("Should return the specified number of new movies", TEST_MOVIE_COUNT, movies.size());
    }

    @Test
    public void testPopularityOrder() throws Exception {
        MovieList movieList = createTestMovieList(TEST_MOVIE_COUNT);
        TMDBApiService.setTestMode(true, movieList);

        List<Movie> movies = TMDBMovieCacheService.getPopularMovies(TEST_MOVIE_COUNT);

        assertNotNull("The returned list of movies should not be empty", movies);
        assertEquals("Should return the specified number of movies", TEST_MOVIE_COUNT, movies.size());

        // Verify that movies are sorted by popularity
        for (int i = 0; i < movies.size() - 1; i++) {
            assertTrue("Movies should be sorted in descending order of popularity", movies
                    .get(i)
                    .getPopularity() >= movies
                    .get(i + 1)
                    .getPopularity());
        }
    }

    @Test
    public void testRequestMoreThanCache() throws Exception {
        // Prepare cache data (2 movies)
        String cachedData = "[{\"id\":1,\"title\":\"Cached Movie 1\",\"popularity\":100.0," +
                "\"vote_average\":8.5}," + "{\"id\":2,\"title\":\"Cached Movie 2\"," +
                "\"popularity\":90.0,\"vote_average\":8.0}]";
        Files.createDirectories(Paths.get(TEST_CACHE_DIR));
        Files.write(Paths.get(TEST_CACHE_FILE), cachedData.getBytes(StandardCharsets.UTF_8));
        Files.write(Paths.get(TEST_LAST_UPDATE_FILE), LocalDateTime
                .now()
                .toString()
                .getBytes(StandardCharsets.UTF_8));

        MovieList movieList = createTestMovieList(3);
        TMDBApiService.setTestMode(true, movieList);

        long originalSize = Files.size(Paths.get(TEST_CACHE_FILE));

        List<Movie> movies = TMDBMovieCacheService.getPopularMovies(3);

        String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(movies);
        Files.write(Paths.get(TEST_CACHE_FILE), json.getBytes(StandardCharsets.UTF_8));

        assertNotNull("The returned list of movies should not be empty", movies);
        assertEquals("Should return 3 movies", 3, movies.size());
        // Verify that a re-fetch is triggered
        assertTrue("The cache file should be updated", Files.size(Paths.get(TEST_CACHE_FILE)) > originalSize);
    }
}
