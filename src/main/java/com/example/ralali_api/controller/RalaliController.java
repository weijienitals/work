package com.example.ralali_api.controller;

import com.example.ralali_api.model.SearchRequest;
import com.example.ralali_api.service.RalaliApiService;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;


@RestController
public class RalaliController {

    private static final Logger logger = LoggerFactory.getLogger(RalaliController.class);
    private final RalaliApiService apiService;

    public RalaliController(RalaliApiService apiService) {
        this.apiService = apiService;
    }

    @PostMapping("/search/v3/sellers")
    public Mono<ResponseEntity<JsonNode>> searchSellers(@RequestBody SearchRequest request) {
        logger.info("Received seller search request with keyword: {}", request.getKeyword());

        return apiService.searchSellers(request.getKeyword())
                .map(this::createResponseEntity);
    }

    @PostMapping("/search/v3/items")
    public Mono<ResponseEntity<JsonNode>> searchProducts(@RequestBody SearchRequest request) {
        logger.info("Received product search request with keyword: {}", request.getKeyword());

        return apiService.searchProducts(request.getKeyword())
                .map(this::createResponseEntity);
    }

    /**
     * Creates a ResponseEntity with the appropriate HTTP status based on the status field in the JsonNode
     */
    private ResponseEntity<JsonNode> createResponseEntity(JsonNode jsonNode) {
        // Extract the status code from the response if it exists
        int statusCode = 200; // Default to OK

        if (jsonNode.has("status") && jsonNode.get("status").isInt()) {
            statusCode = jsonNode.get("status").asInt();
            logger.debug("Setting HTTP status code to {} based on response", statusCode);
        }

        // Create a ResponseEntity with the appropriate status code
        return ResponseEntity.status(statusCode).body(jsonNode);
    }
}