package model.tmdb;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * Movie cast and crew information
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MovieCredits {
    @JsonProperty("id")
    private int id;

    @JsonProperty("cast")
    private List<CastMember> cast;

    @JsonProperty("crew")
    private List<CrewMember> crew;
}
