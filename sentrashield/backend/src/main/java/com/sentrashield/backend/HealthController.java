package com.sentrashield.backend;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@RestController
public class HealthController {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @GetMapping("/api/health")
    public String health() {
        return "Backend is up";
    }

    @GetMapping("/api/ping-ml")
    public String pingMl() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://ml-service:8000/health"))
                .GET()
                .build();
        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return "ML service responded: " + response.body();
    }
}
