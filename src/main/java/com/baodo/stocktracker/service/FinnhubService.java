package com.baodo.stocktracker.service;

import com.baodo.stocktracker.dto.response.SymbolQuoteResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.tracing.Tracer;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.socket.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import reactor.core.publisher.Mono;
import io.micrometer.tracing.Span;

import java.io.IOException;
import java.net.URI;

@Service
@Slf4j
public class FinnhubService {
    @Value("${finnhub.api.key}")
    private String apiKey;

    private final WebClient finnhubWebClient;

    private final SimpMessagingTemplate messagingTemplate;

    private final RedisService redisService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private WebSocketSession session;

    private final Tracer tracer;

    public FinnhubService(WebClient finnhubWebClient, SimpMessagingTemplate messagingTemplate, RedisService redisService, Tracer tracer) {
        this.finnhubWebClient = finnhubWebClient;
        this.messagingTemplate = messagingTemplate;
        this.redisService = redisService;
        this.tracer = tracer;
    }

    @PostConstruct
    public void connect() {
        StandardWebSocketClient client = new StandardWebSocketClient();

        client.execute(
                new WebSocketHandler() {
                    @Override
                    public void afterConnectionEstablished(WebSocketSession session) {
                        FinnhubService.this.session = session;
                        log.info("Connected to Finnhub WebSocket!");
                    }

                    @Override
                    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws IOException {
                        JsonNode json = objectMapper.readTree(message.getPayload().toString());
                        if ("trade".equals(json.get("type").asText())) {
                            JsonNode tradeData = json.get("data").get(0);

                            String symbol = tradeData.get("s").asText();
                            double price = tradeData.get("p").asDouble();

                            redisService.save(symbol, price);
                            messagingTemplate.convertAndSend("/topic/price" + symbol, price);
                        }
                    }

                    @Override
                    public void handleTransportError(WebSocketSession session, Throwable exception) {
                        log.error("WebSocket transport error: {}", exception.getMessage(), exception);
                    }

                    @Override
                    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
                        log.warn("Finnhub WebSocket connection closed: {}", closeStatus);
                    }

                    @Override
                    public boolean supportsPartialMessages() {
                        return false;
                    }
                }, null, URI.create("wss://ws.finnhub.io?token=" + apiKey)
        );
    }

    public Mono<SymbolQuoteResponse> getQuote(String symbol) {
        Span newSpan = tracer.nextSpan().name("FinnhubService - getQuote: Getting Symbol Data from Finnhub API");
        return Mono.deferContextual(contextView -> {
            try (Tracer.SpanInScope scope = tracer.withSpan(newSpan.start())) {
                log.info("Fetching quote for symbol [{}] from Finnhub", symbol);

                return finnhubWebClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/quote")
                                .queryParam("symbol", symbol)
                                .queryParam("token", apiKey)
                                .build())
                        .retrieve()
                        .bodyToMono(SymbolQuoteResponse.class)
                        .doOnTerminate(newSpan::end)
                        .onErrorResume(ex -> {
                            log.info("Failed to fetch stock data from Finnhub for symbol [{}]: {}", symbol, ex.getMessage());
                            return Mono.error(new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to fetch stock data from Finnhub for symbol [" + symbol + "]", ex));
                        });
            }
        });
    }

    public void subscribeSymbol(String symbol) throws IOException {
        Span newSpan = tracer.nextSpan().name("FinnhubService - subscribeSymbol: Subscribe to Symbol Finnhub WS");
        try (Tracer.SpanInScope scope = tracer.withSpan(newSpan.start())) {
            String subMessage = String.format("{\"type\":\"subscribe\",\"symbol\":\"%s\"}", symbol);
            log.info("Subscribing to symbol [{}] via WebSocket", symbol);
            session.sendMessage(new TextMessage(subMessage));
        } finally {
            newSpan.end();
        }
    }


    public void unsubscribeSymbol(String symbol) throws IOException {
        Span newSpan = tracer.nextSpan().name("FinnhubService - unsubscribeSymbol: Unsubscribe from Symbol Finnhub WS");
        try (Tracer.SpanInScope scope = tracer.withSpan(newSpan.start())) {
            String unsubMessage = String.format("{\"type\":\"unsubscribe\",\"symbol\":\"%s\"}", symbol);
            log.info("Unsubscribing from symbol [{}] via WebSocket", symbol);
            session.sendMessage(new TextMessage(unsubMessage));
        } finally {
            newSpan.end();
        }
    }

}
