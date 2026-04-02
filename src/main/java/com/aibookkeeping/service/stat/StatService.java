package com.aibookkeeping.service.stat;

import com.aibookkeeping.vo.CategoryRatioVO;
import com.aibookkeeping.vo.MonthlyStatVO;
import com.aibookkeeping.vo.TrendVO;

import java.util.List;

public interface StatService {

    /**
     * 月度收支总览
     */
    MonthlyStatVO getMonthlyStat(Long userId, String month);

    /**
     * 分类占比
     */
    List<CategoryRatioVO> getCategoryRatio(Long userId, String month, Integer type);

    /**
     * 消费趋势（近N个月）
     */
    List<TrendVO> getTrend(Long userId, Integer months);
}
