package com.bizscore.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.TimeUnit;

/**
 * Сервис для ограничения частоты запросов по IP и пользователям
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final ConcurrentHashMap<String, AtomicInteger> rateLimitCache;

    // Лимиты запросов
    private static final int MAX_REQUESTS_PER_MINUTE = 100;
    private static final int MAX_REQUESTS_PER_HOUR = 1000;

    public boolean isAllowed(String clientId, String endpoint) {
        String keyPerMinute = String.format("%s:%s:minute", clientId, endpoint);
        String keyPerHour = String.format("%s:%s:hour", clientId, endpoint);

        AtomicInteger minuteCount = rateLimitCache.computeIfAbsent(keyPerMinute, k -> new AtomicInteger(0));
        AtomicInteger hourCount = rateLimitCache.computeIfAbsent(keyPerHour, k -> new AtomicInteger(0));

        int currentMinute = minuteCount.incrementAndGet();
        int currentHour = hourCount.incrementAndGet();

        if (currentMinute > MAX_REQUESTS_PER_MINUTE) {
            log.warn("Превышен лимит запросов в минуту для клиента: {}, endpoint: {}", clientId, endpoint);
            return false;
        }

        if (currentHour > MAX_REQUESTS_PER_HOUR) {
            log.warn("Превышен лимит запросов в час для клиента: {}, endpoint: {}", clientId, endpoint);
            return false;
        }

        return true;
    }

    public void cleanupExpiredEntries() {
        rateLimitCache.entrySet().removeIf(entry -> {
            // Здесь можно добавить логику очистки устаревших записей
            return false;
        });
    }
}