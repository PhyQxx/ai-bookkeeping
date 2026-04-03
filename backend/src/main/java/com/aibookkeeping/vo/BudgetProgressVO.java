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
public class BudgetProgressVO {
    private Long budgetId;
    private String categoryName;
    private BigDecimal budgetAmount;
    private BigDecimal usedAmount;
    private BigDecimal remainingAmount;
    private BigDecimal usagePercent;
    private Boolean overBudget;
}
