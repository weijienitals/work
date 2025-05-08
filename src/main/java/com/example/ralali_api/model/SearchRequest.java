package com.example.ralali_api.model;

public class SearchRequest {
    private String keyword;

    // Default constructor for JSON deserialization
    public SearchRequest() {
    }

    public SearchRequest(String keyword) {
        this.keyword = keyword;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
}
