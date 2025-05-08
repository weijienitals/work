

package com.example.ralali_api.controller;


import com.example.ralali_api.service.RalaliApiService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

//try to use web client from spring ->reactor component
//change mapping from get ot post
//bruno for api testing
// exception -> try to handle it specifically, throw away the error
//yml file for the parameter values--> parameter
//must import exact packages --> try to find shortcut key to do that
// must be in war instead of jar
// jackson for json
// use

@RestController
public class RalaliController {

    private final RalaliApiService apiService;

    public RalaliController(RalaliApiService apiService) {
        this.apiService = apiService;
    }

    @PostMapping("/search/v3/sellers/{sellerName}")
    public Mono<ResponseEntity<JsonNode>> getSeller(@PathVariable String sellerName) {
        return apiService.searchSellers(sellerName)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

//    public ResponseEntity<?> getSeller(@PathVariable String sellerName) {
//
//        HttpClient client = HttpClient.newHttpClient();
//        HttpRequest request = HttpRequest.newBuilder()
//                .uri(URI.create("https://apigw.ralali.com/search/v3/sellers"+ "?is_personal_courier=false&is_wholesale=false&keyword=" +sellerName + "&limit=20&max_price=10000000000&min_price=1&order_by=match&page=1"))
//                .header("authorization", "Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJjbGllbnRfaWQiOjEwMDAwLCJ0b2tlbl92ZXJzaW9uIjoiMS4wLjAiLCJ0b2tlbl90eXBlIjoiZ3Vlc3RfdG9rZW4iLCJhdWQiOiIxMSIsImV4cCI6MTc0NjY3MDEyNSwianRpIjoiN2YzMWI3MTQtYTRlMS0wNGVkLTBmNWEtZDExYmJkZGY0YzczIiwiaWF0IjoxNzQ2NTgzNzI1LCJpc3MiOiIvZXgvdjMvdG9rZW4ifQ.Oy0PcCJuJzF7ee2v17O30-9IRCHIh-dHAIQPFGnBMvs42ZhcNeUktUn_tXK1bpKoIHVBzWm78fd619xt897mv0JaLmAGHuCkZHtQm_SnwJBsZsGS9QKRxBFMZcbqbDgsmpSFqbI3FTtw6Wc9FsG1cGOy2fiAiw8JAAnGxW2b7cPKL9lIGAS6bbRYXiBYd0o9-VCijvAdvXxVxpH79Phv7c2SFVXgjBEqF9mbNthRELAng_15i6vgRs3xnwLb4HtM5Y6oVhcwN_Yd1RQQ4GfNJDB-HiDljeZAwnmSze1UuUP42F8F7HUGJGX7TJfIejXrDpYJpesPXFOuuI-Gt8USqg")
//                .build();
//
//        // Send and get JSON response
//        try{
//            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
//            System.out.println("\n--- JSON RESPONSE ---\n" + response.body());
//            return ResponseEntity.ok(response.body());
//        } catch (Exception e){
//            e.printStackTrace();
//        }


//        return null;
//
//

    @PostMapping("/search/v3/items/{productName}")
    public Mono<ResponseEntity<JsonNode>> getProduct(@PathVariable String productName) {
        return apiService.searchProducts(productName)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

//    public ResponseEntity<?> getProduct(@PathVariable String productName) {
//
//        HttpClient client = HttpClient.newHttpClient();
//        HttpRequest request = HttpRequest.newBuilder()
//                .uri(URI.create("https://apigw.ralali.com/search/v3/items"+ "?is_personal_courier=false&is_wholesale=false&keyword=" +productName + "&limit=20&max_price=10000000000&min_price=1&order_by=match&page=1"))
//                .header("authorization", "Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJjbGllbnRfaWQiOjEwMDAwLCJ0b2tlbl92ZXJzaW9uIjoiMS4wLjAiLCJ0b2tlbl90eXBlIjoiZ3Vlc3RfdG9rZW4iLCJhdWQiOiIxMSIsImV4cCI6MTc0NjY3MDEyNSwianRpIjoiN2YzMWI3MTQtYTRlMS0wNGVkLTBmNWEtZDExYmJkZGY0YzczIiwiaWF0IjoxNzQ2NTgzNzI1LCJpc3MiOiIvZXgvdjMvdG9rZW4ifQ.Oy0PcCJuJzF7ee2v17O30-9IRCHIh-dHAIQPFGnBMvs42ZhcNeUktUn_tXK1bpKoIHVBzWm78fd619xt897mv0JaLmAGHuCkZHtQm_SnwJBsZsGS9QKRxBFMZcbqbDgsmpSFqbI3FTtw6Wc9FsG1cGOy2fiAiw8JAAnGxW2b7cPKL9lIGAS6bbRYXiBYd0o9-VCijvAdvXxVxpH79Phv7c2SFVXgjBEqF9mbNthRELAng_15i6vgRs3xnwLb4HtM5Y6oVhcwN_Yd1RQQ4GfNJDB-HiDljeZAwnmSze1UuUP42F8F7HUGJGX7TJfIejXrDpYJpesPXFOuuI-Gt8USqg")
//                .header("x-guest-id","defd5756-0b0c-4271-a6c6-9f5dfb4d1d86")
//                .build();
//
//        // Send and get JSON response
//        try{
//            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
//            System.out.println("\n--- JSON RESPONSE ---\n" + response.body());
//            return ResponseEntity.ok(response.body());
//        } catch (Exception e){
//            e.printStackTrace();
//        }
//
//
//        return null;
//
//    }
}