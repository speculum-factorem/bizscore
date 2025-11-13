package com.bizscore.service;

import com.bizscore.dto.request.BatchScoringRequest;
import com.bizscore.dto.request.CalculateScoreRequest;
import com.bizscore.dto.response.BatchScoringResponse;
import com.bizscore.dto.response.EnhancedScoringResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Тесты для расширенного сервиса скоринга
 * Проверяет пакетную обработку и аналитические функции
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class AdvancedScoringServiceTest {

    @Mock
    private ScoringService scoringService;

    @InjectMocks
    private AdvancedScoringService advancedScoringService;

    @Test
    void processBatchScoring_WithValidRequests_ReturnsSuccessfulResponse() {
        // Given
        CalculateScoreRequest request1 = createTestRequest("Company A", "1234567890");
        CalculateScoreRequest request2 = createTestRequest("Company B", "0987654321");
        BatchScoringRequest batchRequest = new BatchScoringRequest();
        batchRequest.setRequests(Arrays.asList(request1, request2));

        EnhancedScoringResponse response1 = createTestResponse("Company A", 0.85);
        EnhancedScoringResponse response2 = createTestResponse("Company B", 0.75);

        when(scoringService.calculateScore(any(CalculateScoreRequest.class)))
                .thenReturn(response1)
                .thenReturn(response2);

        // When
        BatchScoringResponse result = advancedScoringService.processBatchScoring(batchRequest);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getTotalRequests());
        assertEquals(2, result.getSuccessfulResults().size());
        assertEquals(0, result.getFailedResults().size());
        assertEquals("COMPLETED", result.getStatus());
    }

    @Test
    void analyzeScoringTrends_WithValidPeriod_ReturnsTrendsAnalysis() {
        // When
        var result = advancedScoringService.analyzeScoringTrends("weekly", 30);

        // Then
        assertNotNull(result);
        assertEquals("weekly", result.get("period"));
        assertEquals(30, result.get("days"));
        assertTrue(result.containsKey("riskDistribution"));
        assertTrue(result.containsKey("scoreTrends"));
        assertTrue(result.containsKey("industryStats"));
    }

    @Test
    void compareCompanies_WithValidData_ReturnsComparison() {
        // Given
        var request = new java.util.HashMap<String, Object>();
        request.put("companyIds", Arrays.asList(1L, 2L));

        // When
        var result = advancedScoringService.compareCompanies(request);

        // Then
        assertNotNull(result);
        assertTrue(result.containsKey("comparisonId"));
        assertTrue(result.containsKey("companies"));
        assertTrue(result.containsKey("summary"));
    }

    private CalculateScoreRequest createTestRequest(String companyName, String inn) {
        CalculateScoreRequest request = new CalculateScoreRequest();
        request.setCompanyName(companyName);
        request.setInn(inn);
        request.setAnnualRevenue(1000000.0);
        request.setYearsInBusiness(5);
        request.setEmployeeCount(50);
        return request;
    }

    private EnhancedScoringResponse createTestResponse(String companyName, double score) {
        EnhancedScoringResponse response = new EnhancedScoringResponse();
        response.setCompanyName(companyName);
        response.setScore(score);
        response.setRiskLevel(score > 0.7 ? "LOW" : "MEDIUM");
        return response;
    }
}