package service.tmdbApi;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import model.tmdb.Movie;
import model.tmdb.MovieList;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
* TMDB movie cache service
* Responsible for multi-threaded acquisition of movie data and local caching
*/
@Slf4j
public class TMDBMovieCacheService {
    private static final int CACHE_DURATION_HOURS = 24; // Cache update cycle (hours)
    private static final int THREAD_POOL_SIZE = 5; //Thread pool size
    private static final int BATCH_SIZE = 100; // Number of movies processed per thread
    private static final int PAGE_SIZE = 20; //TMDB API default number of pages
    private static final int MAX_RETRIES = 3; // Maximum number of retries
    private static final long RETRY_DELAY = 1000; // Retry delay (ms)
    private static final long REQUEST_DELAY = 200; // Request interval (milliseconds）

    private static final ExecutorService EXECUTOR_SERVICE =
            Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static String cache = "cache";
    private static String cacheFile = cache + "/popular_movies.json";
    private static String lastUpdateFile = cache + "/last_update.txt";

    // Test mode flag
    private static boolean testMode = false;
    // Disable cache flag
    private static boolean disableCache = false;

    /**
    * Set cache directory (for testing)
    */
    public static void setCache(String dir) {
        cache = dir;
        cacheFile = cache + "/popular_movies.json";
        lastUpdateFile = cache + "/last_update.txt";
        // Set to test mode
        testMode = true;
    }

    /**
    * Set test mode
    *
    * @param disableCaching whether to disable caching (direct API access)
    */
    public static void setTestMode(boolean disableCaching) {
        disableCache = disableCaching;
    }

    /**
    * Get a specified number of popular movies (supports caching)
    *
    * @param count The number of movies to be obtained
    * @return Movie list
    */
    public static List<Movie> getPopularMovies(int count) {
        // Get data directly from the API in test mode
        if (testMode) {
            log.info("Test mode: Get data directly from the API");
            return getPopularMoviesFromApi(count);
        }

        // Get directly from the API when caching is disabled
        if (disableCache) {
            log.info("Disable caching: Get data directly from the API");
            return getPopularMoviesFromApi(count);
        }

        // Check the cache directory
        createCacheDirectoryIfNeeded();

        // Check if cache needs to be updated
        if (shouldUpdateCache()) {
            return updateMovieCache(count);
        }

        // Reading from cache
        List<Movie> movies = readFromCache();
        // If the number of cached movies is insufficient, re-acquire
        if (movies.size() < count) {
            return updateMovieCache(count);
        }
        // Returns the specified number of movies
        return movies.size() > count ? movies.subList(0, count) : movies;
    }

    /**
    * Create the cache directory (if it does not exist)
    */
    private static void createCacheDirectoryIfNeeded() {
        try {
            Files.createDirectories(Paths.get(cache));
        } catch (Exception e) {
            log.error("Failed to create cache directory", e);
        }
    }

    /**
    * Check if the cache needs to be updated
    */
    private static boolean shouldUpdateCache() {
        Path lastUpdatePath = Paths.get(lastUpdateFile);
        if (!Files.exists(lastUpdatePath)) {
            return true;
        }

        try {
            String lastUpdateStr = new String(Files.readAllBytes(lastUpdatePath),
                                              StandardCharsets.UTF_8);
            LocalDateTime lastUpdate = LocalDateTime.parse(lastUpdateStr);
            return LocalDateTime
                    .now()
                    .minusHours(CACHE_DURATION_HOURS)
                    .isAfter(lastUpdate);
        } catch (Exception e) {
            log.error("Failed to read the last update time", e);
            return true;
        }
    }

    /**
    * Update movie cache
    */
    private static List<Movie> updateMovieCache(int count) {
        log.info("Starting to update movie cache...");
        List<Movie> movies = fetchMoviesWithThreadPool(count);

        if (!movies.isEmpty()) {
            // Save to cache
            try {
                String json = OBJECT_MAPPER.writeValueAsString(movies);
                Files.write(Paths.get(cacheFile), json.getBytes(StandardCharsets.UTF_8));
                // Update the last updated time
                Files.write(Paths.get(lastUpdateFile), LocalDateTime
                        .now()
                        .toString()
                        .getBytes(StandardCharsets.UTF_8));
                log.info("Movie cache updated successfully, total {} movies", movies.size());
            } catch (Exception e) {
                log.error("Failed to save cache file", e);
            }
        }

        return movies;
    }

    /**
    * Read movie data from cache
    */
    private static List<Movie> readFromCache() {
        try {
            String json = new String(Files.readAllBytes(Paths.get(cacheFile)),
                                     StandardCharsets.UTF_8);
            return OBJECT_MAPPER.readValue(json, new TypeReference<List<Movie>>() {
            });
        } catch (Exception e) {
            log.error("Failed to read cache file", e);
            return new ArrayList<>();
        }
    }

    /**
    * Use thread pool to get movie data
    */
    private static List<Movie> fetchMoviesWithThreadPool(int totalCount) {
        int batchCount = (totalCount + BATCH_SIZE - 1) / BATCH_SIZE;
        List<Future<List<Movie>>> futures = new ArrayList<>();
        AtomicInteger processedCount = new AtomicInteger(0);

        //Submit tasks to the thread pool, each thread processes a different batch
        for (int i = 0; i < batchCount; i++) {
            final int batchIndex = i;
            final int batchSize = Math.min(BATCH_SIZE, totalCount - i * BATCH_SIZE);
            futures.add(EXECUTOR_SERVICE.submit(() -> {
                List<Movie> batchMovies = getPopularMoviesFromApi(batchSize);
                if (batchMovies != null && !batchMovies.isEmpty()) {
                    int current = processedCount.addAndGet(batchMovies.size());
                    log.info("Thread [{}] has obtained the {}th batch of data, currently there are {} movies", Thread
                            .currentThread()
                            .getId(), batchIndex + 1, current);
                }
                return batchMovies != null ? batchMovies : new ArrayList<>();
            }));
        }

        // Collecting Results
        List<Movie> allMovies = new ArrayList<>();
        for (Future<List<Movie>> future : futures) {
            try {
                List<Movie> movies = future.get(5, TimeUnit.MINUTES);
                if (movies != null) {
                    allMovies.addAll(movies);
                }
            } catch (Exception e) {
                log.error("Failed to obtain movie data", e);
            }
        }

        // Sort by popularity and limit the number
        allMovies.sort((m1, m2) -> Double.compare(m2.getPopularity(), m1.getPopularity()));
        return allMovies.size() > totalCount ? allMovies.subList(0, totalCount) : allMovies;
    }

    /**
    * Get the specified number of most popular movies from the API
    *
    * @param count The number of movies to be obtained
    * @return Movie list
    */
    private static List<Movie> getPopularMoviesFromApi(int count) {
        List<Movie> allMovies = new ArrayList<>();
        int totalPages = (count + PAGE_SIZE - 1) / PAGE_SIZE; 
        int currentPage = 1;

        while (currentPage <= totalPages && allMovies.size() < count) {
            MovieList movieList = null;
            // Add a retry mechanism
            for (int retry = 0; retry < MAX_RETRIES; retry++) {
                try {
                    movieList = TMDBApiService.discoverMovies(currentPage, "popularity.desc");
                    if (movieList != null && movieList.getResults() != null && !movieList
                            .getResults()
                            .isEmpty()) {
                        break;
                    }
                    if (retry < MAX_RETRIES - 1) {
                        log.info("{}th retry to get data for page {}...", retry + 1, currentPage);
                        Thread.sleep(RETRY_DELAY);
                    }
                } catch (Exception e) {
                    log.error("The {}th attempt to obtain data for page {} failed", retry + 1, currentPage, e);
                    if (retry < MAX_RETRIES - 1) {
                        try {
                            Thread.sleep(RETRY_DELAY);
                        } catch (InterruptedException ie) {
                            Thread
                                    .currentThread()
                                    .interrupt();
                            break;
                        }
                    }
                }
            }

            if (movieList == null || movieList.getResults() == null || movieList
                    .getResults()
                    .isEmpty()) {
                log.error("Failed to obtain movie data on page {}, retried {} times", currentPage, MAX_RETRIES);
                break;
            }

            // Make sure movies are not added repeatedly
            for (Movie movie : movieList.getResults()) {
                if (!allMovies.contains(movie)) {
                    allMovies.add(movie);
                    if (allMovies.size() >= count) {
                        break;
                    }
                }
            }

            log.info("The movie data of page {} has been obtained, and there are currently {} movies", currentPage, allMovies.size());

            // If enough movies have been fetched or there is no more data, exit the loop
            if (allMovies.size() >= count || currentPage >= movieList.getTotalPages()) {
                break;
            }

            currentPage++;

            // To avoid triggering API throttling, add a short delay
            try {
                Thread.sleep(REQUEST_DELAY);
            } catch (InterruptedException e) {
                Thread
                        .currentThread()
                        .interrupt();
                break;
            }
        }

        // If the number of movies obtained exceeds the specified number, only the specified number will be returned
        return allMovies.size() > count ? allMovies.subList(0, count) : allMovies;
    }
}
