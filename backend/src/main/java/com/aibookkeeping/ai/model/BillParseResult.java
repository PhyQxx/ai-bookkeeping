package com.aibookkeeping.ai.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * AI 解析记账输入的结果模型
 * 用于 Spring AI BeanOutputConverter 结构化输出
 */
@Data
public class BillParseResult {

    /** 金额（正数） */
    private BigDecimal amount;

    /** 分类名称 */
    private String category;

    /** 账单日期 */
    private LocalDate date;

    /** 备注 */
    private String remark;
}
