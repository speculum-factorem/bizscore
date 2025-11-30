package com.bizscore.service;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Сервис для ограничения частоты запросов по IP и пользователям
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final Cache<String, AtomicInteger> rateLimitCache;

    @Value("${rate-limit.requests-per-minute:100}")
    private int maxRequestsPerMinute;

    @Value("${rate-limit.requests-per-hour:1000}")
    private int maxRequestsPerHour;

    public boolean isAllowed(String clientId, String endpoint) {
        String keyPerMinute = String.format("%s:%s:minute", clientId, endpoint);
        String keyPerHour = String.format("%s:%s:hour", clientId, endpoint);

        AtomicInteger minuteCount = rateLimitCache.get(keyPerMinute, k -> new AtomicInteger(0));
        AtomicInteger hourCount = rateLimitCache.get(keyPerHour, k -> new AtomicInteger(0));

        int currentMinute = minuteCount.incrementAndGet();
        int currentHour = hourCount.incrementAndGet();

        if (currentMinute > maxRequestsPerMinute) {
            log.warn("Превышен лимит запросов в минуту для клиента: {}, endpoint: {}", clientId, endpoint);
            return false;
        }

        if (currentHour > maxRequestsPerHour) {
            log.warn("Превышен лимит запросов в час для клиента: {}, endpoint: {}", clientId, endpoint);
            return false;
        }

        return true;
    }

    @Scheduled(fixedRate = 60000) // Каждую минуту
    public void cleanupExpiredEntries() {
        // Caffeine автоматически очищает истекшие записи, но можно принудительно очистить
        rateLimitCache.cleanUp();
        log.debug("Rate limit cache cleanup completed");
    }
}