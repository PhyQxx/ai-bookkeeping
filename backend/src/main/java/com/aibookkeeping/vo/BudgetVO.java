package com.aibookkeeping.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetVO {

    private Long id;

    private Long categoryId;

    private String categoryName;

    private BigDecimal amount;

    private String month;

    private LocalDateTime createdAt;
}
