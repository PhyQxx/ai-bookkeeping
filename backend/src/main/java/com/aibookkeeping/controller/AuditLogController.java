package com.aibookkeeping.controller;

import com.aibookkeeping.entity.AuditLog;
import com.aibookkeeping.exception.Result;
import com.aibookkeeping.service.audit.AuditLogService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "审计日志")
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping("/my")
    @Operation(summary = "获取我的操作日志")
    public Result<Page<AuditLog>> listMyLogs(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(auditLogService.listMyLogs(userId, pageNum, pageSize));
    }
}
