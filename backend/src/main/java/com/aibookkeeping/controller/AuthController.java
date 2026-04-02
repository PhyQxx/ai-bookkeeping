package com.aibookkeeping.controller;

import com.aibookkeeping.dto.LoginRequest;
import com.aibookkeeping.dto.RefreshTokenRequest;
import com.aibookkeeping.dto.RegisterRequest;
import com.aibookkeeping.exception.Result;
import com.aibookkeeping.service.auth.AuthService;
import com.aibookkeeping.vo.LoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "认证管理", description = "用户注册、登录、登出、Token刷新")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return Result.success();
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        LoginVO vo = authService.login(request);
        return Result.success(vo);
    }

    @PostMapping("/refresh")
    @Operation(summary = "刷新Token")
    public Result<LoginVO> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        LoginVO vo = authService.refreshToken(request.getRefreshToken());
        return Result.success(vo);
    }

    @PostMapping("/logout")
    @Operation(summary = "用户登出")
    public Result<Void> logout(HttpServletRequest request) {
        String token = extractToken(request);
        if (token != null) {
            authService.logout(token);
        }
        return Result.success();
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
