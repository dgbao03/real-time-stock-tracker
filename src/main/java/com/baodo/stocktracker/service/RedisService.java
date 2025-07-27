package com.baodo.stocktracker.service;

import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.Span;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.RedisServerCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@Slf4j
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    private final Tracer tracer;

    public RedisService(RedisTemplate<String, Object> redisTemplate,  Tracer tracer) {
        this.redisTemplate = redisTemplate;
        this.tracer = tracer;
    }

    public void save(String symbol, Object value) {
        Span span = tracer.nextSpan().name("RedisService - save: Saving Symbol to Cache");
        try (Tracer.SpanInScope ignored = tracer.withSpan(span.start())) {
            redisTemplate.opsForValue().set(symbol, value);
            log.info("Saved to Redis: [{}] = {}", symbol, value);
        } finally {
            span.end();
        }
    }

    public <T> T get(String key, Class<T> classType) {
        Span span = tracer.nextSpan().name("RedisService - get:  Getting Symbol from Cache");
        try (Tracer.SpanInScope ignored = tracer.withSpan(span.start())) {
            Object value = redisTemplate.opsForValue().get(key);
            if (classType.isInstance(value)) {
                log.info("Fetched [{}] from Redis", key);
                return classType.cast(value);
            }
            log.warn("Value for key [{}] not found or type mismatch", key);
            return null;
        } finally {
            span.end();
        }
    }

    public boolean exists(String symbol) {
        Span span = tracer.nextSpan().name("RedisService - exists: Checking Existence of Symbol");
        try (Tracer.SpanInScope ignored = tracer.withSpan(span.start())) {
            log.info("Checking existence of [{}] in Redis", symbol);
            return redisTemplate.hasKey(symbol);
        } finally {
            span.end();
        }
    }

    public void delete(String symbol) {
        Span span = tracer.nextSpan().name("RedisService - delete:  Deleting Symbol");
        try (Tracer.SpanInScope ignored = tracer.withSpan(span.start())) {
            redisTemplate.delete(symbol);
            log.info("Deleted key [{}] from Redis", symbol);
        } finally {
            span.end();
        }
    }


    @EventListener(ApplicationReadyEvent.class)
    public void clearRedisOnStartup() {
        try (var connection = Objects.requireNonNull(redisTemplate.getConnectionFactory()).getConnection()) {
            RedisServerCommands commands = connection.serverCommands();
            commands.flushDb();
            log.info("Flushed Redis DB on application startup");
        } catch (Exception e) {
            log.error("Error flushing Redis on startup: {}", e.getMessage(), e);
        }
    }
}
