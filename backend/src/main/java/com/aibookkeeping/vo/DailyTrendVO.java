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
public class DailyTrendVO {

    /** 日期 (yyyy-MM-dd) */
    private String date;

    /** 当日收入 */
    private BigDecimal income;

    /** 当日支出 */
    private BigDecimal expense;
}
