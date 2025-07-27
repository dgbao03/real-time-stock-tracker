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
import org.springframework.stereotype.Controller;

@Controller
@Slf4j
@AllArgsConstructor
public class WebSocketController {
    private WebSocketService webSocketService;

    private Tracer tracer;

    @MessageMapping("/trackingSymbol")
    public void trackingSymbol(@Payload SymbolTrackingRequest request, @Header("simpSessionId") String sessionId) {
        Span wsSpan = tracer.nextSpan().name("WebSocketController - trackingSymbol: Received Request");
        try (Tracer.SpanInScope ignored = tracer.withSpan(wsSpan.start())) {
            log.info("Received tracking symbol request for session [{}]: [{}] -> [{}]", sessionId, request.getCurrentSymbol(), request.getNewSymbol());
            webSocketService.handleTrackingSymbol(sessionId, request.getCurrentSymbol(), request.getNewSymbol());
        } finally {
            wsSpan.end();
        }
        log.info("Tracking symbol request [{}] for session: [{}] has completed", request.getNewSymbol(), sessionId);
    }

}
