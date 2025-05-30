package utils.tmdbApi;

import lombok.extern.slf4j.Slf4j;
import model.tmdb.Movie;
import org.junit.Test;
import service.tmdbApi.TMDBMovieService;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.Assert.*;

/**
* TMDB movie service integration test
* Test the entire service chain, including API calls, caching, multi-threading and other functions
 */
@Slf4j
public class TMDBMovieIntegrationTest {
    private static final int DEMO_MOVIE_COUNT = 20; 

    @Test
    public void testCompleteFlow() {
        log.info("Start testing the complete process...");

        // First acquisition (acquired from API and cached)
        Instant start = Instant.now();
        List<Movie> movies = TMDBMovieService.getTop5000PopularMovies();
        Duration duration = Duration.between(start, Instant.now());

        // Verify the first acquisition result
        assertNotNull("The returned list of movies should not be empty", movies);
        assertFalse("Should return movie data", movies.isEmpty());
        log.info("The first acquisition is complete! Time taken: {} seconds, a total of {} movies were acquired", duration.getSeconds(), movies.size());

        printTopMovies(movies, DEMO_MOVIE_COUNT);

        // Second fetch (read from cache)
        log.info("\nStart second fetch (should read from cache）...");
        start = Instant.now();
        List<Movie> cachedMovies = TMDBMovieService.getTop5000PopularMovies();
        duration = Duration.between(start, Instant.now());

        // Verify the second acquisition result
        assertNotNull("The cached movie list should not be empty", cachedMovies);
        assertEquals("The cache should return the same number of movies", movies.size(), cachedMovies.size());
        assertTrue("Reading from cache should be faster", duration.getSeconds() < 1);
        log.info("The second acquisition is complete! Time taken: {} seconds, a total of {} movies were acquired", duration.getSeconds(), cachedMovies.size());

        // Verify the integrity of movie data
        validateMovieData(movies);
    }

    @Test
    public void testPerformance() {
        log.info("Starting performance testing...");

        // Pre-warming the cache
        TMDBMovieService.getTop5000PopularMovies();

        // Test cache read performance 10 times
        for (int i = 0; i < 10; i++) {
            Instant start = Instant.now();
            List<Movie> movies = TMDBMovieService.getTop5000PopularMovies();
            Duration duration = Duration.between(start, Instant.now());

            assertNotNull("The returned list of movies should not be empty", movies);
            assertTrue("Cache reads should be fast", duration.toMillis() < 1000);
            log.info("The {}th read took: {} milliseconds", i + 1, duration.toMillis());
        }
    }

    /**
     * Verify the integrity of movie data
     */
    private void validateMovieData(List<Movie> movies) {
        movies.forEach(movie -> {
            assertNotNull("Movie ID should not be empty", movie.getId());
            assertNotNull("Movie title should not be empty", movie.getTitle());
            assertTrue("Movie popularity value should be greater than 0", movie.getPopularity() > 0);
            assertTrue("Movie ratings should be between 0-10", movie.getVoteAverage() >= 0 && movie.getVoteAverage() <= 10);
            assertTrue("The number of ratings should be greater than or equal to 0", movie.getVoteCount() >= 0);
        });
    }

    /**
     * Print movie information
     */
    private void printTopMovies(List<Movie> movies, int count) {
        log.info("\\nPrevious {} movie information：", count);
        int limit = Math.min(count, movies.size());
        for (int i = 0; i < limit; i++) {
            Movie movie = movies.get(i);
            log.info("\nMovie #{}", i + 1);
            log.info("ID: {}", movie.getId());
            log.info("title: {}", movie.getTitle());
            log.info("Popularity: {}", movie.getPopularity());
            log.info("score: {} ({} Ratings)", movie.getVoteAverage(), movie.getVoteCount());
            log.info("Release Date: {}", movie.getReleaseDate());
            log.info("Introduction: {}", movie.getOverview());
        }
    }
}
