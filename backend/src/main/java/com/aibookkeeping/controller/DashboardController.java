package com.aibookkeeping.controller;

import com.aibookkeeping.service.stat.DashboardService;
import com.aibookkeeping.vo.BudgetProgressVO;
import com.aibookkeeping.vo.CategoryRankingVO;
import com.aibookkeeping.vo.DailyTrendVO;
import com.aibookkeeping.vo.MonthlyStatVO;
import com.aibookkeeping.exception.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "数据看板", description = "首页 Dashboard 数据")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    @Operation(summary = "本月收支汇总")
    public Result<MonthlyStatVO> getSummary(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(dashboardService.getMonthlySummary(userId));
    }

    @GetMapping("/trend")
    @Operation(summary = "近30天每日收支趋势")
    public Result<List<DailyTrendVO>> getTrend(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(dashboardService.getRecentTrend(userId));
    }

    @GetMapping("/category-ranking")
    @Operation(summary = "分类消费排行 Top5")
    public Result<List<CategoryRankingVO>> getCategoryRanking(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(dashboardService.getCategoryRanking(userId));
    }

    @GetMapping("/budget-progress")
    @Operation(summary = "预算使用进度")
    public Result<List<BudgetProgressVO>> getBudgetProgress(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(dashboardService.getBudgetProgress(userId));
    }
}
