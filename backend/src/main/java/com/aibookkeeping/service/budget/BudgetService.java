package com.aibookkeeping.service.budget;

import com.aibookkeeping.dto.BudgetRequest;
import com.aibookkeeping.vo.BudgetUsageVO;
import com.aibookkeeping.vo.BudgetVO;

import java.util.List;

public interface BudgetService {

    List<BudgetVO> listBudgets(Long userId, String month);

    BudgetVO createOrUpdateBudget(BudgetRequest request, Long userId);

    void deleteBudget(Long id, Long userId);

    List<BudgetUsageVO> getBudgetUsage(Long userId, String month);
}
