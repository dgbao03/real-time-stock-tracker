package com.baodo.stocktracker.controller;

import com.baodo.stocktracker.dto.request.SymbolTrackingRequest;
import com.baodo.stocktracker.service.WebSocketService;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ResponseStatusException;

@Controller
@Slf4j
@AllArgsConstructor
public class WebSocketController {
    private WebSocketService webSocketService;

    private Tracer tracer;

    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/trackingSymbol")
    public void trackingSymbol(@Payload SymbolTrackingRequest request, @Header("simpSessionId") String sessionId) {
        Span wsSpan = tracer.nextSpan().name("WebSocketController - trackingSymbol: Received Request");
        try (Tracer.SpanInScope ignored = tracer.withSpan(wsSpan.start())) {
            log.info("Received tracking symbol request for session [{}]: [{}] -> [{}]", sessionId, request.getCurrentSymbol(), request.getNewSymbol());
            webSocketService.handleTrackingSymbol(sessionId, request.getCurrentSymbol(), request.getNewSymbol());
            log.info("Tracking symbol request [{}] for session: [{}] has completed", request.getNewSymbol(), sessionId);
        } catch (Exception ex) {
            String errorMessage = ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred while tracking symbol [" + request.getNewSymbol() + "]";

            messagingTemplate.convertAndSend("/topic/errors/" + sessionId, errorMessage);

            log.warn("Error tracking symbol for session [{}]: {}", sessionId, errorMessage, ex);
        } finally {
            wsSpan.end();
        }
    }

}
