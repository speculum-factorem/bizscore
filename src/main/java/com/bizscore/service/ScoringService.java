package com.bizscore.service;

import com.bizscore.client.MlServiceClient;
import com.bizscore.dto.request.CalculateScoreRequest;
import com.bizscore.dto.response.EnhancedScoringResponse;
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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScoringService implements ScoringServiceInterface {

    private final ScoringRepository repository;
    private final ScoringMapper mapper;
    private final MetricsService metricsService;
    private final PolicyEngineServiceInterface policyEngineService;
    private final ScoringDecisionRepository scoringDecisionRepository;
    private final MlServiceClient mlServiceClient;
    private final ScoringProcessor scoringProcessor;
    private final ScoringResponseEnricher responseEnricher;

    @Transactional
    @CacheEvict(value = {"scoringResults", "companyScores", "scoringStats"}, 
                allEntries = false,
                key = "#request.companyName + '_' + #request.inn")
    public EnhancedScoringResponse calculateScore(CalculateScoreRequest request) {
        metricsService.incrementScoringRequests();
        Timer.Sample timer = metricsService.startScoringTimer();

        setupMDC(request.getCompanyName(), request.getInn());

        try {
            // Преобразуем DTO в сущность и сохраняем в базу данных
            log.info("Преобразование запроса в сущность и сохранение в базу данных");
            ScoringRequest entity = mapper.toEntity(request);
            ScoringRequest savedEntity = repository.save(entity);
            MDC.put("scoringRequestId", String.valueOf(savedEntity.getId()));

            // Шаг 1: Применяем политики риска ДО вызова ML сервиса
            log.info("Начало оценки политик риска для запроса ID: {}", savedEntity.getId());
            ScoringDecision decision = policyEngineService.evaluatePolicies(savedEntity);
            log.info("Оценка политик завершена. Решение: {}, Приоритет: {}", decision.getDecision(), decision.getPriority());
            MDC.put("policyDecision", decision.getDecision());
            MDC.put("policyPriority", decision.getPriority() != null ? decision.getPriority() : "MEDIUM");

            // Шаг 2: ВСЕГДА вызываем ML сервис для расчета скоринга
            log.info("Вызов ML сервиса для расчета скоринга");
            Optional<Map<String, Object>> mlResponse = mlServiceClient.calculateScore(savedEntity);

            if (mlResponse.isPresent() && scoringProcessor.processMlResponse(savedEntity, mlResponse.get())) {
                log.info("Ответ ML сервиса успешно обработан. Скоринг: {}, Уровень риска: {}", 
                        savedEntity.getScore(), savedEntity.getRiskLevel());
                MDC.put("mlServiceUsed", "true");
            } else {
                log.warn("Использован fallback скоринг из-за недоступности или некорректного ответа ML сервиса");
                scoringProcessor.applyFallbackScoring(savedEntity);
                MDC.put("mlServiceUsed", "false");
                MDC.put("fallbackUsed", "true");
            }

            // Шаг 3: Сохраняем финальный результат в базу данных
            log.debug("Сохранение финального результата скоринга в базу данных");
            ScoringRequest resultEntity = repository.save(savedEntity);
            ScoringResponse basicResponse = mapper.toResponse(resultEntity);

            // Шаг 4: Обогащаем ответ информацией о решении политик
            log.debug("Обогащение ответа информацией о решении политик");
            EnhancedScoringResponse response = responseEnricher.enrich(basicResponse, decision);

            metricsService.incrementScoringSuccess();
            if (response.getScore() != null) {
                metricsService.recordScoreValue(response.getScore());
            }
            metricsService.stopScoringTimer(timer, response.getRiskLevel());

            return response;

        } catch (Exception e) {
            // Обработка ошибок: используем fallback скоринг
            metricsService.incrementScoringFailure();
            log.error("Ошибка при расчете скоринга, используется fallback. Компания: {}, ИНН: {}", 
                    request.getCompanyName(), request.getInn(), e);
            MDC.put("error", "true");
            MDC.put("errorMessage", e.getMessage());
            
            ScoringRequest fallbackEntity = mapper.toEntity(request);
            scoringProcessor.applyFallbackScoring(fallbackEntity);
            ScoringRequest resultEntity = repository.save(fallbackEntity);
            ScoringResponse basicResponse = mapper.toResponse(resultEntity);

            // Создаем базовое решение для fallback случая
            ScoringDecision fallbackDecision = createFallbackDecision(resultEntity.getId());
            scoringDecisionRepository.save(fallbackDecision);
            MDC.put("fallbackUsed", "true");

            return responseEnricher.enrich(basicResponse, fallbackDecision);
        } finally {
            cleanupMDC();
        }
    }

    // Получение результата скоринга по ID с использованием кэша
    @Cacheable(value = "scoringResults", key = "'id_' + #id")
    public ScoringResponse getById(Long id) {
        MDC.put("scoringRequestId", String.valueOf(id));
        log.info("Получение результата скоринга по ID: {} из базы данных", id);
        ScoringResponse response = repository.findById(id)
                .map(mapper::toResponse)
                .orElse(null);
        if (response == null) {
            log.warn("Результат скоринга не найден для ID: {}", id);
        }
        return response;
    }

    // Получение расширенного результата скоринга с информацией о решении политик
    public EnhancedScoringResponse getEnhancedScore(Long id) {
        MDC.put("scoringRequestId", String.valueOf(id));
        log.info("Получение расширенного результата скоринга для ID: {}", id);
        ScoringResponse basicResponse = getById(id);
        if (basicResponse == null) {
            log.warn("Базовый результат скоринга не найден для ID: {}", id);
            return null;
        }

        ScoringDecision decision = scoringDecisionRepository.findByScoringRequestId(id)
                .orElse(null);
        
        if (decision != null) {
            MDC.put("policyDecision", decision.getDecision());
            log.debug("Найдено решение политики для запроса ID: {}", id);
        } else {
            log.debug("Решение политики не найдено для запроса ID: {}", id);
        }

        return responseEnricher.enrich(basicResponse, decision);
    }

    // Получение результата скоринга по названию компании и ИНН с использованием кэша
    @Cacheable(value = "companyScores", key = "#companyName + '_' + #inn")
    public ScoringResponse getByCompanyAndInn(String companyName, String inn) {
        MDC.put("companyName", companyName);
        MDC.put("inn", inn);
        log.info("Получение результата скоринга для компании: {} с ИНН: {}", companyName, inn);
        ScoringResponse response = repository.findByCompanyNameAndInn(companyName, inn)
                .map(mapper::toResponse)
                .orElse(null);
        if (response == null) {
            log.warn("Результат скоринга не найден для компании: {} с ИНН: {}", companyName, inn);
        }
        return response;
    }

    // Получение всех результатов скоринга с пагинацией
    public Page<ScoringResponse> getAllScores(Pageable pageable) {
        log.info("Получение всех результатов скоринга с пагинацией: страница {}, размер {}", 
                pageable.getPageNumber(), pageable.getPageSize());
        MDC.put("page", String.valueOf(pageable.getPageNumber()));
        MDC.put("size", String.valueOf(pageable.getPageSize()));
        return repository.findAll(pageable)
                .map(mapper::toResponse);
    }

    // Получение результатов скоринга по уровню риска с пагинацией
    public Page<ScoringResponse> getScoresByRiskLevel(String riskLevel, Pageable pageable) {
        MDC.put("riskLevel", riskLevel);
        log.info("Получение результатов скоринга с уровнем риска: {} и пагинацией: страница {}, размер {}", 
                riskLevel, pageable.getPageNumber(), pageable.getPageSize());
        return repository.findByRiskLevel(riskLevel, pageable)
                .map(mapper::toResponse);
    }

    // Получение статистики скоринга с использованием кэша
    @Cacheable(value = "scoringStats", key = "'stats'")
    public Map<String, Object> getScoringStats() {
        log.info("Получение статистики скоринга");
        Map<String, Object> stats = new HashMap<>();
        
        // Оптимизированный запрос - получаем все данные за один раз
        Long totalRequests = repository.count();
        stats.put("totalRequests", totalRequests);
        log.debug("Общее количество запросов: {}", totalRequests);
        
        Double averageScore = repository.findAverageScore();
        stats.put("averageScore", averageScore);
        log.debug("Средний скоринг: {}", averageScore);
        
        // Получаем статистику по уровням риска одним запросом для оптимизации
        List<Object[]> riskLevelCounts = repository.countByRiskLevelGrouped();
        Map<String, Long> riskCounts = new HashMap<>();
        riskCounts.put("LOW", 0L);
        riskCounts.put("MEDIUM", 0L);
        riskCounts.put("HIGH", 0L);
        
        for (Object[] result : riskLevelCounts) {
            String riskLevel = (String) result[0];
            Long count = (Long) result[1];
            riskCounts.put(riskLevel, count);
            log.debug("Уровень риска {}: {} запросов", riskLevel, count);
        }
        
        stats.put("lowRiskCount", riskCounts.get("LOW"));
        stats.put("mediumRiskCount", riskCounts.get("MEDIUM"));
        stats.put("highRiskCount", riskCounts.get("HIGH"));
        
        log.info("Статистика скоринга успешно получена");
        return stats;
    }

    // Создание базового решения для fallback случая
    private ScoringDecision createFallbackDecision(Long scoringRequestId) {
        log.debug("Создание fallback решения для запроса ID: {}", scoringRequestId);
        ScoringDecision fallbackDecision = new ScoringDecision();
        fallbackDecision.setScoringRequestId(scoringRequestId);
        fallbackDecision.setDecision("MANUAL_REVIEW");
        fallbackDecision.setReason("Использован fallback скоринг из-за ошибки");
        fallbackDecision.setAppliedPolicy("SYSTEM_FALLBACK");
        fallbackDecision.setPriority("MEDIUM");
        fallbackDecision.setFinalDecision("PENDING");
        return fallbackDecision;
    }

    // Настройка MDC для логирования контекста запроса
    private void setupMDC(String companyName, String inn) {
        if (companyName != null) MDC.put("companyName", companyName);
        if (inn != null) MDC.put("inn", inn);
    }

    // Очистка MDC после обработки запроса
    private void cleanupMDC() {
        MDC.remove("companyName");
        MDC.remove("inn");
        MDC.remove("scoringRequestId");
        MDC.remove("policyDecision");
        MDC.remove("policyPriority");
        MDC.remove("mlServiceUsed");
        MDC.remove("fallbackUsed");
        MDC.remove("error");
        MDC.remove("errorMessage");
        MDC.remove("riskLevel");
        MDC.remove("page");
        MDC.remove("size");
    }
}