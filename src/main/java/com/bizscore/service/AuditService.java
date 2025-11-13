package com.bizscore.service;

import com.bizscore.entity.AuditLog;
import com.bizscore.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

/**
 * Сервис для асинхронного аудита всех операций в системе
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Async("auditExecutor")
    public void logOperation(String requestId, String operation, String requestData,
                             String responseData, String status, Long processingTimeMs,
                             HttpServletRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = "anonymous";
            String userRole = "ROLE_ANONYMOUS";

            if (authentication != null && authentication.isAuthenticated()) {
                username = authentication.getName();
                userRole = authentication.getAuthorities().stream()
                        .findFirst()
                        .map(Object::toString)
                        .orElse("ROLE_USER");
            }

            AuditLog auditLog = new AuditLog();
            auditLog.setRequestId(requestId);
            auditLog.setOperation(operation);
            auditLog.setUsername(username);
            auditLog.setUserRole(userRole);
            auditLog.setRequestData(truncateData(requestData));
            auditLog.setResponseData(truncateData(responseData));
            auditLog.setStatus(status);
            auditLog.setProcessingTimeMs(processingTimeMs);
            auditLog.setTimestamp(LocalDateTime.now());

            if (request != null) {
                auditLog.setClientIp(getClientIp(request));
                auditLog.setUserAgent(request.getHeader("User-Agent"));
            }

            auditLogRepository.save(auditLog);
            log.debug("Аудит операция сохранена: {}", operation);

        } catch (Exception e) {
            log.error("Ошибка сохранения аудит-лога: {}", e.getMessage());
        }
    }

    private String truncateData(String data) {
        if (data == null) return null;
        return data.length() > 4000 ? data.substring(0, 4000) : data;
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0];
        }
        return request.getRemoteAddr();
    }
}