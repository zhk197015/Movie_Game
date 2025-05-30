package service.movie;

import lombok.extern.slf4j.Slf4j;
import model.game.WinCondition;
import model.tmdb.CastMember;
import model.tmdb.CrewMember;
import model.tmdb.Movie;
import model.tmdb.MovieCredits;
import org.junit.Before;
import org.junit.Test;
import service.tmdbApi.TMDBMovieCacheService;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
* Victory condition test class
* Focus on testing the judgment of game victory conditions
*/
@Slf4j
public class WinConditionTest {

  private MovieDataService dataService;
  private MovieIndexService indexService;
  private List<Movie> testMovies;
  private MovieCredits credit1, credit2, credit3;
  private Movie actionMovie, comedyMovie, crimeMovie;

  @Before
  public void setUp() {
    try {
     // Reset the singleton
      java.lang.reflect.Field instance = MovieIndexService.class.getDeclaredField("instance");
      instance.setAccessible(true);
      instance.set(null, null);

      instance = MovieDataServiceImpl.class.getDeclaredField("instance");
      instance.setAccessible(true);
      instance.set(null, null);

      // Setting the test mode
      MovieDataServiceImpl.setTestMode(true);
      TMDBMovieCacheService.setCache("test_cache");
      TMDBMovieCacheService.setTestMode(true);

      // Get a service instance
      indexService = MovieIndexService.getInstance();
      dataService = MovieDataServiceImpl.getInstance();

      // Creating test movie data
      testMovies = createTestMovies();
      actionMovie = testMovies.get(0); // Action Movies
      comedyMovie = testMovies.get(1); // Comedy
      crimeMovie = testMovies.get(2); // Crime

      // Creating cast and crew data
      credit1 = createMovieCreditsWithActor(1, 101, "Actor 1");
      credit2 = createMovieCreditsWithActor(2, 102, "Actor 2");
      credit3 = createMovieCreditsWithActor(3, 103, "Actor 3");

      // Create Director Information
      addDirectorToCredits(credit1, 201, "Director 1");
      addDirectorToCredits(credit2, 202, "Director 2");
      addDirectorToCredits(credit3, 203, "Director 3");

      // Initialize index
      indexService.initializeIndexes(testMovies);
      indexService.indexMovieCredits(1, credit1);
      indexService.indexMovieCredits(2, credit2);
      indexService.indexMovieCredits(3, credit3);

    } catch (Exception e) {
      log.error("Failed to set up the test environment", e);
    }
  }

  @Test
  public void testMatchesWinCondition_Genre() {
    WinCondition actionCondition = new WinCondition("genre", "action", 1);

    assertFalse("Comedy should not match action victory conditions",
        dataService.matchesWinCondition(comedyMovie, actionCondition));
  }

  @Test
  public void testMatchesWinCondition_Actor() {
    WinCondition actorCondition = new WinCondition("actor", "Actor 1", 1);

    assertTrue("Movies containing Actor 1 should match Actor 1 victory conditions",
        dataService.matchesWinCondition(actionMovie, actorCondition));

    assertFalse("Movies that do not include Actor 1 should not match Actor 1 victory conditions",
        dataService.matchesWinCondition(comedyMovie, actorCondition));
  }

  @Test
  public void testMatchesWinCondition_Director() {
    WinCondition directorCondition = new WinCondition("director", "Director 1", 1);

    assertTrue("Movies containing Director 1 should match Director 1 victory conditions",
        dataService.matchesWinCondition(actionMovie, directorCondition));

    assertFalse("Movies that do not include Director 1 should not match Director 1 victory conditions",
        dataService.matchesWinCondition(comedyMovie, directorCondition));
  }

  @Test
  public void testWinConditionProgress() {
    WinCondition condition = new WinCondition("genre", "action", 3);

    assertEquals(0, condition.getCurrentCount());
    assertFalse(condition.isAchieved());

    condition.incrementProgress();
    assertEquals(1, condition.getCurrentCount());
    assertFalse(condition.isAchieved());

    condition.incrementProgress();
    condition.incrementProgress();
    assertEquals(3, condition.getCurrentCount());
    assertTrue(condition.isAchieved());
  }

  /**
   * Creating test movie data
   */
  private List<Movie> createTestMovies() {
    List<Movie> movies = new ArrayList<>();

    Movie actionMovie = new Movie();
    actionMovie.setId(1);
    actionMovie.setTitle("Action Movie");
    actionMovie.setOverview("An action movie");
    actionMovie.setPosterPath("/poster1.jpg");
    actionMovie.setReleaseDate("2024-01-01");
    actionMovie.setVoteAverage(8.5);
    actionMovie.setVoteCount(1000);
    actionMovie.setPopularity(100.0);
    actionMovie.setGenreIds(new int[] { 28, 12 }); // 28 are action movies, 12 are adventure movies

    Movie comedyMovie = new Movie();
    comedyMovie.setId(2);
    comedyMovie.setTitle("Comedy Movie");
    comedyMovie.setOverview("A comedy movie");
    comedyMovie.setPosterPath("/poster2.jpg");
    comedyMovie.setReleaseDate("2024-01-02");
    comedyMovie.setVoteAverage(8.0);
    comedyMovie.setVoteCount(900);
    comedyMovie.setPopularity(90.0);
    comedyMovie.setGenreIds(new int[] { 35, 10749 }); // 35 are comedies, 10749 are romances

    Movie crimeMovie = new Movie();
    crimeMovie.setId(3);
    crimeMovie.setTitle("Crime Movie");
    crimeMovie.setOverview("A crime movie");
    crimeMovie.setPosterPath("/poster3.jpg");
    crimeMovie.setReleaseDate("2024-01-03");
    crimeMovie.setVoteAverage(7.5);
    crimeMovie.setVoteCount(800);
    crimeMovie.setPopularity(80.0);
    crimeMovie.setGenreIds(new int[] { 80, 53 }); // 80 for crime movies, 53 for thrillers

    movies.add(actionMovie);
    movies.add(comedyMovie);
    movies.add(crimeMovie);

    return movies;
  }

  /**
   * Create movie actor data
   */
  private MovieCredits createMovieCreditsWithActor(int movieId, int actorId, String actorName) {
    MovieCredits credits = new MovieCredits();
    credits.setId(movieId);

    List<CastMember> castList = new ArrayList<>();

    CastMember cast = new CastMember();
    cast.setId(actorId);
    cast.setName(actorName);
    cast.setCharacter("Character");
    cast.setOrder(1);

    castList.add(cast);
    credits.setCast(castList);

    credits.setCrew(new ArrayList<>());

    return credits;
  }

  /**
   * Add director to cast and crew information
   */
  private void addDirectorToCredits(MovieCredits credits, int directorId, String directorName) {
    CrewMember director = new CrewMember();
    director.setId(directorId);
    director.setName(directorName);
    director.setJob("Director");
    director.setDepartment("Directing");

    credits.getCrew().add(director);
  }
}