package com.aibookkeeping.service.stat;

import com.aibookkeeping.vo.CategoryRatioVO;
import com.aibookkeeping.vo.DailyTrendVO;
import com.aibookkeeping.vo.MonthlyStatVO;
import com.aibookkeeping.vo.TrendVO;

import java.util.List;

public interface StatService {

    MonthlyStatVO getMonthlyStat(Long userId, String month);

    List<CategoryRatioVO> getCategoryRatio(Long userId, String month, Integer type);

    List<TrendVO> getTrend(Long userId, String month, Integer months);

    List<DailyTrendVO> getDailyTrend(Long userId, String month);
}
