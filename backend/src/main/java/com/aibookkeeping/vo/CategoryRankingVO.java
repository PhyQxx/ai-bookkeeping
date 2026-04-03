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
public class CategoryRankingVO {
    private Long categoryId;
    private String categoryName;
    private BigDecimal amount;
    private Integer count;
    private BigDecimal percentage;
}
