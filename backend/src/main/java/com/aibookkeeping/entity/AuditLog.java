package com.aibookkeeping.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("audit_log")
public class AuditLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String username;
    private String action;
    private String module;
    private String detail;
    private String ip;
    private String method;
    private String path;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
