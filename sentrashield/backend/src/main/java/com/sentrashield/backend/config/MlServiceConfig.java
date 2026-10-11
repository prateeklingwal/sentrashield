package com.sentrashield.backend.config;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class MlServiceConfig {

    @Bean
    public RestClient mlServiceRestClient() {
        // Force HTTP/1.1 -- the JDK HttpClient defaults to attempting an HTTP/2
        // h2c upgrade, which uvicorn does not support and silently drops the body.
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(30));

        return RestClient.builder()
                .baseUrl("http://ml-service:8000")
                .requestFactory(requestFactory)
                .build();
    }
}
