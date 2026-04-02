package com.aibookkeeping.controller;

import com.aibookkeeping.exception.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@Tag(name = "系统", description = "健康检查")
public class HealthController {

    @GetMapping("/actuator/health")
    @Operation(summary = "健康检查", description = "服务健康状态检测")
    public Result<Map<String, Object>> health() {
        return Result.success(Map.of(
                "status", "UP",
                "timestamp", LocalDateTime.now().toString(),
                "service", "ai-bookkeeping"
        ));
    }
}
