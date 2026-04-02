package com.aibookkeeping.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BudgetRequest {

    /**
     * 分类ID，null 表示月度总预算
     */
    private Long categoryId;

    @NotNull(message = "预算金额不能为空")
    @DecimalMin(value = "0.01", message = "预算金额必须大于0")
    private BigDecimal amount;

    @NotBlank(message = "预算月份不能为空")
    private String month;
}
