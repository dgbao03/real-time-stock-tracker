package com.baodo.stocktracker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfiguration {
    @Bean
    public WebClient finnhubClient(WebClient.Builder builder) {
        return builder.baseUrl("https://finnhub.io/api/v1").build();
    }
}
