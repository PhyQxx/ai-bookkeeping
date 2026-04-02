package com.aibookkeeping.controller;

import com.aibookkeeping.dto.UpdatePasswordRequest;
import com.aibookkeeping.dto.UpdateUserRequest;
import com.aibookkeeping.exception.Result;
import com.aibookkeeping.service.user.UserService;
import com.aibookkeeping.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@Tag(name = "用户管理", description = "个人信息管理")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/info")
    @Operation(summary = "获取当前用户信息")
    public Result<UserVO> getUserInfo(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        UserVO vo = userService.getUserInfo(userId);
        return Result.success(vo);
    }

    @PutMapping("/info")
    @Operation(summary = "更新用户信息")
    public Result<UserVO> updateUserInfo(@Valid @RequestBody UpdateUserRequest request,
                                          Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        UserVO vo = userService.updateUserInfo(userId, request);
        return Result.success(vo);
    }

    @PutMapping("/password")
    @Operation(summary = "修改密码")
    public Result<Void> updatePassword(@Valid @RequestBody UpdatePasswordRequest request,
                                        Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        userService.updatePassword(userId, request);
        return Result.success();
    }
}
