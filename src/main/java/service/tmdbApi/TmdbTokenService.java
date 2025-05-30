package service.tmdbApi;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import utils.HttpUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * TMDB Token Services
 */
@Slf4j
public class TmdbTokenService {
    private static final String TMDB_API_KEY = "b66be751fa2a2b0abc87f18e1767150d";
    private static final String TMDB_ACCESS_TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJiNjZiZTc1MWZhMmEyYjBhYmM4N2YxOGUxNzY3MTUwZCIsIm5iZiI6MTc0Mzk0MzQxMi4wMzAwMDAyLCJzdWIiOiI2N2YyNzZmNDJmN2Q0MzcwMjc5OWQ2ZjIiLCJzY29wZXMiOlsiYXBpX3JlYWQiXSwidmVyc2lvbiI6MX0.kVps53qR3qYXbmakBAH4XVd0QIJdmMvBHLMBUgoH_3o";
    /**
    * -- GETTER --
    * Get the base URL
    */
    @Getter
    private static String baseUrl = "https://api.themoviedb.org/3";
    private static String authUrl = baseUrl + "/authentication/token/new";

    /**
    * Set the base URL (for testing only)
    */
    public static void setBaseUrl(String url) {
        baseUrl = url;
        authUrl = baseUrl + "/authentication/token/new";
    }

    /**
    * Get TMDB request token
    *
    * @return request token
    */
    public static String getRequestToken() {
        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Bearer " + TMDB_ACCESS_TOKEN);
            headers.put("accept", "application/json");

            String response = HttpUtil.get(authUrl, headers);
            TokenResponse tokenResponse = HttpUtil.fromJson(response, TokenResponse.class);

            if (tokenResponse != null && tokenResponse.isSuccess()) {
                return tokenResponse.getRequestToken();
            }
            log.error("Failed to obtain TMDB request token: {}", response);
            return null;
        } catch (Exception e) {
            log.error("Exception in obtaining TMDB request token", e);
            return null;
        }
    }

    @Data
    private static class TokenResponse {
        @JsonProperty("success")
        private boolean success;

        @JsonProperty("expires_at")
        private String expiresAt;

        @JsonProperty("request_token")
        private String requestToken;
    }
}
