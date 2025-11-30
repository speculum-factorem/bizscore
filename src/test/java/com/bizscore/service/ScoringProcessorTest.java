package com.bizscore.service;

import com.bizscore.entity.ScoringRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScoringProcessorTest {

    @Mock
    private MetricsService metricsService;

    @InjectMocks
    private ScoringProcessor scoringProcessor;

    private ScoringRequest scoringRequest;

    @BeforeEach
    void setUp() {
        scoringRequest = new ScoringRequest();
        scoringRequest.setId(1L);
        scoringRequest.setCompanyName("Test Company");
        scoringRequest.setInn("1234567890");
    }

    @Test
    void testProcessMlResponse_Success() {
        // Подготовка данных
        Map<String, Object> mlResponse = new HashMap<>();
        mlResponse.put("score", 750);
        mlResponse.put("decision", "APPROVE");

        // Выполнение
        boolean result = scoringProcessor.processMlResponse(scoringRequest, mlResponse);

        // Проверка
        assertTrue(result);
        assertEquals(0.75, scoringRequest.getScore());
        assertEquals("LOW", scoringRequest.getRiskLevel());
    }

    @Test
    void testProcessMlResponse_InvalidResponse() {
        // Подготовка данных - некорректный ответ
        Map<String, Object> mlResponse = new HashMap<>();
        mlResponse.put("score", null);
        mlResponse.put("decision", null);

        // Выполнение
        boolean result = scoringProcessor.processMlResponse(scoringRequest, mlResponse);

        // Проверка
        assertFalse(result);
    }

    @Test
    void testApplyFallbackScoring() {
        // Подготовка данных
        scoringRequest.setAnnualRevenue(2_000_000.0);
        scoringRequest.setEmployeeCount(20);
        scoringRequest.setYearsInBusiness(5);
        scoringRequest.setCreditHistory(3);
        scoringRequest.setHasExistingLoans(false);

        // Выполнение
        scoringProcessor.applyFallbackScoring(scoringRequest);

        // Проверка
        assertNotNull(scoringRequest.getScore());
        assertTrue(scoringRequest.getScore() >= 0.0 && scoringRequest.getScore() <= 1.0);
        assertNotNull(scoringRequest.getRiskLevel());
        assertTrue(scoringRequest.getRiskLevel().matches("LOW|MEDIUM|HIGH"));
        verify(metricsService, times(1)).incrementFallbackScoring();
    }

    @Test
    void testProcessMlResponse_DifferentFieldNames() {
        // Тест с различными именами полей в ответе
        Map<String, Object> mlResponse1 = new HashMap<>();
        mlResponse1.put("Score", 800);
        mlResponse1.put("Decision", "APPROVE");

        boolean result1 = scoringProcessor.processMlResponse(scoringRequest, mlResponse1);
        assertTrue(result1);

        Map<String, Object> mlResponse2 = new HashMap<>();
        mlResponse2.put("final_score", 300);
        mlResponse2.put("risk_level", "HIGH");

        boolean result2 = scoringProcessor.processMlResponse(scoringRequest, mlResponse2);
        assertTrue(result2);
    }
}

