package com.bizscore.service;

import com.bizscore.dto.request.CalculateScoreRequest;
import com.bizscore.dto.response.ScoringResponse;
import com.bizscore.entity.ScoringRequest;
import com.bizscore.mapper.ScoringMapper;
import com.bizscore.repository.ScoringRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScoringService {

    private final ScoringRepository repository;
    private final ScoringMapper mapper;

    @Value("${ml.service.url:http://localhost:8000}")
    private String mlServiceUrl;

    public ScoringResponse calculateScore(CalculateScoreRequest request) {
        MDC.put("companyName", request.getCompanyName());
        MDC.put("inn", request.getInn());

        log.info("Converting request to entity");
        ScoringRequest entity = mapper.toEntity(request);

        log.info("Saving scoring request to database");
        ScoringRequest savedEntity = repository.save(entity);

        try {
            log.info("Calling ML service for scoring");
            Map<String, Object> mlResponse = callMLService(savedEntity);

            if (mlResponse != null) {
                processMLResponse(savedEntity, mlResponse);
            } else {
                useFallbackScoring(savedEntity);
            }

            ScoringRequest resultEntity = repository.save(savedEntity);
            return mapper.toResponse(resultEntity);

        } catch (Exception e) {
            log.error("Error in score calculation, using fallback", e);
            useFallbackScoring(savedEntity);
            ScoringRequest resultEntity = repository.save(savedEntity);
            return mapper.toResponse(resultEntity);
        } finally {
            MDC.remove("companyName");
            MDC.remove("inn");
        }
    }

    private void processMLResponse(ScoringRequest request, Map<String, Object> mlResponse) {
        Integer mlScore = extractScoreFromResponse(mlResponse);
        String decision = extractDecisionFromResponse(mlResponse);

        if (mlScore != null && decision != null) {
            Double normalizedScore = mlScore / 1000.0;
            String riskLevel = convertToRiskLevel(decision);
            request.setScore(normalizedScore);
            request.setRiskLevel(riskLevel);
        } else {
            useFallbackScoring(request);
        }
    }

    private Integer extractScoreFromResponse(Map<String, Object> mlResponse) {
        if (mlResponse.containsKey("score")) return (Integer) mlResponse.get("score");
        if (mlResponse.containsKey("Score")) return (Integer) mlResponse.get("Score");
        if (mlResponse.containsKey("final_score")) return (Integer) mlResponse.get("final_score");
        return null;
    }

    private String extractDecisionFromResponse(Map<String, Object> mlResponse) {
        if (mlResponse.containsKey("decision")) return (String) mlResponse.get("decision");
        if (mlResponse.containsKey("Decision")) return (String) mlResponse.get("Decision");
        if (mlResponse.containsKey("risk_level")) return (String) mlResponse.get("risk_level");
        if (mlResponse.containsKey("status")) return (String) mlResponse.get("status");
        return null;
    }

    private String convertToRiskLevel(String decision) {
        String upperDecision = decision.toUpperCase();
        if (upperDecision.contains("APPROVE") || "LOW".equals(upperDecision)) return "LOW";
        if (upperDecision.contains("REJECT") || "HIGH".equals(upperDecision)) return "HIGH";
        if (upperDecision.contains("MANUAL") || upperDecision.contains("REVIEW") || "MEDIUM".equals(upperDecision)) return "MEDIUM";
        return "MEDIUM";
    }

    private void useFallbackScoring(ScoringRequest request) {
        Double fallbackScore = calculateSimpleScore(request);
        request.setScore(fallbackScore);

        String riskLevel;
        if (fallbackScore >= 0.7) riskLevel = "LOW";
        else if (fallbackScore >= 0.5) riskLevel = "MEDIUM";
        else riskLevel = "HIGH";

        request.setRiskLevel(riskLevel);
    }

    private Map<String, Object> callMLService(ScoringRequest request) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(5000);
            requestFactory.setReadTimeout(10000);
            restTemplate.setRequestFactory(requestFactory);

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

            String mlUrl = mlServiceUrl + "/api/v1/score";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(mlData, headers);

            ResponseEntity<Map> response = restTemplate.exchange(mlUrl, HttpMethod.POST, entity, Map.class);
            return response.getBody();

        } catch (Exception e) {
            log.error("Error calling ML service", e);
            return null;
        }
    }

    private Double calculateSimpleScore(ScoringRequest request) {
        double score = 0.5;
        if (request.getAnnualRevenue() != null && request.getAnnualRevenue() > 1000000) score += 0.2;
        if (request.getEmployeeCount() != null && request.getEmployeeCount() > 10) score += 0.1;
        if (request.getYearsInBusiness() != null && request.getYearsInBusiness() > 3) score += 0.1;
        return Math.min(score, 1.0);
    }

    public ScoringResponse getById(Long id) {
        ScoringRequest entity = repository.findById(id).orElse(null);
        return entity != null ? mapper.toResponse(entity) : null;
    }
}