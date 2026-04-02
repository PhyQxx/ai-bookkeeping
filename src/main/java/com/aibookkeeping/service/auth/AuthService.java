package com.aibookkeeping.service.auth;

import com.aibookkeeping.dto.LoginRequest;
import com.aibookkeeping.dto.RegisterRequest;
import com.aibookkeeping.vo.LoginVO;

public interface AuthService {

    /**
     * 用户注册
     */
    void register(RegisterRequest request);

    /**
     * 用户登录
     */
    LoginVO login(LoginRequest request);

    /**
     * 用户登出（将 Token 加入黑名单）
     */
    void logout(String token);
}
