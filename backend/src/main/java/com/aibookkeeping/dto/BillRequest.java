package com.aibookkeeping.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class BillRequest {

    @NotNull(message = "金额不能为空")
    private BigDecimal amount;

    @NotNull(message = "类型不能为空")
    private Integer type;

    private Long categoryId;

    private LocalDate billDate;

    private String remark;
}
