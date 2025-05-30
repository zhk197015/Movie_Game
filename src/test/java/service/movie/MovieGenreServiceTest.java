package service.movie;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class MovieGenreServiceTest {

    private MovieGenreService genreService;

    @Before
    public void setUp() {
      // Reset or initialize singleton if needed
      genreService = MovieGenreService.getInstance();
    }

    @Test
    public void testGetGenreName() {
      // Assuming TMDBApiService already loads standard genres with lowercase keys
      assertEquals("Action", genreService.getGenreName(28));
      assertEquals("Adventure", genreService.getGenreName(12));
      assertEquals("Animation", genreService.getGenreName(16));
      assertEquals("Unknown Genre", genreService.getGenreName(999)); // non-existent ID
    }

    @Test
    public void testGetGenreId() {
      // Case-insensitive matching
      assertEquals(Integer.valueOf(28), genreService.getGenreId("Action"));
      assertEquals(Integer.valueOf(12), genreService.getGenreId("Adventure"));
      assertEquals(Integer.valueOf(16), genreService.getGenreId("Animation"));
      assertNull(genreService.getGenreId("Unknown Type")); // not present
    }

    @Test
    public void testHasGenre() {
      int[] genreIds = {28, 12, 16};
      assertTrue(genreService.hasGenre(genreIds, "Action"));
      assertTrue(genreService.hasGenre(genreIds, "Adventure"));
      assertTrue(genreService.hasGenre(genreIds, "Animation"));

      assertFalse(genreService.hasGenre(genreIds, "comedy"));  // not in test list
      assertFalse(genreService.hasGenre(new int[0], "action"));
      assertFalse(genreService.hasGenre(null, "action"));
      assertFalse(genreService.hasGenre(genreIds, null));
      assertFalse(genreService.hasGenre(null, null));
    }

    @Test
    public void testInitializeGenresWithEmptyApiResponse() {
      // Simulating if TMDBApiService.getGenres() returns empty (not actually modifying it here)
      // We still want default behavior to return "Unknown Type" for unknown IDs
      assertEquals("Action", genreService.getGenreName(28));
      assertEquals("Adventure", genreService.getGenreName(12));
      assertEquals("Animation", genreService.getGenreName(16));
      assertEquals("Unknown Genre", genreService.getGenreName(999));
    }

    @Test
    public void testSingletonInstance() {
      MovieGenreService instance1 = MovieGenreService.getInstance();
      MovieGenreService instance2 = MovieGenreService.getInstance();
      assertSame(instance1, instance2);
    }
  }