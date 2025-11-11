package com.bizscore.service;

import com.bizscore.dto.request.CalculateScoreRequest;
import com.bizscore.dto.response.EnhancedScoringResponse;
import com.bizscore.dto.response.ScoringDecisionResponse;
import com.bizscore.dto.response.ScoringResponse;
import com.bizscore.entity.ScoringDecision;
import com.bizscore.entity.ScoringRequest;
import com.bizscore.mapper.ScoringMapper;
import com.bizscore.repository.ScoringDecisionRepository;
import com.bizscore.repository.ScoringRepository;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final MetricsService metricsService;
    private final PolicyEngineService policyEngineService;
    private final ScoringDecisionRepository scoringDecisionRepository;

    @Value("${ml.service.url:http://localhost:8000}")
    private String mlServiceUrl;

    @CacheEvict(value = {"scoringResults", "companyScores"}, allEntries = true)
    public EnhancedScoringResponse calculateScore(CalculateScoreRequest request) {
        metricsService.incrementScoringRequests();
        Timer.Sample timer = metricsService.startScoringTimer();

        setupMDC(request.getCompanyName(), request.getInn());

        try {
            log.info("Converting request to entity and saving to database");
            ScoringRequest entity = mapper.toEntity(request);
            ScoringRequest savedEntity = repository.save(entity);

            // Шаг 1: Применяем политики риска ДО вызова ML сервиса
            ScoringDecision decision = policyEngineService.evaluatePolicies(savedEntity);
            log.info("Policy evaluation completed: {}", decision.getDecision());

            // Шаг 2: ВСЕГДА вызываем ML сервис (как и раньше)
            Optional<Map<String, Object>> mlResponse = callMLService(savedEntity);

            if (mlResponse.isPresent()) {
                processMLResponse(savedEntity, mlResponse.get());
                log.info("ML service response processed successfully");
            } else {
                useFallbackScoring(savedEntity);
                metricsService.incrementFallbackScoring();
                log.info("Fallback scoring used");
            }

            // Шаг 3: Сохраняем финальный результат
            ScoringRequest resultEntity = repository.save(savedEntity);
            ScoringResponse basicResponse = mapper.toResponse(resultEntity);

            // Шаг 4: Обогащаем ответ информацией о решении политик
            EnhancedScoringResponse response = enhanceWithDecisionInfo(basicResponse, decision);

            metricsService.incrementScoringSuccess();
            if (response.getScore() != null) {
                metricsService.recordScoreValue(response.getScore());
            }
            metricsService.stopScoringTimer(timer, response.getRiskLevel());

            return response;

        } catch (Exception e) {
            metricsService.incrementScoringFailure();
            log.error("Error in score calculation, using fallback", e);
            ScoringRequest fallbackEntity = mapper.toEntity(request);
            useFallbackScoring(fallbackEntity);
            metricsService.incrementFallbackScoring();
            ScoringRequest resultEntity = repository.save(fallbackEntity);
            ScoringResponse basicResponse = mapper.toResponse(resultEntity);

            // Создаем базовый decision для fallback случая
            ScoringDecision fallbackDecision = new ScoringDecision();
            fallbackDecision.setScoringRequestId(resultEntity.getId());
            fallbackDecision.setDecision("MANUAL_REVIEW");
            fallbackDecision.setReason("Fallback scoring used due to error");
            fallbackDecision.setAppliedPolicy("SYSTEM_FALLBACK");
            fallbackDecision.setPriority("MEDIUM");
            fallbackDecision.setFinalDecision("PENDING");
            scoringDecisionRepository.save(fallbackDecision);

            return enhanceWithDecisionInfo(basicResponse, fallbackDecision);
        } finally {
            cleanupMDC();
        }
    }

    @Cacheable(value = "scoringResults", key = "#id")
    public ScoringResponse getById(Long id) {
        log.info("Fetching score by ID: {} from database", id);
        return repository.findById(id)
                .map(mapper::toResponse)
                .orElse(null);
    }

    public EnhancedScoringResponse getEnhancedScore(Long id) {
        ScoringResponse basicResponse = getById(id);
        if (basicResponse == null) {
            return null;
        }

        ScoringDecision decision = scoringDecisionRepository.findByScoringRequestId(id)
                .orElse(null);

        return enhanceWithDecisionInfo(basicResponse, decision);
    }

    @Cacheable(value = "companyScores", key = "#companyName + '_' + #inn")
    public ScoringResponse getByCompanyAndInn(String companyName, String inn) {
        log.info("Fetching score for company: {} with INN: {}", companyName, inn);
        return repository.findByCompanyNameAndInn(companyName, inn)
                .map(mapper::toResponse)
                .orElse(null);
    }

    public Page<ScoringResponse> getAllScores(Pageable pageable) {
        log.info("Fetching all scores with pagination: {}", pageable);
        return repository.findAll(pageable)
                .map(mapper::toResponse);
    }

    public Page<ScoringResponse> getScoresByRiskLevel(String riskLevel, Pageable pageable) {
        log.info("Fetching scores with risk level: {} and pagination: {}", riskLevel, pageable);
        return repository.findByRiskLevel(riskLevel, pageable)
                .map(mapper::toResponse);
    }

    public Map<String, Object> getScoringStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRequests", repository.count());
        stats.put("averageScore", repository.findAverageScore());
        stats.put("lowRiskCount", repository.countByRiskLevel("LOW"));
        stats.put("mediumRiskCount", repository.countByRiskLevel("MEDIUM"));
        stats.put("highRiskCount", repository.countByRiskLevel("HIGH"));
        return stats;
    }

    private Optional<Map<String, Object>> callMLService(ScoringRequest request) {
        try {
            metricsService.incrementMlServiceCalls();
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
            metricsService.incrementFallbackScoring();
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

    private EnhancedScoringResponse enhanceWithDecisionInfo(ScoringResponse response, ScoringDecision decision) {
        try {
            EnhancedScoringResponse enhancedResponse = new EnhancedScoringResponse();
            enhancedResponse.setId(response.getId());
            enhancedResponse.setCompanyName(response.getCompanyName());
            enhancedResponse.setInn(response.getInn());
            enhancedResponse.setScore(response.getScore());
            enhancedResponse.setRiskLevel(response.getRiskLevel());
            enhancedResponse.setCreatedAt(response.getCreatedAt());

            // Определяем статус обработки на основе решения политик
            String processingStatus = "MANUAL_REVIEW";
            if (decision != null) {
                processingStatus = switch (decision.getDecision()) {
                    case "AUTO_APPROVE" -> "AUTO_APPROVED";
                    case "AUTO_REJECT" -> "AUTO_REJECTED";
                    case "ESCALATE_TO_MANAGER" -> "ESCALATED";
                    default -> "MANUAL_REVIEW";
                };

                enhancedResponse.setPriority(decision.getPriority());
                enhancedResponse.setDecisionReason(decision.getReason());

                // Преобразуем decision в response DTO
                ScoringDecisionResponse decisionResponse = new ScoringDecisionResponse();
                decisionResponse.setId(decision.getId());
                decisionResponse.setScoringRequestId(decision.getScoringRequestId());
                decisionResponse.setDecision(decision.getDecision());
                decisionResponse.setReason(decision.getReason());
                decisionResponse.setAppliedPolicy(decision.getAppliedPolicy());
                decisionResponse.setPriority(decision.getPriority());
                decisionResponse.setManagerNotes(decision.getManagerNotes());
                decisionResponse.setFinalDecision(decision.getFinalDecision());
                decisionResponse.setResolvedBy(decision.getResolvedBy());
                decisionResponse.setResolvedAt(decision.getResolvedAt());
                decisionResponse.setCreatedAt(decision.getCreatedAt());

                enhancedResponse.setDecisionDetails(decisionResponse);
            } else {
                enhancedResponse.setPriority("MEDIUM");
                enhancedResponse.setDecisionReason("No policy decision available");
            }

            enhancedResponse.setProcessingStatus(processingStatus);

            return enhancedResponse;

        } catch (Exception e) {
            log.warn("Error enhancing response with decision info, returning original response");
            EnhancedScoringResponse fallback = new EnhancedScoringResponse();
            fallback.setId(response.getId());
            fallback.setCompanyName(response.getCompanyName());
            fallback.setInn(response.getInn());
            fallback.setScore(response.getScore());
            fallback.setRiskLevel(response.getRiskLevel());
            fallback.setCreatedAt(response.getCreatedAt());
            fallback.setProcessingStatus("MANUAL_REVIEW");
            fallback.setPriority("MEDIUM");
            fallback.setDecisionReason("Error processing policy information");
            return fallback;
        }
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