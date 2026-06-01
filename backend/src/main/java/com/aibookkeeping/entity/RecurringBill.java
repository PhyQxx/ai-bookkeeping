package com.aibookkeeping.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("recurring_bill")
public class RecurringBill {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long ledgerId;

    private BigDecimal amount;

    /** 1=收入, 2=支出 */
    private Integer type;

    private Long categoryId;

    private String remark;

    /** 周期频率: DAILY, WEEKLY, MONTHLY */
    private String frequency;

    /** 对于每周是 1-7 (周一到周日), 对于每月是 1-31 (几号) */
    private Integer dayOfPeriod;

    private LocalDate startDate;

    private LocalDate endDate;

    private LocalDate lastGeneratedDate;

    /** 1=启用, 0=暂停 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
