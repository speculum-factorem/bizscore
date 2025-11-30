package com.bizscore.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * Валидатор для российского ИНН
 * ИНН может быть 10 или 12 цифр
 */
public class InnValidator implements ConstraintValidator<ValidInn, String> {

    private static final Pattern INN_PATTERN = Pattern.compile("^\\d{10}|\\d{12}$");

    @Override
    public void initialize(ValidInn constraintAnnotation) {
        // Инициализация не требуется
    }

    @Override
    public boolean isValid(String inn, ConstraintValidatorContext context) {
        if (inn == null || inn.isBlank()) {
            return false;
        }

        // Проверка формата (10 или 12 цифр)
        if (!INN_PATTERN.matcher(inn).matches()) {
            return false;
        }

        // Дополнительная проверка контрольной суммы для 10-значного ИНН
        if (inn.length() == 10) {
            return validateInn10(inn);
        }

        // Дополнительная проверка контрольной суммы для 12-значного ИНН
        if (inn.length() == 12) {
            return validateInn12(inn);
        }

        return false;
    }

    private boolean validateInn10(String inn) {
        int[] coefficients = {2, 4, 10, 3, 5, 9, 4, 6, 8};
        int sum = 0;
        for (int i = 0; i < 9; i++) {
            sum += Character.getNumericValue(inn.charAt(i)) * coefficients[i];
        }
        int checkDigit = sum % 11;
        if (checkDigit == 10) {
            checkDigit = 0;
        }
        return checkDigit == Character.getNumericValue(inn.charAt(9));
    }

    private boolean validateInn12(String inn) {
        // Проверка первой контрольной суммы
        int[] coefficients1 = {7, 2, 4, 10, 3, 5, 9, 4, 6, 8};
        int sum1 = 0;
        for (int i = 0; i < 10; i++) {
            sum1 += Character.getNumericValue(inn.charAt(i)) * coefficients1[i];
        }
        int checkDigit1 = sum1 % 11;
        if (checkDigit1 == 10) {
            checkDigit1 = 0;
        }
        if (checkDigit1 != Character.getNumericValue(inn.charAt(10))) {
            return false;
        }

        // Проверка второй контрольной суммы
        int[] coefficients2 = {3, 7, 2, 4, 10, 3, 5, 9, 4, 6, 8};
        int sum2 = 0;
        for (int i = 0; i < 11; i++) {
            sum2 += Character.getNumericValue(inn.charAt(i)) * coefficients2[i];
        }
        int checkDigit2 = sum2 % 11;
        if (checkDigit2 == 10) {
            checkDigit2 = 0;
        }
        return checkDigit2 == Character.getNumericValue(inn.charAt(11));
    }
}

