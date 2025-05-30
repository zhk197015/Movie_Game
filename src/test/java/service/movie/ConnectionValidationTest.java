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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
* Movie connection verification test class
* Focus on testing the connection relationship verification between movies
 */
@Slf4j
public class ConnectionValidationTest {

    private MovieIndexService indexService;
    private MovieDataService dataService;
    private List<Movie> testMovies;
    private MovieCredits credits1, credits2, credits3;
    private Movie movie1, movie2, movie3;
    private GameSession testSession;

    @Before
    public void setUp() {
      try {
        // Reset singletons
        java.lang.reflect.Field instance = MovieIndexService.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, null);

        instance = MovieDataServiceImpl.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, null);

        // Set test mode
        TMDBMovieCacheService.setCache("test_cache");
        TMDBMovieCacheService.setTestMode(true);
        MovieDataServiceImpl.setTestMode(true);

        indexService = MovieIndexService.getInstance();
        dataService = MovieDataServiceImpl.getInstance();

        testMovies = createTestMovies();
        movie1 = testMovies.get(0);
        movie2 = testMovies.get(1);
        movie3 = testMovies.get(2);

        // Create cast and crew data
        credits1 = createMovieCredits(1, new int[]{101, 102}, new String[]{"Actor 1", "Actor 2"},
                new int[]{201}, new String[]{"Director 1"});

        credits2 = createMovieCredits(2, new int[]{101, 103}, new String[]{"Actor 1", "Actor 3"},
                new int[]{202}, new String[]{"Director 2"});

        credits3 = createMovieCredits(3, new int[]{104, 105}, new String[]{"Actor 4", "Actor 5"},
                new int[]{202}, new String[]{"Director 2"});

        indexService.initializeIndexes(testMovies);
        indexService.indexMovieCredits(1, credits1);
        indexService.indexMovieCredits(2, credits2);
        indexService.indexMovieCredits(3, credits3);

        WinCondition dummy = new WinCondition("actor", "Actor 1");
        testSession = new GameSession("test-session", movie1, dummy, dummy, "P1", "P2");

      } catch (Exception e) {
        e.printStackTrace();
        fail("Test setup failed due to reflection or service config");
      }
    }

    @Test
    public void testValidateConnection_WithCommonActor() {
      assertTrue(dataService.validateConnection(movie1, movie2));
    }

    @Test
    public void testValidateConnection_WithCommonDirector() {
      assertTrue(dataService.validateConnection(movie2, movie3));
    }

    @Test
    public void testValidateConnection_WithNoCommonPerson() {
      assertFalse(dataService.validateConnection(movie1, movie3));
    }

    @Test
    public void testGetConnections_WithCommonActor() {
      List<Connection> connections = dataService.getConnections(movie1, movie2);
      assertNotNull(connections);
      assertTrue(connections.size() > 0);

      boolean found = connections.stream()
              .anyMatch(c -> "actor".equals(c.getConnectionType()) && "Actor 1".equals(c.getConnectionValue()));
      assertTrue("Expected actor connection not found", found);
    }

    @Test
    public void testGetConnections_WithCommonDirector() {
      List<Connection> connections = dataService.getConnections(movie2, movie3);
      assertNotNull(connections);
      assertTrue(connections.size() > 0);

      boolean found = connections.stream()
              .anyMatch(c -> "director".equals(c.getConnectionType()) && "Director 2".equals(c.getConnectionValue()));
      assertTrue("Expected director connection not found", found);
    }

    @Test
    public void testGetConnections_WithNoCommonPerson() {
      List<Connection> connections = dataService.getConnections(movie1, movie3);
      assertNotNull(connections);
      assertEquals(0, connections.size());
    }

    @Test
    public void testConnectionUsage() {
      List<Connection> connections = dataService.getConnections(movie1, movie2);
      assertNotNull(connections);
      assertTrue(connections.size() > 0);

      Connection connection = connections.get(0);
      assertFalse(dataService.isConnectionUsedThreeTimes(connection, testSession));

      for (int i = 0; i < 3; i++) {
        dataService.registerUsedConnection(connection, testSession);
      }

      assertTrue(dataService.isConnectionUsedThreeTimes(connection, testSession));
    }

    // --- Test Data ---

    private List<Movie> createTestMovies() {
      Movie m1 = new Movie(); m1.setId(1); m1.setTitle("Movie 1"); m1.setGenreIds(new int[]{28});
      Movie m2 = new Movie(); m2.setId(2); m2.setTitle("Movie 2"); m2.setGenreIds(new int[]{35});
      Movie m3 = new Movie(); m3.setId(3); m3.setTitle("Movie 3"); m3.setGenreIds(new int[]{80});
      return Arrays.asList(m1, m2, m3);
    }

    private MovieCredits createMovieCredits(int movieId, int[] actorIds, String[] actorNames,
                                            int[] directorIds, String[] directorNames) {
      MovieCredits credits = new MovieCredits();
      credits.setId(movieId);

      List<CastMember> castList = new ArrayList<>();
      for (int i = 0; i < actorIds.length; i++) {
        CastMember cast = new CastMember();
        cast.setId(actorIds[i]);
        cast.setName(actorNames[i]);
        cast.setCharacter("Character " + i);
        cast.setOrder(i);
        castList.add(cast);
      }
      credits.setCast(castList);

      List<CrewMember> crewList = new ArrayList<>();
      for (int i = 0; i < directorIds.length; i++) {
        CrewMember crew = new CrewMember();
        crew.setId(directorIds[i]);
        crew.setName(directorNames[i]);
        crew.setJob("Director");
        crew.setDepartment("Directing");
        crewList.add(crew);
      }
      credits.setCrew(crewList);

      return credits;
    }
  }