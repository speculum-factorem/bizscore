package com.bizscore.controller;

import com.bizscore.dto.request.CalculateScoreRequest;
import com.bizscore.dto.response.ScoringResponse;
import com.bizscore.service.ScoringService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ScoringController {

    private final ScoringService scoringService;

    @PostMapping("/score")
    public ResponseEntity<ScoringResponse> calculateScore(@Valid @RequestBody CalculateScoreRequest request) {
        String requestId = generateRequestId();
        setupMDC(requestId, request.getCompanyName(), request.getInn(), "calculate_score");

        try {
            log.info("Starting score calculation for company: {}", request.getCompanyName());
            ScoringResponse result = scoringService.calculateScore(request);
            log.info("Score calculation completed - Score: {}, Risk Level: {}",
                    result.getScore(), result.getRiskLevel());
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Score calculation failed for company: {}", request.getCompanyName(), e);
            return ResponseEntity.internalServerError().build();
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/score/{id}")
    public ResponseEntity<ScoringResponse> getScore(@PathVariable Long id) {
        String requestId = generateRequestId();
        setupMDC(requestId, null, null, "get_score");

        try {
            log.info("Retrieving score for ID: {}", id);
            ScoringResponse result = scoringService.getById(id);

            if (result == null) {
                log.warn("Score not found for ID: {}", id);
                return ResponseEntity.notFound().build();
            }

            log.info("Score retrieved successfully for ID: {}", id);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Failed to retrieve score for ID: {}", id, e);
            return ResponseEntity.internalServerError().build();
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("BizScore Service is working!");
    }

    private String generateRequestId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private void setupMDC(String requestId, String companyName, String inn, String operation) {
        MDC.put("requestId", requestId);
        MDC.put("operation", operation);

        if (companyName != null) {
            MDC.put("companyName", companyName);
        }
        if (inn != null) {
            MDC.put("inn", inn);
        }
    }
}