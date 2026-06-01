package com.aibookkeeping.entity;

import com.baomidou.mybatisplus.annotation.*;
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
@TableName("budget")
public class Budget {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long ledgerId;

    /**
     * NULL = 月度总预算, 非NULL = 分类预算
     */
    private Long categoryId;

    private BigDecimal amount;

    /**
     * 预算月份，如 2026-04
     */
    private String month;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
