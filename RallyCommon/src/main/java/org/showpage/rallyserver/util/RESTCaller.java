package org.showpage.rallyserver.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.showpage.rallyserver.RestResponse;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * REST client for making HTTP calls using Java's built-in HttpClient.
 * Provides generic methods for GET, POST, PUT, and DELETE operations with JSON support.
 */
@Slf4j
public class RESTCaller {
    private final String serverUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Map<String, String> defaultHeaders;

    /**
     * Create a RESTCaller with the specified server URL.
     *
     * @param serverUrl The base server URL (e.g., "http://localhost:8080")
     */
    public RESTCaller(String serverUrl) {
        this.serverUrl = serverUrl.endsWith("/") ? serverUrl.substring(0, serverUrl.length() - 1) : serverUrl;

        httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        objectMapper = new ObjectMapper();
        defaultHeaders = new HashMap<>();

        // Set default Content-Type header
        defaultHeaders.put("Content-Type", "application/json");
    }

    /**
     * Add or update a default header that will be included in all requests.
     *
     * @param name Header name
     * @param value Header value
     */
    public void setDefaultHeader(String name, String value) {
        defaultHeaders.put(name, value);
    }

    /**
     * Remove a default header.
     *
     * @param name Header name to remove
     */
    public void removeDefaultHeader(String name) {
        defaultHeaders.remove(name);
    }

    /**
     * Perform a GET request.
     *
     * @param path Relative path (e.g., "/api/rallies")
     * @param authHeader Authorization header value (can be null)
     * @param typeRef TypeReference for JSON deserialization
     * @return Deserialized response object
     * @throws IOException If an I/O error occurs
     * @throws InterruptedException If the request is interrupted
     */
    public <T> T get(String path, String authHeader, TypeReference<T> typeRef) throws IOException, InterruptedException {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl + path))
                .GET();

        return execute(requestBuilder, authHeader, typeRef);
    }

    /**
     * Perform a POST request.
     *
     * @param path Relative path (e.g., "/api/rally")
     * @param body Request body object (will be serialized to JSON)
     * @param authHeader Authorization header value (can be null)
     * @param typeRef TypeReference for JSON deserialization
     * @return Deserialized response object
     * @throws IOException If an I/O error occurs
     * @throws InterruptedException If the request is interrupted
     */
    public <T> T post(String path, Object body, String authHeader, TypeReference<T> typeRef) throws IOException, InterruptedException {
        String jsonBody = objectMapper.writeValueAsString(body);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl + path))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody));

        return execute(requestBuilder, authHeader, typeRef);
    }

    /**
     * Perform a PUT request.
     *
     * @param path Relative path (e.g., "/api/rally/1")
     * @param body Request body object (will be serialized to JSON)
     * @param authHeader Authorization header value (can be null)
     * @param typeRef TypeReference for JSON deserialization
     * @return Deserialized response object
     * @throws IOException If an I/O error occurs
     * @throws InterruptedException If the request is interrupted
     */
    public <T> T put(String path, Object body, String authHeader, TypeReference<T> typeRef) throws IOException, InterruptedException {
        String jsonBody = objectMapper.writeValueAsString(body);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl + path))
                .PUT(HttpRequest.BodyPublishers.ofString(jsonBody));

        return execute(requestBuilder, authHeader, typeRef);
    }

    /**
     * Perform a DELETE request.
     *
     * @param path Relative path (e.g., "/api/rally/1")
     * @param authHeader Authorization header value (can be null)
     * @param typeRef TypeReference for JSON deserialization
     * @return Deserialized response object
     * @throws IOException If an I/O error occurs
     * @throws InterruptedException If the request is interrupted
     */
    public <T> T delete(String path, String authHeader, TypeReference<T> typeRef) throws IOException, InterruptedException {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl + path))
                .DELETE();

        return execute(requestBuilder, authHeader, typeRef);
    }

    /**
     * Add default headers and optional authorization header to the request.
     */
    private void addHeaders(HttpRequest.Builder requestBuilder, String authHeader) {
        // Add default headers
        for (Map.Entry<String, String> header : defaultHeaders.entrySet()) {
            requestBuilder.header(header.getKey(), header.getValue());
        }

        // Add authorization header if provided
        if (authHeader != null && !authHeader.isEmpty()) {
            requestBuilder.header("Authorization", authHeader);
        }
    }

    /**
     * All the methods do the same thing after initializing the HttpRequest Builder.
     */
    private <T> T execute(
            HttpRequest.Builder requestBuilder,
            String authHeader,
            TypeReference<T> typeRef
    ) throws IOException, InterruptedException {
        addHeaders(requestBuilder, authHeader);

        HttpRequest request = requestBuilder.build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        T retVal = objectMapper.readValue(response.body(), typeRef);
        setResponseCode(retVal, response);

        return retVal;
    }

    /**
     * If this is a RestResponse, then we can also force the status code.
     */
    private <T> void setResponseCode(T retVal, HttpResponse<String> response) {
        if (retVal instanceof RestResponse<?> rr) {
            rr.setStatusCode(response.statusCode());
        }
    }
}
