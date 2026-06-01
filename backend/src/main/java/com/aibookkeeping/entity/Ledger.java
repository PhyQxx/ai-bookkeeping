package com.aibookkeeping.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ledger")
public class Ledger {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 账本名称 */
    private String name;

    /** 账本说明 */
    private String description;

    /** 是否默认账本: 1=是, 0=否 */
    private Integer isDefault;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
