package model.tmdb;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * TMDB movie list entity class
 */
@Data
public class MovieList {
    @JsonProperty("page")
    private int page;

    @JsonProperty("results")
    private List<Movie> results;

    @JsonProperty("total_pages")
    private int totalPages;

    @JsonProperty("total_results")
    private int totalResults;
}
