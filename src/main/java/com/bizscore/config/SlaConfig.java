package com.bizscore.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация метрик для SLA мониторинга
 * Настраивает общие теги для всех метрик приложения
 */
@Configuration
public class SlaConfig {

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> slaMetrics() {
        return registry -> {
            registry.config().commonTags(
                    "application", "bizscore-service",
                    "environment", "production",
                    "version", "1.0.0",
                    "team", "risk-management"
            );
        };
    }
}