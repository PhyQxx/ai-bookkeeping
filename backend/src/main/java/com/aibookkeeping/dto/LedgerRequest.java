package com.aibookkeeping.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LedgerRequest {
    @NotBlank(message = "账本名称不能为空")
    private String name;
    private String description;
}
