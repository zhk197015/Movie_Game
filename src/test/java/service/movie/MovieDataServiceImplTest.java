package service.movie;

import lombok.extern.slf4j.Slf4j;
import model.game.Connection;
import model.game.GameSession;
import model.game.WinCondition;
import model.tmdb.CastMember;
import model.tmdb.CrewMember;
import model.tmdb.Movie;
import model.tmdb.MovieCredits;
import org.junit.Before;
import org.junit.Test;
import service.tmdbApi.TMDBMovieCacheService;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Movie data service implementation test
 */
@Slf4j
//@RunWith(JUnit4.class)
public class MovieDataServiceImplTest {

    private MovieDataServiceImpl movieDataService;
    private List<Movie> testMovies;
    private MovieCredits testCredits;
    private Movie movie1, movie2;
    private GameSession testSession;

    @Before
    public void setUp() {
        try {
            // Reset singleton
            java.lang.reflect.Field instance = MovieDataServiceImpl.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(null, null);

            TMDBMovieCacheService.setCache("test_cache");
            TMDBMovieCacheService.setTestMode(true);
            MovieDataServiceImpl.setTestMode(true);

            testMovies = createTestMovies();
            testCredits = createTestCredits();
            movie1 = testMovies.get(0);
            movie2 = testMovies.get(1);

            WinCondition dummy = new WinCondition("genre", "28", 1); // Use genre ID as string
            testSession = new GameSession("test-session", movie1, dummy, dummy, "P1", "P2");

            MovieIndexService.getInstance().initializeIndexes(testMovies);
            movieDataService = MovieDataServiceImpl.getInstance();

        } catch (Exception e) {
            e.printStackTrace();
            fail("Test setup failed");
        }
    }

    @Test
    public void testGetInitialMoviesList() {
        List<Movie> movies = movieDataService.getInitialMoviesList();
        assertNotNull(movies);
    }

    @Test
    public void testSearchMoviesByPrefix() {
        List<Movie> results = movieDataService.searchMoviesByPrefix("mov");
        assertNotNull(results);
    }

    @Test
    public void testGetMovieById() {
        Movie movie = movieDataService.getMovieById(1);
        assertNotNull(movie);
        assertEquals(1, movie.getId());
    }


    @Test
    public void testGetConnections() {
        List<Connection> connections = movieDataService.getConnections(movie1, movie2);
        assertNotNull(connections);
    }

    @Test
    public void testIsConnectionUsedThreeTimes() {
        Connection connection = new Connection(movie1, movie2, "actor", "Actor 1", 101);
        assertFalse(movieDataService.isConnectionUsedThreeTimes(connection, testSession));
        for (int i = 0; i < 3; i++) {
            movieDataService.registerUsedConnection(connection, testSession);
        }
        assertTrue(movieDataService.isConnectionUsedThreeTimes(connection, testSession));
    }

    @Test
    public void testIsMovieAlreadyUsed() {
        assertTrue(movieDataService.isMovieAlreadyUsed(movie1, testSession));
        assertFalse(movieDataService.isMovieAlreadyUsed(movie2, testSession));
        movieDataService.registerUsedMovie(movie2, testSession);
        assertTrue(movieDataService.isMovieAlreadyUsed(movie2, testSession));
    }

    @Test
    public void testMatchesWinConditionGenreById() {
        WinCondition wrongGenre = new WinCondition("genre", "99"); // Not present
        assertFalse(movieDataService.matchesWinCondition(movie1, wrongGenre));
    }

    @Test
    public void testMatchesWinConditionActor() {
        MovieIndexService.getInstance().setMovieCreditsForTest(movie1.getId(), testCredits);
        WinCondition match = new WinCondition("actor", "Actor 1");
        WinCondition noMatch = new WinCondition("actor", "No Actor");

        assertTrue(movieDataService.matchesWinCondition(movie1, match));
        assertFalse(movieDataService.matchesWinCondition(movie1, noMatch));
    }

    @Test
    public void testMatchesWinConditionDirector() {
        MovieIndexService.getInstance().setMovieCreditsForTest(movie1.getId(), testCredits);
        WinCondition match = new WinCondition("director", "Director 1");
        WinCondition noMatch = new WinCondition("director", "Unknown");

        assertTrue(movieDataService.matchesWinCondition(movie1, match));
        assertFalse(movieDataService.matchesWinCondition(movie1, noMatch));
    }

    @Test
    public void testMatchesWinConditionWriter() {
        MovieIndexService.getInstance().setMovieCreditsForTest(movie1.getId(), testCredits);
        WinCondition match = new WinCondition("writer", "Writer 1");
        WinCondition noMatch = new WinCondition("writer", "Fake Writer");

        assertTrue(movieDataService.matchesWinCondition(movie1, match));
        assertFalse(movieDataService.matchesWinCondition(movie1, noMatch));
    }

    @Test
    public void testMatchesWinConditionInvalidType() {
        WinCondition invalid = new WinCondition("invalid", "value");
        assertFalse(movieDataService.matchesWinCondition(movie1, invalid));
    }

    @Test
    public void testMatchesWinConditionNulls() {
        assertFalse(movieDataService.matchesWinCondition(null, new WinCondition("genre", "28")));
        assertFalse(movieDataService.matchesWinCondition(movie1, null));
        assertFalse(movieDataService.matchesWinCondition(null, null));
    }

    @Test
    public void testRegisterUsedMovie() {
        movieDataService.registerUsedMovie(movie2, testSession);
        assertEquals(movie2, testSession.getCurrentMovie());
        assertEquals(2, testSession.getCurrentStep());
        assertTrue(testSession.isMovieAlreadyUsed(movie2));
    }

    @Test
    public void testRegisterUsedConnection() {
        Connection connection = new Connection(movie1, movie2, "actor", "Actor 1", 101);
        movieDataService.registerUsedConnection(connection, testSession);
        assertEquals(1, connection.getUsageCount());
        assertEquals(1, testSession.getConnectionUsageCount().get(101).intValue());
    }

    // --- Test Data ---

    private List<Movie> createTestMovies() {
        Movie m1 = new Movie(); m1.setId(1); m1.setTitle("Movie 1"); m1.setGenreIds(new int[]{28});
        Movie m2 = new Movie(); m2.setId(2); m2.setTitle("Movie 2"); m2.setGenreIds(new int[]{35});
        return Arrays.asList(m1, m2);
    }

    private MovieCredits createTestCredits() {
        MovieCredits credits = new MovieCredits();
        credits.setId(1);

        CastMember c1 = new CastMember(); c1.setId(101); c1.setName("Actor 1");
        CastMember c2 = new CastMember(); c2.setId(102); c2.setName("Actor 2");
        credits.setCast(Arrays.asList(c1, c2));

        CrewMember d1 = new CrewMember(); d1.setId(201); d1.setName("Director 1"); d1.setJob("Director");
        CrewMember w1 = new CrewMember(); w1.setId(202); w1.setName("Writer 1"); w1.setJob("Writer");
        credits.setCrew(Arrays.asList(d1, w1));

        return credits;
    }
}