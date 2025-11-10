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
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScoringService {

    private final ScoringRepository repository;
    private final ScoringMapper mapper;
    private final RestTemplate restTemplate;

    @Value("${ml.service.url:http://localhost:8000}")
    private String mlServiceUrl;

    public ScoringResponse calculateScore(CalculateScoreRequest request) {
        setupMDC(request.getCompanyName(), request.getInn());

        try {
            log.info("Converting request to entity and saving to database");
            ScoringRequest entity = mapper.toEntity(request);
            ScoringRequest savedEntity = repository.save(entity);

            Optional<Map<String, Object>> mlResponse = callMLService(savedEntity);

            if (mlResponse.isPresent()) {
                processMLResponse(savedEntity, mlResponse.get());
                log.info("ML service response processed successfully");
            } else {
                useFallbackScoring(savedEntity);
                log.info("Fallback scoring used");
            }

            ScoringRequest resultEntity = repository.save(savedEntity);
            return mapper.toResponse(resultEntity);

        } catch (Exception e) {
            log.error("Error in score calculation, using fallback", e);
            ScoringRequest fallbackEntity = mapper.toEntity(request);
            useFallbackScoring(fallbackEntity);
            ScoringRequest resultEntity = repository.save(fallbackEntity);
            return mapper.toResponse(resultEntity);
        } finally {
            cleanupMDC();
        }
    }

    private Optional<Map<String, Object>> callMLService(ScoringRequest request) {
        try {
            Map<String, Object> mlData = buildMLRequestData(request);
            String mlUrl = mlServiceUrl + "/api/v1/score";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(mlData, headers);

            log.debug("Calling ML service at: {}", mlUrl);
            ResponseEntity<Map> response = restTemplate.exchange(mlUrl, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return Optional.of(response.getBody());
            } else {
                log.warn("ML service returned non-success status: {}", response.getStatusCode());
                return Optional.empty();
            }

        } catch (RestClientException e) {
            log.error("Error calling ML service: {}", e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.error("Unexpected error calling ML service", e);
            return Optional.empty();
        }
    }

    private Map<String, Object> buildMLRequestData(ScoringRequest request) {
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

    private void processMLResponse(ScoringRequest request, Map<String, Object> mlResponse) {
        Integer mlScore = extractScoreFromResponse(mlResponse);
        String decision = extractDecisionFromResponse(mlResponse);

        if (mlScore != null && decision != null) {
            Double normalizedScore = mlScore / 1000.0;
            String riskLevel = convertToRiskLevel(decision);
            request.setScore(normalizedScore);
            request.setRiskLevel(riskLevel);
            log.debug("ML response processed - Score: {}, Risk Level: {}", normalizedScore, riskLevel);
        } else {
            log.warn("Invalid ML response, using fallback scoring");
            useFallbackScoring(request);
        }
    }

    private Integer extractScoreFromResponse(Map<String, Object> mlResponse) {
        return Optional.ofNullable(mlResponse.get("score"))
                .or(() -> Optional.ofNullable(mlResponse.get("Score")))
                .or(() -> Optional.ofNullable(mlResponse.get("final_score")))
                .filter(Integer.class::isInstance)
                .map(Integer.class::cast)
                .orElse(null);
    }

    private String extractDecisionFromResponse(Map<String, Object> mlResponse) {
        return Optional.ofNullable(mlResponse.get("decision"))
                .or(() -> Optional.ofNullable(mlResponse.get("Decision")))
                .or(() -> Optional.ofNullable(mlResponse.get("risk_level")))
                .or(() -> Optional.ofNullable(mlResponse.get("status")))
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .orElse(null);
    }

    private String convertToRiskLevel(String decision) {
        if (decision == null) return "MEDIUM";

        String upperDecision = decision.toUpperCase();
        if (upperDecision.contains("APPROVE") || "LOW".equals(upperDecision)) return "LOW";
        if (upperDecision.contains("REJECT") || "HIGH".equals(upperDecision)) return "HIGH";
        if (upperDecision.contains("MANUAL") || upperDecision.contains("REVIEW") || "MEDIUM".equals(upperDecision))
            return "MEDIUM";
        return "MEDIUM";
    }

    private void useFallbackScoring(ScoringRequest request) {
        Double fallbackScore = calculateSimpleScore(request);
        request.setScore(fallbackScore);
        request.setRiskLevel(determineRiskLevel(fallbackScore));
        log.debug("Fallback scoring - Score: {}, Risk Level: {}", fallbackScore, request.getRiskLevel());
    }

    private Double calculateSimpleScore(ScoringRequest request) {
        double score = 0.5;

        if (request.getAnnualRevenue() != null && request.getAnnualRevenue() > 1_000_000) score += 0.2;
        if (request.getEmployeeCount() != null && request.getEmployeeCount() > 10) score += 0.1;
        if (request.getYearsInBusiness() != null && request.getYearsInBusiness() > 3) score += 0.1;
        if (request.getCreditHistory() != null && request.getCreditHistory() > 2) score += 0.1;
        if (Boolean.TRUE.equals(request.getHasExistingLoans())) score -= 0.1;

        return Math.max(0.0, Math.min(score, 1.0));
    }

    private String determineRiskLevel(Double score) {
        if (score >= 0.7) return "LOW";
        else if (score >= 0.4) return "MEDIUM";
        else return "HIGH";
    }

    public ScoringResponse getById(Long id) {
        return repository.findById(id)
                .map(mapper::toResponse)
                .orElse(null);
    }

    private void setupMDC(String companyName, String inn) {
        if (companyName != null) MDC.put("companyName", companyName);
        if (inn != null) MDC.put("inn", inn);
    }

    private void cleanupMDC() {
        MDC.remove("companyName");
        MDC.remove("inn");
    }
}