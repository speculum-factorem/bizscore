package com.bizscore.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Сервис для мониторинга SLA (Service Level Agreement)
 * Отслеживает производительность и доступность сервиса
 */
@Slf4j
@Service
public class SlaMonitoringService {

    private final MeterRegistry meterRegistry;
    private final Counter totalRequests;
    private final Counter successfulRequests;
    private final Counter failedRequests;
    private final Timer requestProcessingTime;
    private final AtomicInteger activeRequests;
    private final ConcurrentHashMap<String, LocalDateTime> requestStartTimes;

    // Параметры SLA
    private static final long MAX_RESPONSE_TIME_MS = 5000;
    private static final double MAX_ERROR_RATE = 0.05;
    private static final int MAX_CONCURRENT_REQUESTS = 100;
    private static final double MAX_CPU_USAGE = 0.8;
    private static final long MAX_MEMORY_USAGE = 85;

    public SlaMonitoringService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.activeRequests = meterRegistry.gauge("sla.active.requests", new AtomicInteger(0));
        this.requestStartTimes = new ConcurrentHashMap<>();

        this.totalRequests = Counter.builder("sla.requests.total")
                .description("Общее количество запросов")
                .register(meterRegistry);

        this.successfulRequests = Counter.builder("sla.requests.successful")
                .description("Количество успешных запросов")
                .register(meterRegistry);

        this.failedRequests = Counter.builder("sla.requests.failed")
                .description("Количество неудачных запросов")
                .register(meterRegistry);

        this.requestProcessingTime = Timer.builder("sla.request.processing.time")
                .description("Время обработки запроса")
                .register(meterRegistry);
    }

    public void recordRequestStart(String requestId) {
        totalRequests.increment();
        activeRequests.incrementAndGet();
        requestStartTimes.put(requestId, LocalDateTime.now());

        if (activeRequests.get() > MAX_CONCURRENT_REQUESTS) {
            log.warn("Обнаружено высокое количество одновременных запросов: {}", activeRequests.get());
        }
    }

    public void recordRequestSuccess(String requestId, long processingTimeMs) {
        successfulRequests.increment();
        activeRequests.decrementAndGet();
        requestStartTimes.remove(requestId);
        requestProcessingTime.record(processingTimeMs, TimeUnit.MILLISECONDS);

        if (processingTimeMs > MAX_RESPONSE_TIME_MS) {
            log.warn("Нарушение SLA: Запрос {} выполнялся {} мс", requestId, processingTimeMs);
        }
    }

    public void recordRequestFailure(String requestId, String errorType) {
        failedRequests.increment();
        activeRequests.decrementAndGet();
        requestStartTimes.remove(requestId);

        meterRegistry.counter("sla.errors", "type", errorType).increment();
    }

    public double calculateErrorRate() {
        double total = totalRequests.count();
        double failed = failedRequests.count();
        return total > 0 ? failed / total : 0.0;
    }

    public boolean isSlaViolated() {
        double errorRate = calculateErrorRate();
        return errorRate > MAX_ERROR_RATE;
    }

    public void logSlaStatus() {
        double errorRate = calculateErrorRate();
        int concurrent = activeRequests.get();

        log.info("Статус SLA - Уровень ошибок: {:.2f}%, Активные запросы: {}, Нарушение SLA: {}",
                errorRate * 100, concurrent, isSlaViolated());

        if (isSlaViolated()) {
            log.error("ОБНАРУЖЕНО НАРУШЕНИЕ SLA: Превышен порог уровня ошибок");
        }
    }
}