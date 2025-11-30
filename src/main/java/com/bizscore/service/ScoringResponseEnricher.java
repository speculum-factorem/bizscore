package com.bizscore.service;

import com.bizscore.dto.response.EnhancedScoringResponse;
import com.bizscore.dto.response.ScoringDecisionResponse;
import com.bizscore.dto.response.ScoringResponse;
import com.bizscore.entity.ScoringDecision;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * Компонент для обогащения ответов скоринга информацией о решениях политик
 * Отвечает за преобразование базового ответа в расширенный с деталями решений
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScoringResponseEnricher {

    /**
     * Обогащает базовый ответ информацией о решении политик
     * 
     * @param response базовый ответ скоринга
     * @param decision решение политики
     * @return обогащенный ответ
     */
    public EnhancedScoringResponse enrich(ScoringResponse response, ScoringDecision decision) {
        if (response == null) {
            log.error("Невозможно обогатить null ответ");
            throw new IllegalArgumentException("Ответ не может быть null");
        }

        MDC.put("scoringResponseId", String.valueOf(response.getId()));
        log.debug("Начало обогащения ответа для ID: {}", response.getId());

        try {
            EnhancedScoringResponse enhancedResponse = new EnhancedScoringResponse();
            enhancedResponse.setId(response.getId());
            enhancedResponse.setCompanyName(response.getCompanyName());
            enhancedResponse.setInn(response.getInn());
            enhancedResponse.setScore(response.getScore());
            enhancedResponse.setRiskLevel(response.getRiskLevel());
            enhancedResponse.setCreatedAt(response.getCreatedAt());

            // Определяем статус обработки на основе решения политик
            String processingStatus = "MANUAL_REVIEW";
            if (decision != null && decision.getDecision() != null) {
                processingStatus = switch (decision.getDecision()) {
                    case "AUTO_APPROVE" -> "AUTO_APPROVED";
                    case "AUTO_REJECT" -> "AUTO_REJECTED";
                    case "ESCALATE_TO_MANAGER" -> "ESCALATED";
                    default -> "MANUAL_REVIEW";
                };

                enhancedResponse.setPriority(decision.getPriority() != null ? decision.getPriority() : "MEDIUM");
                enhancedResponse.setDecisionReason(decision.getReason() != null ? decision.getReason() : "Применено решение политики");

                // Преобразуем решение политики в DTO для ответа
                ScoringDecisionResponse decisionResponse = createDecisionResponse(decision);
                enhancedResponse.setDecisionDetails(decisionResponse);
                
                MDC.put("processingStatus", processingStatus);
                MDC.put("priority", enhancedResponse.getPriority());
                log.debug("Ответ обогащен информацией о решении политики. Статус: {}, Приоритет: {}", 
                        processingStatus, enhancedResponse.getPriority());
            } else {
                enhancedResponse.setPriority("MEDIUM");
                enhancedResponse.setDecisionReason("Решение политики недоступно");
                log.debug("Решение политики отсутствует, используется значение по умолчанию");
            }

            enhancedResponse.setProcessingStatus(processingStatus);

            log.info("Ответ успешно обогащен для ID: {}", response.getId());
            return enhancedResponse;

        } catch (Exception e) {
            log.warn("Ошибка при обогащении ответа информацией о решении, возвращается fallback ответ для ID: {}", 
                    response.getId(), e);
            MDC.put("enrichmentError", "true");
            return createFallbackResponse(response, e);
        } finally {
            MDC.remove("scoringResponseId");
            MDC.remove("processingStatus");
            MDC.remove("priority");
            MDC.remove("enrichmentError");
        }
    }

    // Преобразование сущности ScoringDecision в DTO для ответа
    private ScoringDecisionResponse createDecisionResponse(ScoringDecision decision) {
        ScoringDecisionResponse decisionResponse = new ScoringDecisionResponse();
        decisionResponse.setId(decision.getId());
        decisionResponse.setScoringRequestId(decision.getScoringRequestId());
        decisionResponse.setDecision(decision.getDecision());
        decisionResponse.setReason(decision.getReason());
        decisionResponse.setAppliedPolicy(decision.getAppliedPolicy());
        decisionResponse.setPriority(decision.getPriority());
        decisionResponse.setManagerNotes(decision.getManagerNotes());
        decisionResponse.setFinalDecision(decision.getFinalDecision());
        decisionResponse.setResolvedBy(decision.getResolvedBy());
        decisionResponse.setResolvedAt(decision.getResolvedAt());
        decisionResponse.setCreatedAt(decision.getCreatedAt());
        return decisionResponse;
    }

    // Создание fallback ответа при ошибке обогащения
    private EnhancedScoringResponse createFallbackResponse(ScoringResponse response, Exception e) {
        EnhancedScoringResponse fallback = new EnhancedScoringResponse();
        fallback.setId(response.getId());
        fallback.setCompanyName(response.getCompanyName());
        fallback.setInn(response.getInn());
        fallback.setScore(response.getScore());
        fallback.setRiskLevel(response.getRiskLevel());
        fallback.setCreatedAt(response.getCreatedAt());
        fallback.setProcessingStatus("MANUAL_REVIEW");
        fallback.setPriority("MEDIUM");
        fallback.setDecisionReason("Ошибка обработки информации о политике: " + e.getMessage());
        return fallback;
    }
}

