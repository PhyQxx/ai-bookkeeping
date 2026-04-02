package com.aibookkeeping.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserRequest {

    @Size(max = 50, message = "昵称不能超过50个字符")
    private String nickname;

    private String avatar;
}
