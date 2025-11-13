package com.bizscore.aspect;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.util.Arrays;
import java.util.UUID;

/**
 * Аспект для логирования выполнения методов
 * Обеспечивает сквозное логирование всех операций
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class LoggingAspect {

    private static final String[] SENSITIVE_FIELDS = {"password", "token", "secret", "authorization"};

    @Around("execution(* com.bizscore.controller..*(..)) || execution(* com.bizscore.service..*(..))")
    public Object logMethodExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getMethod().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();

        String requestId = MDC.get("requestId");
        if (requestId == null) {
            requestId = UUID.randomUUID().toString().substring(0, 8);
            MDC.put("requestId", requestId);
        }

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        try {
            log.info("Начало выполнения метода: {}.{} с параметрами: {}",
                    className, methodName, maskSensitiveData(joinPoint.getArgs()));

            Object result = joinPoint.proceed();

            stopWatch.stop();
            long executionTime = stopWatch.getTotalTimeMillis();

            log.info("Успешное выполнение метода: {}.{} за {} мс",
                    className, methodName, executionTime);

            MDC.put("duration", String.valueOf(executionTime));
            return result;

        } catch (Exception e) {
            stopWatch.stop();
            log.error("Ошибка выполнения метода: {}.{} за {} мс. Ошибка: {}",
                    className, methodName, stopWatch.getTotalTimeMillis(), e.getMessage(), e);
            throw e;
        }
    }

    private Object maskSensitiveData(Object[] args) {
        if (args == null) return "null";

        return Arrays.stream(args)
                .map(arg -> {
                    if (arg instanceof String) {
                        String str = (String) arg;
                        for (String sensitive : SENSITIVE_FIELDS) {
                            if (str.toLowerCase().contains(sensitive)) {
                                return "***MASKED***";
                            }
                        }
                    }
                    return arg;
                })
                .toArray();
    }
}