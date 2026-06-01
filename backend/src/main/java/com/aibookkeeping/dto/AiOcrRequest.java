package com.aibookkeeping.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiOcrRequest {
    /** 图片 Base64 数据 (含 data:image/xxx;base64, 前缀) */
    @NotBlank(message = "图片数据不能为空")
    private String imageBase64;
}
