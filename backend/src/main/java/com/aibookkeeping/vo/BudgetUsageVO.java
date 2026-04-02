package com.aibookkeeping.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetUsageVO {

    private Long budgetId;

    private Long categoryId;

    private String categoryName;

    private BigDecimal budgetAmount;

    private BigDecimal usedAmount;

    private BigDecimal remainingAmount;

    /**
     * 使用百分比 0-100+
     */
    private BigDecimal usagePercent;

    /**
     * 是否超支
     */
    private Boolean overBudget;
}
