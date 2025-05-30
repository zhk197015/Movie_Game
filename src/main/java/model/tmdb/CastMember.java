package model.tmdb;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Movie actor model
 */
@Data
@EqualsAndHashCode(of = "id")
@JsonIgnoreProperties(ignoreUnknown = true)
public class CastMember {
    @JsonProperty("id")
    private int id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("character")
    private String character;

    @JsonProperty("order")
    private int order;

    @JsonProperty("profile_path")
    private String profilePath;

    @JsonProperty("gender")
    private int gender;
}
