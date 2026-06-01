package com.aibookkeeping.aspect;

import com.aibookkeeping.entity.AuditLog;
import com.aibookkeeping.mapper.AuditLogMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;

    @Around("@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PutMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.DeleteMapping)")
    public Object logWriteOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long elapsed = System.currentTimeMillis() - start;

        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setCreatedAt(LocalDateTime.now());

            // Extract request info
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                auditLog.setIp(getClientIp(request));
                auditLog.setMethod(request.getMethod());
                auditLog.setPath(request.getRequestURI());
            }

            // Extract user info
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof Long) {
                auditLog.setUserId((Long) auth.getPrincipal());
                auditLog.setUsername(auth.getName());
            }

            // Determine action and module from controller class/method
            String className = joinPoint.getTarget().getClass().getSimpleName();
            String methodName = joinPoint.getSignature().getName();
            auditLog.setModule(className.replace("Controller", ""));
            auditLog.setAction(methodName);

            // Serialize args (skip Authentication)
            Object[] args = joinPoint.getArgs();
            StringBuilder detail = new StringBuilder();
            for (Object arg : args) {
                if (arg != null && !arg.getClass().getName().contains("Authentication")) {
                    try {
                        detail.append(objectMapper.writeValueAsString(arg)).append("; ");
                    } catch (Exception e) {
                        detail.append(arg.toString()).append("; ");
                    }
                }
            }
            auditLog.setDetail(detail.length() > 2000 ? detail.substring(0, 2000) : detail.toString());

            // Async save to avoid blocking response
            auditLogMapper.insert(auditLog);
        } catch (Exception e) {
            log.warn("Failed to save audit log", e);
        }

        return result;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip != null && ip.contains(",") ? ip.split(",")[0].trim() : ip;
    }
}
