package com.bizscore.service;

import com.bizscore.entity.ScoringDecision;
import com.bizscore.entity.ScoringRequest;

/**
 * Интерфейс сервиса оценки политик
 * Определяет контракт для работы с политиками риска
 */
public interface PolicyEngineServiceInterface {
    
    /**
     * Оценивает политики риска для запроса на скоринг
     * 
     * @param scoringRequest запрос на скоринг
     * @return решение политики
     */
    ScoringDecision evaluatePolicies(ScoringRequest scoringRequest);
}

