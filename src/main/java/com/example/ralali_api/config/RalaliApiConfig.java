package com.example.ralali_api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "ralali.api")
public class RalaliApiConfig {

    private String baseUrl;
    private String sellersEndpoint;
    private String itemsEndpoint;
    private String authToken;
    private String guestId;
    private Map<String, Object> queryParams;

    // Getters and setters
    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getSellersEndpoint() {
        return sellersEndpoint;
    }

    public void setSellersEndpoint(String sellersEndpoint) {
        this.sellersEndpoint = sellersEndpoint;
    }

    public String getItemsEndpoint() {
        return itemsEndpoint;
    }

    public void setItemsEndpoint(String itemsEndpoint) {
        this.itemsEndpoint = itemsEndpoint;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public String getGuestId() {
        return guestId;
    }

    public void setGuestId(String guestId) {
        this.guestId = guestId;
    }

    public Map<String, Object> getQueryParams() {
        return queryParams;
    }

    public void setQueryParams(Map<String, Object> queryParams) {
        this.queryParams = queryParams;
    }
}