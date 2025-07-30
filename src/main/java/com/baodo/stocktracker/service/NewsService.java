package com.baodo.stocktracker.service;

import com.baodo.stocktracker.dto.response.NewsItemResponse;
import io.micrometer.tracing.Tracer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class NewsService {
    @Value("${finnhub.api.key}")
    private String apiKey;

    private WebClient finnhubClient;

    private Tracer tracer;

    public NewsService(WebClient finnhubClient, Tracer tracer) {
        this.finnhubClient = finnhubClient;
        this.tracer = tracer;
    }

    public List<NewsItemResponse> getCompanyNews(String symbol) {
        var span = tracer.nextSpan().name("NewsService - getCompanyNews: Fetching company news").start();
        try (Tracer.SpanInScope ignored = tracer.withSpan(span)) {
            log.info("Fetching company news for symbol from Finnhub API: {}", symbol);

            LocalDate toDate = LocalDate.now();
            LocalDate fromDate = toDate.minusDays(3);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            try {
                if ("TEST_ERROR".equalsIgnoreCase(symbol)) {
                    throw new RuntimeException("Simulated exception for testing");
                }

                List<NewsItemResponse> news = finnhubClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/company-news")
                                .queryParam("symbol", symbol.toUpperCase())
                                .queryParam("from", fromDate.format(formatter))
                                .queryParam("to", toDate.format(formatter))
                                .queryParam("token", apiKey)
                                .build())
                        .retrieve()
                        .bodyToFlux(NewsItemResponse.class)
                        .collectList()
                        .block();

                return news == null ? Collections.emptyList() : news;
            } catch (Exception ex) {
                log.info("Failed to fetch company news from Finnhub for symbol [{}]: {}", symbol, ex.getMessage());
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to fetch company news from Finnhub for symbol [" + symbol + "]");
            }
        } finally {
            span.end();
        }
    }


}
