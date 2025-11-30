package com.bizscore.client.impl;

import com.bizscore.client.MlServiceClient;
import com.bizscore.entity.ScoringRequest;
import com.bizscore.service.MetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Реализация клиента для ML сервиса
 * Обеспечивает взаимодействие с внешним ML сервисом для расчета скоринга
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MlServiceClientImpl implements MlServiceClient {

    private final RestTemplate restTemplate;
    private final MetricsService metricsService;

    // URL ML сервиса из конфигурации
    @Value("${ml.service.url:http://localhost:8000}")
    private String mlServiceUrl;

    // Вызов ML сервиса для расчета скоринга с автоматическими повторами при ошибках
    @Override
    @Retryable(
            retryFor = {RestClientException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public Optional<Map<String, Object>> calculateScore(ScoringRequest scoringRequest) {
        MDC.put("mlServiceCall", "true");
        try {
            metricsService.incrementMlServiceCalls();
            // Формируем данные для запроса к ML сервису
            Map<String, Object> mlData = buildMLRequestData(scoringRequest);
            String mlUrl = mlServiceUrl + "/api/v1/score";
            MDC.put("mlServiceUrl", mlUrl);

            // Настройка HTTP заголовков
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(mlData, headers);

            log.info("Вызов ML сервиса по адресу: {} для запроса ID: {}", mlUrl, scoringRequest.getId());
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    mlUrl, HttpMethod.POST, entity, 
                    new ParameterizedTypeReference<Map<String, Object>>() {});

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("ML сервис вернул успешный ответ для запроса ID: {}", scoringRequest.getId());
                MDC.put("mlServiceStatus", "success");
                return Optional.of(response.getBody());
            } else {
                log.warn("ML сервис вернул неуспешный статус: {} для запроса ID: {}", 
                        response.getStatusCode(), scoringRequest.getId());
                MDC.put("mlServiceStatus", "error");
                return Optional.empty();
            }

        } catch (RestClientException e) {
            // Повторный выброс исключения для механизма retry
            log.error("Ошибка при вызове ML сервиса для запроса ID: {}. Сообщение: {}", 
                    scoringRequest.getId(), e.getMessage(), e);
            MDC.put("mlServiceStatus", "error");
            MDC.put("mlServiceError", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Неожиданная ошибка при вызове ML сервиса для запроса ID: {}", 
                    scoringRequest.getId(), e);
            MDC.put("mlServiceStatus", "error");
            MDC.put("mlServiceError", e.getMessage());
            return Optional.empty();
        } finally {
            MDC.remove("mlServiceCall");
            MDC.remove("mlServiceUrl");
            MDC.remove("mlServiceStatus");
            MDC.remove("mlServiceError");
        }
    }

    // Формирование данных запроса для ML сервиса из сущности ScoringRequest
    private Map<String, Object> buildMLRequestData(ScoringRequest request) {
        log.debug("Формирование данных запроса для ML сервиса для компании: {}", request.getCompanyName());
        Map<String, Object> mlData = new HashMap<>();
        mlData.put("companyName", request.getCompanyName());
        mlData.put("inn", request.getInn());
        mlData.put("businessType", request.getBusinessType());
        mlData.put("yearsInBusiness", request.getYearsInBusiness());
        mlData.put("annualRevenue", request.getAnnualRevenue());
        mlData.put("employeeCount", request.getEmployeeCount());
        mlData.put("requestedAmount", request.getRequestedAmount());
        mlData.put("hasExistingLoans", request.getHasExistingLoans());
        mlData.put("industry", request.getIndustry());
        mlData.put("creditHistory", request.getCreditHistory());
        return mlData;
    }
}

