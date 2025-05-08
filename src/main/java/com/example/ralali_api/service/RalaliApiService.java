package com.example.ralali_api.service;

import com.example.ralali_api.config.RalaliApiConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
public class RalaliApiService {

    private final WebClient.Builder webClientBuilder;
    private final RalaliApiConfig apiConfig;
    private final ObjectMapper objectMapper;

    public RalaliApiService(WebClient.Builder webClientBuilder, RalaliApiConfig apiConfig, ObjectMapper objectMapper) {
        this.webClientBuilder = webClientBuilder;
        this.apiConfig = apiConfig;
        this.objectMapper = objectMapper;
    }

    public Mono<JsonNode> searchSellers(String sellerName) {
        String url = buildUrl(apiConfig.getBaseUrl(), apiConfig.getSellersEndpoint(), sellerName);

        return webClientBuilder.build()
                .post()
                .uri(url)
                .header("authorization", "Bearer " + apiConfig.getAuthToken())
                .retrieve()
                .bodyToMono(String.class)
                .map(this::validateAndConvertToJson);
    }

    public Mono<JsonNode> searchProducts(String productName) {
        String url = buildUrl(apiConfig.getBaseUrl(), apiConfig.getItemsEndpoint(), productName);

        return webClientBuilder.build()
                .post()
                .uri(url)
                .header("authorization", "Bearer " + apiConfig.getAuthToken())
                .header("x-guest-id", apiConfig.getGuestId())
                .retrieve()
                .bodyToMono(String.class)
                .map(this::validateAndConvertToJson);
    }

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

    private JsonNode validateAndConvertToJson(String responseBody) {
        try {
            return objectMapper.readTree(responseBody);
        } catch (Exception e) {
            throw new RuntimeException("Invalid JSON response from API", e);
        }
    }
}