package com.bizscore.controller;

import com.bizscore.config.AdvancedSecurityConfig;
import com.bizscore.config.RateLimitFilter;
import com.bizscore.dto.request.BatchScoringRequest;
import com.bizscore.dto.request.CalculateScoreRequest;
import com.bizscore.dto.response.BatchScoringResponse;
import com.bizscore.service.AdvancedScoringService;
import com.bizscore.service.CustomUserDetailsService;
import com.bizscore.service.JwtService;
import com.bizscore.service.RateLimitService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Тесты для расширенного контроллера скоринга
 * Проверяет API endpoints и безопасность
 */
@WebMvcTest(controllers = AdvancedScoringController.class)
@Import(AdvancedSecurityConfig.class)
class AdvancedScoringControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdvancedScoringService scoringService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @MockBean
    private RateLimitFilter rateLimitFilter;

    @MockBean
    private RateLimitService rateLimitService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = "MANAGER")
    void calculateBatchScore_WithValidRequest_ReturnsOk() throws Exception {
        // Given
        CalculateScoreRequest request1 = new CalculateScoreRequest();
        request1.setCompanyName("Test Company");
        request1.setInn("7707083893");
        request1.setYearsInBusiness(5);
        request1.setAnnualRevenue(5000000.0);
        request1.setEmployeeCount(50);
        request1.setRequestedAmount(1000000.0);

        BatchScoringRequest batchRequest = new BatchScoringRequest();
        batchRequest.setRequests(Arrays.asList(request1));

        BatchScoringResponse response = new BatchScoringResponse();
        response.setBatchId("test-batch-id");
        response.setTotalRequests(1);
        response.setSuccessfulResults(new ArrayList<>());
        response.setFailedResults(new ArrayList<>());
        response.setProcessedAt(new Date());
        response.setStatus("COMPLETED");

        when(scoringService.processBatchScoring(any(BatchScoringRequest.class)))
                .thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v2/scoring/batch")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batchId").exists())  // Проверяем наличие поля
                .andExpect(jsonPath("$.batchId").value("test-batch-id"))
                .andExpect(jsonPath("$.totalRequests").value(1));
    }

    @Test
    @WithMockUser(roles = "USER")
    void calculateBatchScore_WithInsufficientPermissions_ReturnsForbidden() throws Exception {
        // Given
        BatchScoringRequest batchRequest = new BatchScoringRequest();
        batchRequest.setRequests(new ArrayList<>());

        // When & Then
        // В WebMvcTest с отключенными фильтрами, @PreAuthorize все еще должен работать
        // Но если фильтры отключены, Security может не проверить роли правильно
        // Проверяем, что запрос отклонен (403 Forbidden)
        mockMvc.perform(post("/api/v2/scoring/batch")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batchRequest)))
                .andExpect(status().isForbidden());
    }
}