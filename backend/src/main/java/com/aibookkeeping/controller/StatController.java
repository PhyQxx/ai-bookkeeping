package com.aibookkeeping.controller;

import com.aibookkeeping.exception.Result;
import com.aibookkeeping.service.stat.StatService;
import com.aibookkeeping.vo.CategoryRatioVO;
import com.aibookkeeping.vo.MonthlyStatVO;
import com.aibookkeeping.vo.TrendVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/stat")
@Tag(name = "统计分析", description = "月度总览、分类占比、消费趋势")
@RequiredArgsConstructor
public class StatController {

    private final StatService statService;

    @GetMapping("/monthly")
    @Operation(summary = "月度收支总览")
    public Result<MonthlyStatVO> getMonthlyStat(
            @RequestParam(defaultValue = "#{T(java.time.YearMonth).now().toString()}") String month,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        MonthlyStatVO vo = statService.getMonthlyStat(userId, month);
        return Result.success(vo);
    }

    @GetMapping("/category-ratio")
    @Operation(summary = "分类占比")
    public Result<List<CategoryRatioVO>> getCategoryRatio(
            @RequestParam(defaultValue = "#{T(java.time.YearMonth).now().toString()}") String month,
            @RequestParam(required = false) Integer type,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        List<CategoryRatioVO> list = statService.getCategoryRatio(userId, month, type);
        return Result.success(list);
    }

    @GetMapping("/trend")
    @Operation(summary = "消费趋势（近N个月）")
    public Result<List<TrendVO>> getTrend(
            @RequestParam(defaultValue = "6") Integer months,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        List<TrendVO> list = statService.getTrend(userId, months);
        return Result.success(list);
    }
}
