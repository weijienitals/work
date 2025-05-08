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
                            .map(responseBody -> {
                                // First parse the JSON
                                JsonNode initialNode = validateAndConvertToJson(responseBody, "seller", keyword);
                                // Then check and fix not-found issues
                                return checkForNotFoundAndFixResponse(initialNode, "seller", keyword);
                            })
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
                            .map(responseBody -> {
                                // First parse the JSON
                                JsonNode initialNode = validateAndConvertToJson(responseBody, "product", keyword);
                                // Then check and fix not-found issues
                                return checkForNotFoundAndFixResponse(initialNode, "product", keyword);
                            })
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

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", java.time.LocalDateTime.now().toString());

        // Always include a numeric status code
        int statusCode = ex.getStatusCode().value();
        errorResponse.put("status", statusCode);

        // Check for specific status codes and provide appropriate messages
        if (statusCode == 422 || statusCode == 404) {
            // For entity not found errors
            errorResponse.put("error", "Not Found");

            // Provide entity-specific message
            if ("seller".equals(entityType)) {
                errorResponse.put("message", "No seller found matching '" + keyword + "'");
            } else if ("product".equals(entityType)) {
                errorResponse.put("message", "No product found matching '" + keyword + "'");
            } else {
                errorResponse.put("message", "No " + entityType + " found matching '" + keyword + "'");
            }

            // Override the status to be consistently 404 for not found
            errorResponse.put("status", 404);
        } else if (statusCode == 401) {
            errorResponse.put("error", "Unauthorized");
            errorResponse.put("message", "Authentication token has expired. Please update your token.");
        } else if (statusCode == 403) {
            errorResponse.put("error", "Forbidden");
            errorResponse.put("message", "Your account does not have permission to access this resource.");
        } else if (statusCode == 405) {
            errorResponse.put("error", "Method Not Allowed");
            errorResponse.put("message", "The API does not support this HTTP method for this endpoint.");
        } else if (statusCode >= 500) {
            errorResponse.put("error", "Server Error");
            errorResponse.put("message", "The Ralali server encountered an error. Please try again later.");
        } else {
            // Generic error for other status codes
            errorResponse.put("error", ex.getStatusCode().toString());
            errorResponse.put("message", "API Request Failed");
        }

        // Try to extract more details from response body if available
        try {
            String responseBody = ex.getResponseBodyAsString();
            if (responseBody != null && !responseBody.isEmpty()) {
                JsonNode responseJson = objectMapper.readTree(responseBody);
                if (responseJson.has("message")) {
                    errorResponse.put("details", responseJson.get("message").asText());
                }
            }
        } catch (Exception e) {
            logger.debug("Could not parse error response body", e);
        }

        // Add the original status code from Ralali for reference
        errorResponse.put("originalStatus", statusCode);

        try {
            return Mono.just(objectMapper.valueToTree(errorResponse));
        } catch (Exception e) {
            logger.error("Failed to create error response", e);
            Map<String, Object> fallbackResponse = new HashMap<>();
            fallbackResponse.put("error", "Internal Error");
            fallbackResponse.put("message", "Failed to create error response: " + e.getMessage());
            fallbackResponse.put("status", 500);
            fallbackResponse.put("timestamp", java.time.LocalDateTime.now().toString());
            return Mono.just(objectMapper.valueToTree(fallbackResponse));
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
     * Properly detects DATA_NOT_FOUND responses from Ralali
     */
    private JsonNode validateAndConvertToJson(String responseBody, String entityType, String keyword) {
        try {
            // First, check if the string is valid JSON
            if (responseBody == null || responseBody.trim().isEmpty()) {
                logger.error("Empty response received from API");
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Not Found");
                errorResponse.put("message", "No " + entityType + " found matching '" + keyword + "'");
                errorResponse.put("timestamp", java.time.LocalDateTime.now().toString());
                errorResponse.put("status", 404); // Not Found status
                return objectMapper.valueToTree(errorResponse);
            }

            // Try to parse as JSON object
            JsonNode root;
            try {
                root = objectMapper.readTree(responseBody);
                logger.debug("Successfully parsed JSON response");
            } catch (Exception e) {
                logger.error("Invalid JSON response: {}", responseBody.substring(0, Math.min(responseBody.length(), 200)));
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Invalid JSON");
                errorResponse.put("message", "The API returned an invalid JSON response");
                errorResponse.put("timestamp", java.time.LocalDateTime.now().toString());
                errorResponse.put("status", 500); // Internal Server Error status
                return objectMapper.valueToTree(errorResponse);
            }

            // Just return the parsed JSON - further processing will be done in checkForNotFoundAndFixResponse
            return root;
        } catch (Exception e) {
            logger.error("Failed to process API response: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Processing Error");
            errorResponse.put("message", "Error while processing API response: " + e.getMessage());
            errorResponse.put("timestamp", java.time.LocalDateTime.now().toString());
            errorResponse.put("status", 500); // Internal Server Error status
            return objectMapper.valueToTree(errorResponse);
        }
    }

    private JsonNode checkForNotFoundAndFixResponse(JsonNode root, String entityType, String keyword) {
        logger.info("Checking for not found patterns in response");
        logger.debug("Raw response structure: {}", root);

        // Create a new response
        Map<String, Object> fixedResponse = new HashMap<>();
        fixedResponse.put("timestamp", java.time.LocalDateTime.now().toString());

        // CASE 1: Check for the nested DATA_NOT_FOUND pattern
        if (root.has("data") &&
                root.get("data").isObject() &&
                root.get("data").has("name") &&
                "DATA_NOT_FOUND".equals(root.get("data").get("name").asText())) {

            logger.info("Detected DATA_NOT_FOUND in nested data object");

            // Set 404 status
            fixedResponse.put("status", 404);
            fixedResponse.put("error", "Not Found");

            // Set entity-specific message
            if ("seller".equals(entityType)) {
                fixedResponse.put("message", "No seller found matching '" + keyword + "'");
            } else if ("product".equals(entityType)) {
                fixedResponse.put("message", "No product found matching '" + keyword + "'");
            } else {
                fixedResponse.put("message", "No " + entityType + " found matching '" + keyword + "'");
            }

            // Add the original data
            fixedResponse.put("data", root.get("data"));

            return objectMapper.valueToTree(fixedResponse);
        }

        // If no not-found pattern was detected, return the original response
        return root;
    }
}