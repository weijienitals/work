package com.example.ralali_api.service;

import com.example.ralali_api.config.RalaliApiConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.util.HashMap;
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
                .header("Authorization", apiConfig.getAuthToken())
                .exchangeToMono(response -> {

                    // Continue with normal response processing
                    return response.bodyToMono(String.class)
                            .map(responseBody -> {
                                // First parse the JSON
                                JsonNode initialNode = validateAndConvertToJson(responseBody, "seller", keyword);
                                return initialNode;
                            });
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
                .header("Authorization", apiConfig.getAuthToken())
                .header("x-guest-id", apiConfig.getGuestId())
                .exchangeToMono(response -> {
                    // Continue with normal response processing
                    return response.bodyToMono(String.class)
                            .map(responseBody -> {
                                // First parse the JSON
                                JsonNode initialNode = validateAndConvertToJson(responseBody, "product", keyword);
                                // Then check and fix not-found issues
//                                return checkForNotFoundAndFixResponse(initialNode, "product", keyword);
                                return initialNode;
                            });
//
                });
    }


    /**
     * responsible for constructing the complete URL with query parameters that will be used to communicate with the Ralali API.
     */
    private String buildUrl(String baseUrl, String endpoint, String keyword) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl + endpoint);

        // Add all query parameters from configuration
        for (Map.Entry<String, Object> entry : apiConfig.getQueryParams().entrySet()) {
            builder.queryParam(entry.getKey().replace("-", "_"), entry.getValue());
        }

//         Add the keyword parameter
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

}