package com.bizscore.controller;

import com.bizscore.dto.request.CalculateScoreRequest;
import com.bizscore.dto.response.ScoringResponse;
import com.bizscore.service.ScoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ScoringController {

    private final ScoringService scoringService;

    @PostMapping("/score")
    public ResponseEntity<ScoringResponse> calculateScore(@RequestBody CalculateScoreRequest request) {
        String requestId = java.util.UUID.randomUUID().toString().substring(0, 8);
        MDC.put("requestId", requestId);
        MDC.put("companyName", request.getCompanyName());
        MDC.put("inn", request.getInn());
        MDC.put("operation", "calculate_score");

        try {
            ScoringResponse result = scoringService.calculateScore(request);
            log.info("Score calculation completed - Score: {}, Risk Level: {}",
                    result.getScore(), result.getRiskLevel());
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Score calculation failed for company: {}", request.getCompanyName(), e);
            throw e;
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/score/{id}")
    public ResponseEntity<ScoringResponse> getScore(@PathVariable Long id) {
        MDC.put("requestId", java.util.UUID.randomUUID().toString().substring(0, 8));
        MDC.put("operation", "get_score");

        try {
            ScoringResponse result = scoringService.getById(id);
            if (result == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Failed to retrieve score for ID: {}", id, e);
            throw e;
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/health")
    public String health() {
        return "BizScore Service is working!";
    }
}