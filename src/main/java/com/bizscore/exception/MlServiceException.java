package com.bizscore.exception;

/**
 * Исключение для ошибок взаимодействия с ML сервисом
 */
public class MlServiceException extends RuntimeException {
    
    public MlServiceException(String message) {
        super(message);
    }
    
    public MlServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}

