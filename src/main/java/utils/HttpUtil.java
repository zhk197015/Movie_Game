package utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import java.io.IOException;
import java.net.Proxy;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * HTTP request tool class
 */
@Slf4j
public class HttpUtil {
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .proxy(Proxy.NO_PROXY) // ⬅ Disable proxy explicitly
            .build();

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
    * Send GET request
    *
    * @param url request address
    * @param headers request header
    * @return response result
    */
    public static String get(String url, Map<String, String> headers) throws IOException {
        log.debug("Send a GET request: {}", url);
        Request.Builder requestBuilder = new Request.Builder().url(url);
        if (headers != null) {
            headers.forEach(requestBuilder::addHeader);
        }
        Request request = requestBuilder.build();
        try (Response response = HTTP_CLIENT
                .newCall(request)
                .execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            String responseBody = response
                    .body()
                    .string();
            log.debug("GET request response: {}", responseBody);
            return responseBody;
        }
    }

    /**
    * Send POST request
    *
    * @param url request address
    * @param headers request header
    * @param body request body
    * @return response result
    */
    public static String post(String url, Map<String, String> headers, String body) throws IOException {
        log.debug("Send a POST request: {}, body: {}", url, body);
        RequestBody requestBody = RequestBody.create(body, MediaType.parse("application/json"));
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .post(requestBody);
        if (headers != null) {
            headers.forEach(requestBuilder::addHeader);
        }
        Request request = requestBuilder.build();
        try (Response response = HTTP_CLIENT
                .newCall(request)
                .execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            String responseBody = null;
            if (response.body() != null) {
                responseBody = response
                        .body()
                        .string();
            }
            log.debug("POST request response: {}", responseBody);
            return responseBody;
        }
    }

    /**
    * Convert JSON string to object
    *
    * @param json JSON string
    * @param clazz target class
    * @return converted object
    */
    public static <T> T fromJson(String json, Class<T> clazz) throws IOException {
        return OBJECT_MAPPER.readValue(json, clazz);
    }

    /**
    * Convert an object to a JSON string
    *
    * @param object object
    * @return JSON string
    */
    public static String toJson(Object object) throws IOException {
        return OBJECT_MAPPER.writeValueAsString(object);
    }

    /**
    * URL encoding
    *
    * @param value The string to be encoded
    * @return The encoded string
    */
    public static String urlEncode(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            log.error("URL encoding exception", e);
            return value;
        }
    }
}
