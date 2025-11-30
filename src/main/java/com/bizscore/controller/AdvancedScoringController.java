package com.bizscore.controller;

import com.bizscore.dto.request.BatchScoringRequest;
import com.bizscore.dto.response.BatchScoringResponse;
import com.bizscore.dto.response.EnhancedScoringResponse;
import com.bizscore.service.AdvancedScoringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Расширенный контроллер скоринга с пакетной обработкой и дополнительными функциями
 */
@Slf4j
@RestController
@RequestMapping("/api/v2/scoring")
@RequiredArgsConstructor
@Tag(name = "Advanced Scoring API", description = "Расширенный API для скоринга бизнеса")
public class AdvancedScoringController {

    private final AdvancedScoringService scoringService;

    @Operation(summary = "Пакетный расчет скоринга", description = "Рассчитывает скоринговые баллы для нескольких компаний одновременно")
    @PostMapping("/batch")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<BatchScoringResponse> calculateBatchScore(
            @Valid @RequestBody BatchScoringRequest request) {

        log.info("Начало пакетного расчета скоринга для {} компаний", request.getRequests().size());

        try {
            BatchScoringResponse response = scoringService.processBatchScoring(request);
            log.info("Пакетный расчет завершен. Успешно: {}, С ошибками: {}",
                    response.getSuccessfulResults().size(), response.getFailedResults().size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Ошибка пакетного расчета скоринга", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Перерасчет скоринга", description = "Пересчитывает скоринговый балл для существующего запроса")
    @PostMapping("/{id}/recalculate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EnhancedScoringResponse> recalculateScore(@PathVariable Long id) {
        log.info("Перерасчет скоринга для ID: {}", id);

        try {
            EnhancedScoringResponse response = scoringService.recalculateScore(id);
            if (response == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Ошибка перерасчета скоринга для ID: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Анализ тенденций", description = "Возвращает анализ тенденций скоринга за период")
    @GetMapping("/analytics/trends")
    @PreAuthorize("hasRole('ANALYST') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getScoringTrends(
            @RequestParam String period,
            @RequestParam(defaultValue = "30") int days) {

        log.info("Запрос анализа тенденций за период: {}, дней: {}", period, days);

        try {
            Map<String, Object> trends = scoringService.analyzeScoringTrends(period, days);
            return ResponseEntity.ok(trends);

        } catch (Exception e) {
            log.error("Ошибка анализа тенденций", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Сравнительный анализ", description = "Сравнивает скоринговые показатели компаний")
    @PostMapping("/analytics/compare")
    public ResponseEntity<Map<String, Object>> compareCompanies(@RequestBody Map<String, Object> request) {
        log.info("Сравнительный анализ компаний");

        try {
            Map<String, Object> comparison = scoringService.compareCompanies(request);
            return ResponseEntity.ok(comparison);

        } catch (Exception e) {
            log.error("Ошибка сравнительного анализа", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}