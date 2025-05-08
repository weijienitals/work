package com.example.ralali_api.service;

import com.example.ralali_api.config.RalaliApiConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for interacting with the Ralali API
 */
@Service
public class RalaliApiService {

    private static final Logger logger = LoggerFactory.getLogger(RalaliApiService.class);
    private final WebClient.Builder webClientBuilder;
    private final RalaliApiConfig apiConfig;
    private final ObjectMapper objectMapper;

    // Possible header names where a token might be found
    private static final String[] POSSIBLE_TOKEN_HEADERS = {
            "Authorization",
            "X-Auth-Token",
            "Token",
            "X-Api-Key",
            "X-Session-Token"
    };

    /**
     * Constructor with dependency injection
     */
    public RalaliApiService(WebClient.Builder webClientBuilder, RalaliApiConfig apiConfig, ObjectMapper objectMapper) {
        this.webClientBuilder = webClientBuilder;
        this.apiConfig = apiConfig;
        this.objectMapper = objectMapper;
    }

    /**
     * Search for sellers by keyword
     *
     * @param keyword The search term
     * @return JSON response as a Mono
     */
    public Mono<JsonNode> searchSellers(String keyword) {
        String url = buildUrl(apiConfig.getBaseUrl(), apiConfig.getSellersEndpoint(), keyword);
        logger.info("Making seller search request with keyword: {}", keyword);
        logger.debug("Request URL: {}", url);

        // Using GET method for Ralali API with header extraction
        return webClientBuilder.build()
                .get()
                .uri(url)
                .header("Authorization", "Bearer " + apiConfig.getAuthToken())
                .exchangeToMono(response -> {
                    // Check for token in response headers
                    checkForTokenInHeaders(response.headers().asHttpHeaders());

                    // Continue with normal response processing
                    return response.bodyToMono(String.class)
                            .map(this::validateAndConvertToJson)
                            .onErrorResume(WebClientResponseException.class, ex ->
                                    handleApiError(ex, "seller", keyword));
                });
    }

    /**
     * Search for products by keyword
     *
     * @param keyword The search term
     * @return JSON response as a Mono
     */
    public Mono<JsonNode> searchProducts(String keyword) {
        String url = buildUrl(apiConfig.getBaseUrl(), apiConfig.getItemsEndpoint(), keyword);
        logger.info("Making product search request with keyword: {}", keyword);
        logger.debug("Request URL: {}", url);

        // Using GET method for Ralali API with header extraction
        return webClientBuilder.build()
                .get()
                .uri(url)
                .header("Authorization", "Bearer " + apiConfig.getAuthToken())
                .header("x-guest-id", apiConfig.getGuestId())
                .exchangeToMono(response -> {
                    // Check for token in response headers
                    checkForTokenInHeaders(response.headers().asHttpHeaders());

                    // Continue with normal response processing
                    return response.bodyToMono(String.class)
                            .map(this::validateAndConvertToJson)
                            .onErrorResume(WebClientResponseException.class, ex ->
                                    handleApiError(ex, "product", keyword));
                });
    }

    /**
     * Check response headers for any authentication tokens and update if found
     */
    private void checkForTokenInHeaders(HttpHeaders headers) {
        // Log all headers for debugging
        logger.debug("Response headers: {}", headers);

        // Check for tokens in common authorization header names
        for (String headerName : POSSIBLE_TOKEN_HEADERS) {
            List<String> headerValues = headers.get(headerName);
            if (headerValues != null && !headerValues.isEmpty()) {
                String headerValue = headerValues.get(0);

                // Check if it's a Bearer token
                if (headerValue.startsWith("Bearer ")) {
                    String token = headerValue.substring(7); // Remove "Bearer " prefix
                    logger.info("Found new token in {} header", headerName);
                    updateToken(token);
                    break;
                }
                // If it's just a raw token
                else if (headerValue.startsWith("eyJ")) { // JWT tokens typically start with "eyJ"
                    logger.info("Found new token in {} header", headerName);
                    updateToken(headerValue);
                    break;
                }
            }
        }

        // Also check for guest ID in headers
        List<String> guestIdValues = headers.get("x-guest-id");
        if (guestIdValues != null && !guestIdValues.isEmpty()) {
            String guestId = guestIdValues.get(0);
            if (!guestId.isEmpty() && !guestId.equals(apiConfig.getGuestId())) {
                logger.info("Found new guest ID: {}", guestId);
                apiConfig.setGuestId(guestId);
            }
        }
    }

    /**
     * Update the authentication token
     */
    private void updateToken(String newToken) {
        if (!newToken.equals(apiConfig.getAuthToken())) {
            logger.info("Updating authentication token");
            apiConfig.setAuthToken(newToken);
        }
    }

    /**
     * Handle API errors with specific messages based on entity type
     */
    private Mono<JsonNode> handleApiError(WebClientResponseException ex, String entityType, String keyword) {
        logger.error("Error response: {} {}", ex.getStatusCode(), ex.getResponseBodyAsString());

        // Check for token in error response headers too
        checkForTokenInHeaders(ex.getHeaders());

        Map<String, Object> errorData = new HashMap<>();
        errorData.put("timestamp", java.time.LocalDateTime.now().toString());
        errorData.put("status", ex.getStatusCode().value());

        // Check for specific status codes and provide appropriate messages
        if (ex.getStatusCode().value() == 422) {
            errorData.put("error", "Not Found");
            errorData.put("message", "No " + entityType + " found matching '" + keyword + "'");
        } else if (ex.getStatusCode().value() == 401) {
            errorData.put("error", "Unauthorized");
            errorData.put("message", "Authentication token has expired. Please update your token.");
        } else if (ex.getStatusCode().value() == 403) {
            errorData.put("error", "Forbidden");
            errorData.put("message", "Your account does not have permission to access this resource.");
        } else if (ex.getStatusCode().value() == 405) {
            errorData.put("error", "Method Not Allowed");
            errorData.put("message", "The API does not support this HTTP method for this endpoint.");
        } else {
            // Generic error for other status codes
            errorData.put("error", ex.getStatusCode().toString());
            errorData.put("message", "API Request Failed");
        }

        // Try to extract more details from response body if available
        try {
            String responseBody = ex.getResponseBodyAsString();
            if (responseBody != null && !responseBody.isEmpty()) {
                JsonNode responseJson = objectMapper.readTree(responseBody);
                if (responseJson.has("message")) {
                    errorData.put("details", responseJson.get("message").asText());
                }
            }
        } catch (Exception e) {
            logger.debug("Could not parse error response body", e);
        }

        try {
            return Mono.just(objectMapper.valueToTree(errorData));
        } catch (Exception e) {
            return Mono.error(new RuntimeException("Failed to create error response", e));
        }
    }

    /**
     * Build URL with query parameters
     */
    private String buildUrl(String baseUrl, String endpoint, String keyword) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl + endpoint);

        // Add all query parameters from configuration
        for (Map.Entry<String, Object> entry : apiConfig.getQueryParams().entrySet()) {
            builder.queryParam(entry.getKey().replace("-", "_"), entry.getValue());
        }

        // Add the keyword parameter
        builder.queryParam("keyword", keyword);

        return builder.build().toUriString();
    }

    /**
     * Validate and convert response to JSON with comprehensive validation
     */
    private JsonNode validateAndConvertToJson(String responseBody) {
        try {
            // First, check if the string is valid JSON
            if (responseBody == null || responseBody.trim().isEmpty()) {
                logger.error("Empty response received from API");
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Empty Response");
                errorResponse.put("message", "The API returned an empty response");
                errorResponse.put("timestamp", java.time.LocalDateTime.now().toString());
                return objectMapper.valueToTree(errorResponse);
            }

            // Try to parse as JSON object
            JsonNode root;
            try {
                root = objectMapper.readTree(responseBody);
            } catch (Exception e) {
                logger.error("Invalid JSON response: {}", responseBody.substring(0, Math.min(responseBody.length(), 200)));
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Invalid JSON");
                errorResponse.put("message", "The API returned an invalid JSON response");
                errorResponse.put("timestamp", java.time.LocalDateTime.now().toString());
                return objectMapper.valueToTree(errorResponse);
            }

            // Log the JSON structure for debugging
            logger.debug("Response JSON type: {}", root.getNodeType().name());

            // Check for expected structure (Ralali should return a specific format)
            if (!root.has("status") && !root.has("data")) {
                logger.warn("Unexpected JSON structure - missing required fields. Response: {}",
                        responseBody.substring(0, Math.min(responseBody.length(), 200)));

                // Still return what we got, but log the issue
                logger.info("Returning unmodified response despite unexpected format");
            }

            // Process data field if it exists
            if (root.has("data")) {
                if (root.get("data").isArray()) {
                    int size = root.get("data").size();
                    logger.debug("Response contains data array with {} items", size);

                    if (size == 0) {
                        Map<String, Object> notFoundResponse = new HashMap<>();
                        notFoundResponse.put("status", "success");
                        notFoundResponse.put("message", "Search completed successfully but no results were found");
                        notFoundResponse.put("data", root.get("data"));
                        return objectMapper.valueToTree(notFoundResponse);
                    }
                } else if (root.get("data").isObject()) {
                    logger.debug("Response contains data object");
                } else if (root.get("data").isNull()) {
                    logger.debug("Response contains null data");

                    Map<String, Object> notFoundResponse = new HashMap<>();
                    notFoundResponse.put("status", "success");
                    notFoundResponse.put("message", "Search completed successfully but no results were found");
                    notFoundResponse.put("data", null);
                    return objectMapper.valueToTree(notFoundResponse);
                }
            }

            // Return the JSON object (as a JsonNode)
            return root;
        } catch (Exception e) {
            logger.error("Failed to process API response: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Processing Error");
            errorResponse.put("message", "Error while processing API response: " + e.getMessage());
            errorResponse.put("timestamp", java.time.LocalDateTime.now().toString());
            return objectMapper.valueToTree(errorResponse);
        }
    }
}