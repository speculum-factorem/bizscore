package com.bizscore.util;

import com.bizscore.dto.request.CalculateScoreRequest;
import com.bizscore.entity.ScoringRequest;

/**
 * Генератор тестовых данных
 * Создает объекты для использования в тестах
 */
public class TestDataGenerator {

    public static CalculateScoreRequest createValidCalculateScoreRequest() {
        CalculateScoreRequest request = new CalculateScoreRequest();
        request.setCompanyName("Тестовая компания");
        request.setInn("123456789012");
        request.setBusinessType("ООО");
        request.setYearsInBusiness(5);
        request.setAnnualRevenue(5000000.0);
        request.setEmployeeCount(50);
        request.setRequestedAmount(1000000.0);
        request.setHasExistingLoans(false);
        request.setIndustry("Технологии");
        request.setCreditHistory(3);
        return request;
    }

    public static ScoringRequest createScoringRequest() {
        ScoringRequest request = new ScoringRequest();
        request.setCompanyName("Тестовая компания");
        request.setInn("123456789012");
        request.setAnnualRevenue(5000000.0);
        request.setYearsInBusiness(5);
        request.setEmployeeCount(50);
        request.setRequestedAmount(1000000.0);
        request.setHasExistingLoans(false);
        request.setIndustry("Технологии");
        request.setCreditHistory(3);
        request.setScore(0.75);
        request.setRiskLevel("LOW");
        return request;
    }

    public static CalculateScoreRequest createInvalidCalculateScoreRequest() {
        CalculateScoreRequest request = new CalculateScoreRequest();
        // Намеренно не заполняем обязательные поля
        return request;
    }
}