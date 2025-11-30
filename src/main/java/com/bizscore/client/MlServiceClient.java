package com.bizscore.client;

import com.bizscore.entity.ScoringRequest;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.client.RestClientException;

import java.util.Map;
import java.util.Optional;

/**
 * Клиент для интеграции с ML сервисом
 * Абстрагирует детали взаимодействия с внешним ML сервисом
 */
public interface MlServiceClient {
    
    /**
     * Вызывает ML сервис для расчета скоринга
     * 
     * @param scoringRequest запрос на скоринг
     * @return ответ от ML сервиса или пустой Optional в случае ошибки
     */
    @Retryable(
            retryFor = {RestClientException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    Optional<Map<String, Object>> calculateScore(ScoringRequest scoringRequest);
}

