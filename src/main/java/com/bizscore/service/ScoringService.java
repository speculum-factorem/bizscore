package com.bizscore.service;

import com.bizscore.entity.ScoringRequest;
import com.bizscore.repository.ScoringRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.util.HashMap;
import java.util.Map;

@Service
public class ScoringService {

    private final ScoringRepository repository;

    @Value("${ml.service.url:http://localhost:8000}")
    private String mlServiceUrl;

    public ScoringService(ScoringRepository repository) {
        this.repository = repository;
    }

    public ScoringRequest calculateScore(ScoringRequest request) {
        // Сохраняем запрос в БД (со всеми полями)
        ScoringRequest savedRequest = repository.save(request);

        try {
            // Отправляем данные в ML сервис
            Map<String, Object> mlResponse = callMLService(savedRequest);

            // Обновляем результат из ML сервиса
            if (mlResponse != null) {
                // ДЕТАЛЬНАЯ ОБРАБОТКА ОТВЕТА ML С ПРОВЕРКОЙ РАЗНЫХ ВАРИАНТОВ
                Integer mlScore = null;
                String decision = null;

                // Ищем score в разных возможных вариантах
                if (mlResponse.containsKey("score")) {
                    mlScore = (Integer) mlResponse.get("score");
                } else if (mlResponse.containsKey("Score")) {
                    mlScore = (Integer) mlResponse.get("Score");
                } else if (mlResponse.containsKey("final_score")) {
                    mlScore = (Integer) mlResponse.get("final_score");
                }

                // Ищем decision в разных возможных вариантах
                if (mlResponse.containsKey("decision")) {
                    decision = (String) mlResponse.get("decision");
                } else if (mlResponse.containsKey("Decision")) {
                    decision = (String) mlResponse.get("Decision");
                } else if (mlResponse.containsKey("risk_level")) {
                    decision = (String) mlResponse.get("risk_level");
                } else if (mlResponse.containsKey("status")) {
                    decision = (String) mlResponse.get("status");
                }

                System.out.println("Найдены в ответе ML - score: " + mlScore + ", decision: " + decision);

                if (mlScore != null && decision != null) {
                    // Конвертируем score из 610 в 0.61
                    Double normalizedScore = mlScore / 1000.0;

                    // ПРАВИЛЬНАЯ конвертация decision в riskLevel
                    String riskLevel;
                    decision = decision.toUpperCase(); // приводим к верхнему регистру

                    if (decision.contains("APPROVE") || "LOW".equals(decision)) {
                        riskLevel = "LOW";
                    } else if (decision.contains("REJECT") || "HIGH".equals(decision)) {
                        riskLevel = "HIGH";
                    } else if (decision.contains("MANUAL") || decision.contains("REVIEW") || "MEDIUM".equals(decision)) {
                        riskLevel = "MEDIUM";
                    } else {
                        riskLevel = "MEDIUM"; // по умолчанию
                    }

                    savedRequest.setScore(normalizedScore);
                    savedRequest.setRiskLevel(riskLevel);

                    System.out.println("Конвертировано: score=" + normalizedScore + ", riskLevel=" + riskLevel);
                } else {
                    System.out.println("Не найдены необходимые поля в ответе ML, используем fallback");
                    useFallbackScoring(savedRequest);
                }
            } else {
                // Запасной вариант если ML сервис не доступен
                useFallbackScoring(savedRequest);
                System.out.println("Используется fallback алгоритм");
            }

            // Сохраняем обновленные данные
            return repository.save(savedRequest);

        } catch (Exception e) {
            // Если ML сервис недоступен - используем простой расчет
            useFallbackScoring(savedRequest);
            System.out.println("Ошибка ML, используется fallback: " + e.getMessage());
            return repository.save(savedRequest);
        }
    }

    private void useFallbackScoring(ScoringRequest request) {
        Double fallbackScore = calculateSimpleScore(request);
        request.setScore(fallbackScore);

        // Определяем riskLevel для fallback
        String riskLevel;
        if (fallbackScore >= 0.7) {
            riskLevel = "LOW";
        } else if (fallbackScore >= 0.5) {
            riskLevel = "MEDIUM";
        } else {
            riskLevel = "HIGH";
        }
        request.setRiskLevel(riskLevel);

        System.out.println("Fallback: score=" + fallbackScore + ", riskLevel=" + riskLevel);
    }

    private Map<String, Object> callMLService(ScoringRequest request) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            // Настройка таймаутов для избежания зависаний
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(5000); // 5 секунд на подключение
            requestFactory.setReadTimeout(10000);   // 10 секунд на чтение
            restTemplate.setRequestFactory(requestFactory);

            // Используем ВСЕ поля из запроса
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

            System.out.println("Отправляю запрос в ML сервис: " + mlServiceUrl + "/api/v1/score");
            System.out.println("Данные для ML: " + mlData);

            // Отправляем запрос
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(mlData, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    mlServiceUrl + "/api/v1/score",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            // ДЕТАЛЬНАЯ ИНФОРМАЦИЯ ОТВЕТА
            System.out.println("ML сервис ответил: " + response.getStatusCode());
            System.out.println("Полный ответ ML: " + response.getBody());

            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null) {
                System.out.println("Ключи в ответе ML: " + responseBody.keySet());
                for (Map.Entry<String, Object> entry : responseBody.entrySet()) {
                    System.out.println("   " + entry.getKey() + " = " + entry.getValue() + " (тип: " + (entry.getValue() != null ? entry.getValue().getClass().getSimpleName() : "null") + ")");
                }
            }

            return responseBody;

        } catch (Exception e) {
            System.out.println("Ошибка при вызове ML сервиса: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            return null;
        }
    }

    private Double calculateSimpleScore(ScoringRequest request) {
        // Простая логика расчета скора если ML сервис недоступен
        double score = 0.5; // базовый score

        if (request.getRevenue() > 1000000) score += 0.2;
        if (request.getEmployees() > 10) score += 0.1;
        if (request.getBusinessAge() > 3) score += 0.1;

        return Math.min(score, 1.0); // максимум 1.0
    }

    public ScoringRequest getById(Long id) {
        return repository.findById(id).orElse(null);
    }
}