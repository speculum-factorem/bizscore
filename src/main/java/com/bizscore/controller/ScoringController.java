package com.bizscore.controller;

import com.bizscore.dto.request.CalculateScoreRequest;
import com.bizscore.dto.response.ScoringResponse;
import com.bizscore.service.ScoringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Scoring API", description = "API для скоринга бизнеса")
@SecurityRequirement(name = "bearerAuth")
public class ScoringController {

    private final ScoringService scoringService;

    @Operation(summary = "Рассчитать скоринговый балл", description = "Рассчитывает скоринговый балл для компании на основе предоставленных данных")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешный расчет",
                    content = @Content(schema = @Schema(implementation = ScoringResponse.class))),
            @ApiResponse(responseCode = "400", description = "Неверные входные данные"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @PostMapping("/score")
    public ResponseEntity<ScoringResponse> calculateScore(
            @Parameter(description = "Данные для расчета скоринга")
            @Valid @RequestBody CalculateScoreRequest request) {

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

    @Operation(summary = "Получить результат скоринга по ID", description = "Возвращает результат скоринга по идентификатору")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Результат найден"),
            @ApiResponse(responseCode = "404", description = "Результат не найден")
    })
    @GetMapping("/score/{id}")
    public ResponseEntity<ScoringResponse> getScore(
            @Parameter(description = "ID результата скоринга")
            @PathVariable Long id) {

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

    @Operation(summary = "Получить все результаты скоринга", description = "Возвращает все результаты скоринга с пагинацией")
    @GetMapping("/scores")
    public ResponseEntity<Page<ScoringResponse>> getAllScores(
            @Parameter(description = "Номер страницы (0-based)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Размер страницы")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Поле для сортировки")
            @RequestParam(defaultValue = "createdAt") String sortBy,

            @Parameter(description = "Направление сортировки")
            @RequestParam(defaultValue = "DESC") String direction) {

        String requestId = generateRequestId();
        setupMDC(requestId, null, null, "get_all_scores");

        try {
            Sort.Direction sortDirection = Sort.Direction.fromString(direction);
            Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

            log.info("Retrieving all scores with pagination: page={}, size={}", page, size);
            Page<ScoringResponse> scores = scoringService.getAllScores(pageable);

            return ResponseEntity.ok(scores);

        } catch (Exception e) {
            log.error("Failed to retrieve scores", e);
            return ResponseEntity.internalServerError().build();
        } finally {
            MDC.clear();
        }
    }

    @Operation(summary = "Получить результаты по уровню риска", description = "Возвращает результаты скоринга по уровню риска с пагинацией")
    @GetMapping("/scores/risk/{riskLevel}")
    public ResponseEntity<Page<ScoringResponse>> getScoresByRiskLevel(
            @Parameter(description = "Уровень риска (LOW, MEDIUM, HIGH)")
            @PathVariable String riskLevel,

            @Parameter(description = "Номер страницы (0-based)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Размер страницы")
            @RequestParam(defaultValue = "20") int size) {

        String requestId = generateRequestId();
        setupMDC(requestId, null, null, "get_scores_by_risk");

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

            log.info("Retrieving scores with risk level: {}", riskLevel);
            Page<ScoringResponse> scores = scoringService.getScoresByRiskLevel(riskLevel, pageable);

            return ResponseEntity.ok(scores);

        } catch (Exception e) {
            log.error("Failed to retrieve scores by risk level: {}", riskLevel, e);
            return ResponseEntity.internalServerError().build();
        } finally {
            MDC.clear();
        }
    }

    @Operation(summary = "Получить статистику скоринга", description = "Возвращает общую статистику по всем скоринговым запросам")
    @GetMapping("/scores/stats")
    public ResponseEntity<Map<String, Object>> getScoringStats() {
        String requestId = generateRequestId();
        setupMDC(requestId, null, null, "get_scoring_stats");

        try {
            log.info("Retrieving scoring statistics");
            Map<String, Object> stats = scoringService.getScoringStats();
            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            log.error("Failed to retrieve scoring statistics", e);
            return ResponseEntity.internalServerError().build();
        } finally {
            MDC.clear();
        }
    }

    @Operation(summary = "Проверка здоровья сервиса")
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("BizScore Service is working!");
    }

    @Operation(summary = "Метрики приложения", description = "Возвращает метрики приложения в формате Prometheus")
    @GetMapping("/metrics")
    public ResponseEntity<String> metrics() {
        return ResponseEntity.ok("Metrics endpoint is available at /actuator/prometheus");
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