package com.aibookkeeping.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("bill")
public class Bill {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long ledgerId;

    private BigDecimal amount;

    /** 1=收入, 2=支出 */
    private Integer type;

    private Long categoryId;

    private LocalDate billDate;

    private String remark;

    /** 1=AI输入, 2=手动输入 */
    private Integer inputType;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
