package utils.tmdbApi;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import model.tmdb.Movie;
import model.tmdb.MovieList;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import service.tmdbApi.TMDBApiService;
import service.tmdbApi.TMDBMovieCacheService;
import service.tmdbApi.TMDBMovieService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * TMDB Movie Service Test Category
 */
@Slf4j
public class TMDBMovieServiceTest {
    private MockWebServer mockWebServer;
    private String originalBaseUrl;

    @Before
    public void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        originalBaseUrl = TMDBMovieService.getBaseUrl();
        TMDBMovieService.setBaseUrl("http://" + mockWebServer.getHostName() + ":" + mockWebServer.getPort());

        TMDBMovieCacheService.setCache("test_cache");
    }

    @After
    public void tearDown() throws IOException {
        mockWebServer.shutdown();
        TMDBMovieService.setBaseUrl(originalBaseUrl);
    }

    @Test
    public void testGetPopularMovies() throws Exception {
        List<Movie> moviesList = createTestMovies();

        MovieList movieList = new MovieList();
        movieList.setPage(1);
        movieList.setResults(moviesList);
        movieList.setTotalPages(1);
        movieList.setTotalResults(moviesList.size());

        TMDBApiService.setTestMode(true, movieList);

        List<Movie> movies = TMDBMovieService.getPopularMovies(3);

        if (movies != null && !movies.isEmpty()) {
            log.error("The actual number of movies returned: {}", movies.size());
            for (Movie movie : movies) {
                log.error("Movie ID: {}, Title: {}, Popularity: {}", movie.getId(), movie.getTitle(),
                          movie.getPopularity());
            }
        }

        assertNotNull(movies);
        assertEquals(3, movies.size());

        Movie firstMovie = movies.get(0);
        assertEquals(1, firstMovie.getId());
        assertEquals("Movie 1", firstMovie.getTitle());
        assertEquals(8.5, firstMovie.getVoteAverage(), 0.001);
        assertEquals(100.0, firstMovie.getPopularity(), 0.001);

        Movie lastMovie = movies.get(2);
        assertEquals(3, lastMovie.getId());
        assertEquals("Movie 3", lastMovie.getTitle());
        assertEquals(7.5, lastMovie.getVoteAverage(), 0.001);
        assertEquals(80.0, lastMovie.getPopularity(), 0.001);
    }

    /**
     * Create a list of movies for testing
     */
    private List<Movie> createTestMovies() {
        List<Movie> movies = new ArrayList<>();

        Movie movie1 = new Movie();
        movie1.setId(1);
        movie1.setTitle("Movie 1");
        movie1.setOverview("Overview 1");
        movie1.setPosterPath("/poster1.jpg");
        movie1.setReleaseDate("2024-01-01");
        movie1.setVoteAverage(8.5);
        movie1.setVoteCount(1000);
        movie1.setPopularity(100.0);

        Movie movie2 = new Movie();
        movie2.setId(2);
        movie2.setTitle("Movie 2");
        movie2.setOverview("Overview 2");
        movie2.setPosterPath("/poster2.jpg");
        movie2.setReleaseDate("2024-01-02");
        movie2.setVoteAverage(8.0);
        movie2.setVoteCount(900);
        movie2.setPopularity(90.0);

        Movie movie3 = new Movie();
        movie3.setId(3);
        movie3.setTitle("Movie 3");
        movie3.setOverview("Overview 3");
        movie3.setPosterPath("/poster3.jpg");
        movie3.setReleaseDate("2024-01-03");
        movie3.setVoteAverage(7.5);
        movie3.setVoteCount(800);
        movie3.setPopularity(80.0);

        movies.add(movie1);
        movies.add(movie2);
        movies.add(movie3);

        return movies;
    }
}
