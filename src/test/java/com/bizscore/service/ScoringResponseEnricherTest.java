package com.bizscore.service;

import com.bizscore.dto.response.ScoringResponse;
import com.bizscore.entity.ScoringDecision;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ScoringResponseEnricherTest {

    @InjectMocks
    private ScoringResponseEnricher responseEnricher;

    private ScoringResponse scoringResponse;
    private ScoringDecision scoringDecision;

    @BeforeEach
    void setUp() {
        scoringResponse = new ScoringResponse();
        scoringResponse.setId(1L);
        scoringResponse.setCompanyName("Test Company");
        scoringResponse.setInn("1234567890");
        scoringResponse.setScore(0.75);
        scoringResponse.setRiskLevel("LOW");
        scoringResponse.setCreatedAt(LocalDateTime.now());

        scoringDecision = new ScoringDecision();
        scoringDecision.setId(1L);
        scoringDecision.setScoringRequestId(1L);
        scoringDecision.setDecision("AUTO_APPROVE");
        scoringDecision.setReason("Политика автоматического одобрения");
        scoringDecision.setAppliedPolicy("HIGH_REVENUE_POLICY");
        scoringDecision.setPriority("HIGH");
        scoringDecision.setFinalDecision("APPROVED");
        scoringDecision.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void testEnrich_WithDecision() {
        // Выполнение
        var result = responseEnricher.enrich(scoringResponse, scoringDecision);

        // Проверка
        assertNotNull(result);
        assertEquals(scoringResponse.getId(), result.getId());
        assertEquals("AUTO_APPROVED", result.getProcessingStatus());
        assertEquals("HIGH", result.getPriority());
        assertNotNull(result.getDecisionDetails());
        assertEquals("AUTO_APPROVE", result.getDecisionDetails().getDecision());
    }

    @Test
    void testEnrich_WithoutDecision() {
        // Выполнение
        var result = responseEnricher.enrich(scoringResponse, null);

        // Проверка
        assertNotNull(result);
        assertEquals("MANUAL_REVIEW", result.getProcessingStatus());
        assertEquals("MEDIUM", result.getPriority());
        assertEquals("Решение политики недоступно", result.getDecisionReason());
    }

    @Test
    void testEnrich_NullResponse() {
        // Проверка исключения при null ответе
        assertThrows(IllegalArgumentException.class, () -> {
            responseEnricher.enrich(null, scoringDecision);
        });
    }

    @Test
    void testEnrich_DifferentDecisions() {
        // Тест с различными типами решений
        scoringDecision.setDecision("AUTO_REJECT");
        var result1 = responseEnricher.enrich(scoringResponse, scoringDecision);
        assertEquals("AUTO_REJECTED", result1.getProcessingStatus());

        scoringDecision.setDecision("ESCALATE_TO_MANAGER");
        var result2 = responseEnricher.enrich(scoringResponse, scoringDecision);
        assertEquals("ESCALATED", result2.getProcessingStatus());
    }
}

