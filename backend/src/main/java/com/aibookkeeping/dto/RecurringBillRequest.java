package com.aibookkeeping.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class RecurringBillRequest {

    @NotNull(message = "金额不能为空")
    @DecimalMin(value = "0.01", message = "金额必须大于 0")
    private BigDecimal amount;

    @NotNull(message = "类型不能为空")
    private Integer type;

    @NotNull(message = "分类不能为空")
    private Long categoryId;

    private String remark;

    @NotBlank(message = "周期频率不能为空")
    private String frequency;

    private Integer dayOfPeriod;

    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;

    private LocalDate endDate;

    private Integer status;
}
