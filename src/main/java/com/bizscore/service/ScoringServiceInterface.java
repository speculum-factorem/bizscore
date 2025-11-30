package com.bizscore.service;

import com.bizscore.dto.request.CalculateScoreRequest;
import com.bizscore.dto.response.EnhancedScoringResponse;
import com.bizscore.dto.response.ScoringResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;

/**
 * Интерфейс сервиса скоринга
 * Определяет контракт для работы с скорингом компаний
 */
public interface ScoringServiceInterface {
    
    /**
     * Рассчитывает скоринговый балл для компании
     * 
     * @param request запрос на расчет скоринга
     * @return расширенный ответ со скоринговым баллом и решением политик
     */
    EnhancedScoringResponse calculateScore(CalculateScoreRequest request);
    
    /**
     * Получает результат скоринга по ID
     * 
     * @param id идентификатор результата скоринга
     * @return базовый ответ скоринга
     */
    ScoringResponse getById(Long id);
    
    /**
     * Получает расширенный результат скоринга по ID
     * 
     * @param id идентификатор результата скоринга
     * @return расширенный ответ со скоринговым баллом и решением политик
     */
    EnhancedScoringResponse getEnhancedScore(Long id);
    
    /**
     * Получает результат скоринга по названию компании и ИНН
     * 
     * @param companyName название компании
     * @param inn ИНН компании
     * @return базовый ответ скоринга
     */
    ScoringResponse getByCompanyAndInn(String companyName, String inn);
    
    /**
     * Получает все результаты скоринга с пагинацией
     * 
     * @param pageable параметры пагинации
     * @return страница результатов скоринга
     */
    Page<ScoringResponse> getAllScores(Pageable pageable);
    
    /**
     * Получает результаты скоринга по уровню риска
     * 
     * @param riskLevel уровень риска
     * @param pageable параметры пагинации
     * @return страница результатов скоринга
     */
    Page<ScoringResponse> getScoresByRiskLevel(String riskLevel, Pageable pageable);
    
    /**
     * Получает статистику по скорингу
     * 
     * @return статистика скоринга
     */
    Map<String, Object> getScoringStats();
}

