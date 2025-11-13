package com.bizscore.config;

import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Конфигурация для ограничения частоты запросов
 */
@Configuration
public class RateLimitConfig {

    @Bean
    public ConcurrentHashMap<String, AtomicInteger> rateLimitCache() {
        return new ConcurrentHashMap<>();
    }
}