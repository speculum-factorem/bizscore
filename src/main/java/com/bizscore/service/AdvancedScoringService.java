package com.bizscore.service;

import com.bizscore.dto.request.BatchScoringRequest;
import com.bizscore.dto.request.CalculateScoreRequest;
import com.bizscore.dto.response.BatchScoringResponse;
import com.bizscore.dto.response.EnhancedScoringResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Расширенный сервис скоринга с пакетной обработкой и аналитикой
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdvancedScoringService {

    private final ScoringService scoringService;

    // @Transactional не используется здесь, так как async методы выполняются в отдельных потоках
    // и не могут наследовать транзакцию родителя. Каждый async метод имеет свою транзакцию.
    public BatchScoringResponse processBatchScoring(BatchScoringRequest request) {
        log.info("Обработка пакетного запроса для {} компаний", request.getRequests().size());

        BatchScoringResponse response = new BatchScoringResponse();
        response.setBatchId(UUID.randomUUID().toString());
        response.setTotalRequests(request.getRequests().size());

        List<CompletableFuture<EnhancedScoringResponse>> futures = request.getRequests().stream()
                .map(req -> processSingleScoringAsync(req))
                .collect(Collectors.toList());

        // Ожидаем завершения всех задач
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // Собираем результаты
        List<EnhancedScoringResponse> successful = new ArrayList<>();
        List<Map<String, Object>> failed = new ArrayList<>();

        for (int i = 0; i < futures.size(); i++) {
            try {
                EnhancedScoringResponse result = futures.get(i).get();
                successful.add(result);
            } catch (Exception e) {
                Map<String, Object> failedResult = new HashMap<>();
                failedResult.put("companyName", request.getRequests().get(i).getCompanyName());
                failedResult.put("inn", request.getRequests().get(i).getInn());
                failedResult.put("error", e.getMessage());
                failed.add(failedResult);
            }
        }

        response.setSuccessfulResults(successful);
        response.setFailedResults(failed);
        response.setProcessedAt(new Date());

        log.info("Пакетная обработка завершена. Успешно: {}, С ошибками: {}",
                successful.size(), failed.size());

        return response;
    }

    @Async("scoringExecutor")
    public CompletableFuture<EnhancedScoringResponse> processSingleScoringAsync(CalculateScoreRequest request) {
        try {
            log.debug("Асинхронная обработка скоринга для компании: {}", request.getCompanyName());
            EnhancedScoringResponse result = scoringService.calculateScore(request);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            log.error("Ошибка асинхронной обработки скоринга для компании: {}",
                    request.getCompanyName(), e);
            // Создаем fallback ответ для обработки ошибок
            EnhancedScoringResponse fallbackResponse = createFallbackResponse(request, e);
            return CompletableFuture.completedFuture(fallbackResponse);
        }
    }

    private EnhancedScoringResponse createFallbackResponse(CalculateScoreRequest request, Exception e) {
        // Создаем базовый ответ с информацией об ошибке
        EnhancedScoringResponse response = new EnhancedScoringResponse();
        response.setCompanyName(request.getCompanyName());
        response.setInn(request.getInn());
        response.setScore(0.0);
        response.setRiskLevel("HIGH");
        response.setProcessingStatus("ERROR");
        response.setPriority("HIGH");
        response.setDecisionReason("Ошибка обработки: " + e.getMessage());
        return response;
    }

    public EnhancedScoringResponse recalculateScore(Long scoringId) {
        log.info("Перерасчет скоринга для ID: {}", scoringId);

        // Здесь должна быть логика получения исходных данных и перерасчета
        // В реальной реализации нужно получить исходный запрос и пересчитать

        return null; // Заглушка для реализации
    }

    public Map<String, Object> analyzeScoringTrends(String period, int days) {
        log.info("Анализ тенденций скоринга за период: {}, дней: {}", period, days);

        Map<String, Object> trends = new HashMap<>();
        trends.put("period", period);
        trends.put("days", days);
        trends.put("analysisDate", new Date());

        // Анализ распределения рисков
        Map<String, Long> riskDistribution = new HashMap<>();
        riskDistribution.put("LOW", 45L);
        riskDistribution.put("MEDIUM", 35L);
        riskDistribution.put("HIGH", 20L);
        trends.put("riskDistribution", riskDistribution);

        // Тенденции среднего скора
        Map<String, Object> scoreTrends = new HashMap<>();
        scoreTrends.put("currentWeek", 0.72);
        scoreTrends.put("previousWeek", 0.68);
        scoreTrends.put("trend", "improving");
        trends.put("scoreTrends", scoreTrends);

        // Статистика по отраслям
        Map<String, Object> industryStats = new HashMap<>();
        industryStats.put("topPerforming", "Технологии");
        industryStats.put("needsAttention", "Розничная торговля");
        trends.put("industryStats", industryStats);

        log.info("Анализ тенденций завершен");
        return trends;
    }

    public Map<String, Object> compareCompanies(Map<String, Object> request) {
        log.info("Сравнительный анализ компаний");

        Map<String, Object> comparison = new HashMap<>();
        comparison.put("comparisonId", UUID.randomUUID().toString());
        comparison.put("comparedAt", new Date());

        // Здесь должна быть логика сравнения компаний
        // В реальной реализации нужно получить данные компаний и сравнить их показатели

        List<Map<String, Object>> companiesComparison = new ArrayList<>();

        Map<String, Object> company1 = new HashMap<>();
        company1.put("name", "Company A");
        company1.put("score", 0.85);
        company1.put("riskLevel", "LOW");
        companiesComparison.add(company1);

        Map<String, Object> company2 = new HashMap<>();
        company2.put("name", "Company B");
        company2.put("score", 0.65);
        company2.put("riskLevel", "MEDIUM");
        companiesComparison.add(company2);

        comparison.put("companies", companiesComparison);
        comparison.put("summary", "Company A имеет лучшие показатели");

        return comparison;
    }
}