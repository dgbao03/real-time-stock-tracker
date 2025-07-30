package com.baodo.stocktracker.service;

import com.baodo.stocktracker.dto.response.SymbolQuoteResponse;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.Span;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
@AllArgsConstructor
public class WebSocketService {
    private final SimpMessagingTemplate messagingTemplate;

    private final RedisService redisService;

    private final FinnhubService finnhubService;

    private final Map<String, Set<String>> symbolViewers = new HashMap<>();

    private Tracer tracer;

    public void handleTrackingSymbol(String sessionId, String currentSymbol, String newSymbol) {
        Span parentSpan = tracer.nextSpan().name("WebSocketService - handleTrackingSymbol: Starting Point");
        try (Tracer.SpanInScope scope = tracer.withSpan(parentSpan.start())) {
            log.info("Session [{}] requests to switch from [{}] to [{}]", sessionId, currentSymbol, newSymbol);

            // Step 1: Unsubscribe current symbol
            if (symbolViewers.containsKey(currentSymbol)) {
                log.info("Unsubscribing session [{}] from symbol [{}]", sessionId, currentSymbol);
                unsubscribeViewerFromSymbol(currentSymbol, sessionId);
            }

            // Step 2: Check cache, fetch from Finnhub if needed
            if (!redisService.exists(newSymbol)) {
                log.info("Symbol [{}] not found in cache. Fetching from data Finnhub", newSymbol);

                SymbolQuoteResponse quote = finnhubService.getQuote(newSymbol).block();
                if (quote == null || (quote.getC() == 0.0 && quote.getT() == 0)) {
                    log.info("Symbol [{}] is invalid or has no trade data", newSymbol);
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Symbol [" + newSymbol + "] not found");
                }
                redisService.save(newSymbol, quote.getC());
            }

            // Step 3: Broadcast to client
            Span broadcastSpan = tracer.nextSpan(parentSpan).name("WebSocketService - handleTrackingSymbol: Broadcasting Symbol to Client");
            try (Tracer.SpanInScope s = tracer.withSpan(broadcastSpan.start())) {
                messagingTemplate.convertAndSend("/topic/price" + newSymbol, redisService.get(newSymbol, Double.class));
                log.info("Broadcasted price for symbol [{}] to session [{}]", newSymbol, sessionId);
            } finally {
                broadcastSpan.end();
            }

            // Step 4: Update viewer tracking
            Span updateSpan = tracer.nextSpan(parentSpan).name("WebSocketService - handleTrackingSymbol: Update Viewer for Symbol");
            try (Tracer.SpanInScope s = tracer.withSpan(updateSpan.start())) {
                symbolViewers.computeIfAbsent(newSymbol, k -> new HashSet<>()).add(sessionId);
                log.debug("Updated symbolViewers: {}", symbolViewers);
            } finally {
                updateSpan.end();
            }

            // Step 5: Subscribe to Finnhub
            try {
                finnhubService.subscribeSymbol(newSymbol);
            } catch (IOException e) {
                log.error("Error subscribing to symbol [{}] on Finnhub", newSymbol, e);
            }

        } finally {
            parentSpan.end();
        }
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        String sessionId = StompHeaderAccessor.wrap(event.getMessage()).getSessionId();
        log.info("Session [{}] disconnected", sessionId);

        for (String symbol : new HashSet<>(symbolViewers.keySet())) {
            unsubscribeViewerFromSymbol(symbol, sessionId);
        }

        log.debug("After disconnect, updated symbolViewers: {}", symbolViewers);
    }

    private void unsubscribeViewerFromSymbol(String symbol, String sessionId) {
        Span unsubscribeSpan = tracer.nextSpan().name("WebSocketService - unsubscribeViewerFromSymbol: Unsubscribe Symbol");
        try (Tracer.SpanInScope s = tracer.withSpan(unsubscribeSpan.start())) {
            Set<String> viewers = symbolViewers.get(symbol);
            if (viewers != null) {
                viewers.remove(sessionId);
                log.debug("Removed session [{}] from viewers of symbol [{}]", sessionId, symbol);

                if (viewers.isEmpty()) {
                    symbolViewers.remove(symbol);
                    redisService.delete(symbol);

                    try {
                        finnhubService.unsubscribeSymbol(symbol);
                        log.info("Unsubscribed from symbol [{}] on Finnhub", symbol);
                    } catch (IOException e) {
                        log.error("Error unsubscribing from symbol [{}] on Finnhub", symbol, e);
                    }
                }
            }
        } finally {
            unsubscribeSpan.end();
        }
    }

}

