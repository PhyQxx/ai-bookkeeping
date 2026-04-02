package com.aibookkeeping.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiParseRequest {

    @NotBlank(message = "输入内容不能为空")
    private String input;
}
