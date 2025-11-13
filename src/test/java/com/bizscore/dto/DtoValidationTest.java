package com.bizscore.dto;

import com.bizscore.dto.request.BatchScoringRequest;
import com.bizscore.dto.request.CalculateScoreRequest;
import com.bizscore.util.TestDataGenerator;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты валидации DTO
 * Проверяет аннотации валидации на объектах передачи данных
 */
class DtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void calculateScoreRequest_WithValidData_ShouldPassValidation() {
        // Given
        CalculateScoreRequest request = TestDataGenerator.createValidCalculateScoreRequest();

        // When
        Set<ConstraintViolation<CalculateScoreRequest>> violations = validator.validate(request);

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    void calculateScoreRequest_WithInvalidData_ShouldFailValidation() {
        // Given
        CalculateScoreRequest request = TestDataGenerator.createInvalidCalculateScoreRequest();

        // When
        Set<ConstraintViolation<CalculateScoreRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("companyName")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("inn")));
    }

    @Test
    void batchScoringRequest_WithEmptyList_ShouldFailValidation() {
        // Given
        BatchScoringRequest request = new BatchScoringRequest();
        // Не устанавливаем список запросов

        // When
        Set<ConstraintViolation<BatchScoringRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
    }
}