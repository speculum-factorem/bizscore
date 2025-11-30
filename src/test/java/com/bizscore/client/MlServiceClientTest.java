package com.bizscore.client;

import com.bizscore.client.impl.MlServiceClientImpl;
import com.bizscore.entity.ScoringRequest;
import com.bizscore.service.MetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")

@ExtendWith(MockitoExtension.class)
class MlServiceClientTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private MetricsService metricsService;

    @InjectMocks
    private MlServiceClientImpl mlServiceClient;

    private ScoringRequest scoringRequest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(mlServiceClient, "mlServiceUrl", "http://localhost:8000");
        scoringRequest = new ScoringRequest();
        scoringRequest.setId(1L);
        scoringRequest.setCompanyName("Test Company");
        scoringRequest.setInn("1234567890");
    }

    @Test
    void testCalculateScore_Success() {
        // Подготовка данных
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("score", 750);
        responseBody.put("decision", "APPROVE");

        ResponseEntity<Map<String, Object>> response = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(response);

        // Выполнение
        Optional<Map<String, Object>> result = mlServiceClient.calculateScore(scoringRequest);

        // Проверка
        assertTrue(result.isPresent());
        assertEquals(750, result.get().get("score"));
        verify(metricsService, times(1)).incrementMlServiceCalls();
    }

    @Test
    void testCalculateScore_EmptyResponse() {
        // Подготовка данных - пустой ответ
        ResponseEntity<Map<String, Object>> response = new ResponseEntity<>(null, HttpStatus.OK);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(response);

        // Выполнение
        Optional<Map<String, Object>> result = mlServiceClient.calculateScore(scoringRequest);

        // Проверка
        assertFalse(result.isPresent());
    }

    @Test
    void testCalculateScore_RestClientException() {
        // Подготовка данных - исключение
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Connection error"));

        // Выполнение и проверка
        assertThrows(RestClientException.class, () -> {
            mlServiceClient.calculateScore(scoringRequest);
        });
    }
}

