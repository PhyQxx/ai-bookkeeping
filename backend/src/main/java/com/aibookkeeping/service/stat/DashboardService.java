package com.aibookkeeping.service.stat;

import com.aibookkeeping.vo.BudgetProgressVO;
import com.aibookkeeping.vo.CategoryRankingVO;
import com.aibookkeeping.vo.DailyTrendVO;
import com.aibookkeeping.vo.MonthlyStatVO;

import java.util.List;

public interface DashboardService {
    MonthlyStatVO getMonthlySummary(Long userId);
    List<DailyTrendVO> getRecentTrend(Long userId);
    List<CategoryRankingVO> getCategoryRanking(Long userId);
    List<BudgetProgressVO> getBudgetProgress(Long userId);
}
