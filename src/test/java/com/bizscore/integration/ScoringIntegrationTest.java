package com.bizscore.integration;

import com.bizscore.dto.request.CalculateScoreRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Интеграционные тесты для скоринга
 * Проверяет полный цикл работы приложения
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ScoringIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser
    void calculateScore_WithValidRequest_ReturnsScore() throws Exception {
        // Given
        CalculateScoreRequest request = new CalculateScoreRequest();
        request.setCompanyName("Test Company");
        request.setInn("7707083893");
        request.setAnnualRevenue(5000000.0);
        request.setYearsInBusiness(3);
        request.setEmployeeCount(25);
        request.setRequestedAmount(1000000.0);
        request.setHasExistingLoans(false);

        // When & Then
        mockMvc.perform(post("/api/score")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("Test Company"))
                .andExpect(jsonPath("$.score").exists())
                .andExpect(jsonPath("$.riskLevel").exists());
    }

    @Test
    @WithMockUser
    void calculateScore_WithInvalidRequest_ReturnsBadRequest() throws Exception {
        // Given
        CalculateScoreRequest request = new CalculateScoreRequest();
        // Не заполнены обязательные поля

        // When & Then
        mockMvc.perform(post("/api/score")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}