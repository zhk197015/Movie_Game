package service.movie;

import lombok.extern.slf4j.Slf4j;
import model.tmdb.Genre;
import service.tmdbApi.TMDBApiService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Movie Type Service
* Responsible for managing the mapping relationship of movie types
 */
@Slf4j
public class MovieGenreService {
    // Singleton Instance
    private static MovieGenreService instance;
    // Mapping of type IDs to names
    private final Map<Integer, String> genreMap = new ConcurrentHashMap<>();
    // Mapping of type names to IDs
    private final Map<String, Integer> genreNameMap = new ConcurrentHashMap<>();

    /**
     * Private Constructor
     */
    private MovieGenreService() {
        initializeGenres();
    }

    /**
     * Get a singleton instance
     */
    public static synchronized MovieGenreService getInstance() {
        if (instance == null) {
            instance = new MovieGenreService();
        }
        return instance;
    }

    /**
     * Initialize movie type mapping
     */
    private void initializeGenres() {
        List<Genre> genres = TMDBApiService.getMovieGenres();
        if (genres.isEmpty()) {
            log.warn("Unable to get movie genre list, using default mapping");
            // Use default mapping
            Map<Integer, String> defaultGenres = new HashMap<>();
            defaultGenres.put(28, "Action");
            defaultGenres.put(12, "Adventure");
            defaultGenres.put(16, "Animation");
            defaultGenres.put(35, "Comedy");
            defaultGenres.put(80, "Crime");
            defaultGenres.put(99, "Documentary");
            defaultGenres.put(18, "Drama");
            defaultGenres.put(10751, "Family");
            defaultGenres.put(14, "Fantasy");
            defaultGenres.put(36, "History");
            defaultGenres.put(27, "Horror");
            defaultGenres.put(10402, "Music");
            defaultGenres.put(9648, "Mystery");
            defaultGenres.put(10749, "Romance");
            defaultGenres.put(878, "Science Fiction");
            defaultGenres.put(10770, "TV Movie");
            defaultGenres.put(53, "Thriller");
            defaultGenres.put(10752, "War");
            defaultGenres.put(37, "Western");

            defaultGenres.forEach((id, name) -> {
                genreMap.put(id, name);
                genreNameMap.put(name, id);
            });
        } else {
            // Use the type column returned by the API
            genres.forEach(genre -> {
                genreMap.put(genre.getId(), genre.getName());
                genreNameMap.put(genre.getName(), genre.getId());
            });
        }
    }

    /**
     * Get the name corresponding to the type ID
     *
     * @param genreId 
     * @return Type Name
     */
    public String getGenreName(int genreId) {
        return genreMap.getOrDefault(genreId, "Unknown Genre");
    }

    /**
     * Get the ID corresponding to the type name
     *
     * @param genreName 
     * @return TypeID
     */
    public Integer getGenreId(String genreName) {
        return genreNameMap.get(genreName);
    }

    /**
     * Checks if a movie belongs to a given genre
     *
     * @param genreIds  List of movie genre IDs
     * @param genreName The name of the type to check
     * @return Is it of the specified type?
     */
    public boolean hasGenre(int[] genreIds, String genreName) {
        if (genreIds == null || genreName == null) {
            return false;
        }
        Integer targetGenreId = getGenreId(genreName);
        if (targetGenreId == null) {
            return false;
        }
        for (int genreId : genreIds) {
            if (genreId == targetGenreId) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Get all genre ID-to-name mappings.
     *
     * @return an unmodifiable map of genre IDs to genre names
     */
    public Map<Integer, String> getAllGenreMap() {
        return Map.copyOf(genreMap);
    }

    /**
     * Get a sorted list of all genre names.
     *
     * @return sorted list of genre names
     */
    public List<String> getAllGenreNames() {
        return genreMap.values()
                       .stream()
                       .sorted()
                       .toList();
    }

}
