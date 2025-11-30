package com.bizscore.service;

import com.bizscore.entity.ScoringRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * Компонент для обработки ответов от ML сервиса
 * Отвечает за парсинг и применение результатов ML скоринга
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScoringProcessor {

    private final MetricsService metricsService;

    /**
     * Обрабатывает ответ от ML сервиса и применяет результаты к запросу
     * 
     * @param scoringRequest запрос на скоринг
     * @param mlResponse ответ от ML сервиса
     * @return true если обработка успешна, false если использован fallback
     */
    public boolean processMlResponse(ScoringRequest scoringRequest, Map<String, Object> mlResponse) {
        MDC.put("scoringRequestId", String.valueOf(scoringRequest.getId()));
        log.info("Начало обработки ответа ML сервиса для запроса ID: {}", scoringRequest.getId());
        
        // Извлекаем скоринг и решение из ответа ML сервиса
        Integer mlScore = extractScoreFromResponse(mlResponse);
        String decision = extractDecisionFromResponse(mlResponse);

        if (mlScore != null && decision != null) {
            // Нормализуем скоринг из диапазона 0-1000 в диапазон 0-1
            Double normalizedScore = mlScore / 1000.0;
            String riskLevel = convertToRiskLevel(decision);
            scoringRequest.setScore(normalizedScore);
            scoringRequest.setRiskLevel(riskLevel);
            
            MDC.put("mlScore", String.valueOf(normalizedScore));
            MDC.put("riskLevel", riskLevel);
            log.info("Ответ ML сервиса успешно обработан. Скоринг: {}, Уровень риска: {} для запроса ID: {}", 
                    normalizedScore, riskLevel, scoringRequest.getId());
            return true;
        } else {
            log.warn("Некорректный ответ ML сервиса (отсутствует скоринг или решение) для запроса ID: {}. " +
                    "Используется fallback скоринг", scoringRequest.getId());
            MDC.put("mlResponseInvalid", "true");
            return false;
        }
    }

    /**
     * Применяет fallback скоринг к запросу при недоступности ML сервиса
     * 
     * @param scoringRequest запрос на скоринг
     */
    public void applyFallbackScoring(ScoringRequest scoringRequest) {
        MDC.put("scoringRequestId", String.valueOf(scoringRequest.getId()));
        log.info("Применение fallback скоринга для запроса ID: {}", scoringRequest.getId());
        
        // Расчет простого скоринга на основе базовых параметров
        Double fallbackScore = calculateSimpleScore(scoringRequest);
        String riskLevel = determineRiskLevel(fallbackScore);
        scoringRequest.setScore(fallbackScore);
        scoringRequest.setRiskLevel(riskLevel);
        
        metricsService.incrementFallbackScoring();
        MDC.put("fallbackScore", String.valueOf(fallbackScore));
        MDC.put("riskLevel", riskLevel);
        log.info("Fallback скоринг применен. Скоринг: {}, Уровень риска: {} для запроса ID: {}", 
                fallbackScore, riskLevel, scoringRequest.getId());
    }

    // Извлечение скоринга из ответа ML сервиса (поддержка различных форматов полей)
    private Integer extractScoreFromResponse(Map<String, Object> mlResponse) {
        return Optional.ofNullable(mlResponse.get("score"))
                .or(() -> Optional.ofNullable(mlResponse.get("Score")))
                .or(() -> Optional.ofNullable(mlResponse.get("final_score")))
                .filter(Integer.class::isInstance)
                .map(Integer.class::cast)
                .orElse(null);
    }

    // Извлечение решения из ответа ML сервиса (поддержка различных форматов полей)
    private String extractDecisionFromResponse(Map<String, Object> mlResponse) {
        return Optional.ofNullable(mlResponse.get("decision"))
                .or(() -> Optional.ofNullable(mlResponse.get("Decision")))
                .or(() -> Optional.ofNullable(mlResponse.get("risk_level")))
                .or(() -> Optional.ofNullable(mlResponse.get("status")))
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .orElse(null);
    }

    // Преобразование решения ML сервиса в уровень риска (LOW, MEDIUM, HIGH)
    private String convertToRiskLevel(String decision) {
        if (decision == null) return "MEDIUM";

        String upperDecision = decision.toUpperCase();
        if (upperDecision.contains("APPROVE") || "LOW".equals(upperDecision)) return "LOW";
        if (upperDecision.contains("REJECT") || "HIGH".equals(upperDecision)) return "HIGH";
        if (upperDecision.contains("MANUAL") || upperDecision.contains("REVIEW") || "MEDIUM".equals(upperDecision))
            return "MEDIUM";
        return "MEDIUM";
    }

    // Расчет простого скоринга на основе базовых параметров компании
    private Double calculateSimpleScore(ScoringRequest request) {
        double score = 0.5; // Базовый скоринг

        // Увеличение скоринга при положительных факторах
        if (request.getAnnualRevenue() != null && request.getAnnualRevenue() > 1_000_000) score += 0.2;
        if (request.getEmployeeCount() != null && request.getEmployeeCount() > 10) score += 0.1;
        if (request.getYearsInBusiness() != null && request.getYearsInBusiness() > 3) score += 0.1;
        if (request.getCreditHistory() != null && request.getCreditHistory() > 2) score += 0.1;
        
        // Уменьшение скоринга при отрицательных факторах
        if (Boolean.TRUE.equals(request.getHasExistingLoans())) score -= 0.1;

        // Ограничение скоринга диапазоном [0.0, 1.0]
        return Math.max(0.0, Math.min(score, 1.0));
    }

    // Определение уровня риска на основе скоринга
    private String determineRiskLevel(Double score) {
        if (score >= 0.7) return "LOW";
        else if (score >= 0.4) return "MEDIUM";
        else return "HIGH";
    }
}

