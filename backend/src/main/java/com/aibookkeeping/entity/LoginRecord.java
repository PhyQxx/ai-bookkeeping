package com.aibookkeeping.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("login_record")
public class LoginRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String username;
    private String ip;
    private String location;
    private String userAgent;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime loginTime;
}
