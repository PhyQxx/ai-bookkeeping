package com.aibookkeeping.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notification")
public class Notification {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 消息标题 */
    private String title;

    /** 消息内容 */
    private String content;

    /** 消息类型: SYSTEM, BUDGET_WARNING */
    private String type;

    /** 是否已读: 0=未读, 1=已读 */
    private Integer isRead;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;
}
