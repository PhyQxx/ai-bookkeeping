package com.aibookkeeping.service.auth;

import com.aibookkeeping.dto.LoginRequest;
import com.aibookkeeping.dto.RegisterRequest;
import com.aibookkeeping.vo.LoginVO;

public interface AuthService {

    void register(RegisterRequest request);

    LoginVO login(LoginRequest request);

    void logout(String token);
}
