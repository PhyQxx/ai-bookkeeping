package com.aibookkeeping.service.auth;

import com.aibookkeeping.dto.LoginRequest;
import com.aibookkeeping.dto.RegisterRequest;
import com.aibookkeeping.entity.User;
import com.aibookkeeping.exception.BusinessException;
import com.aibookkeeping.exception.ErrorCode;
import com.aibookkeeping.mapper.UserMapper;
import com.aibookkeeping.util.JwtUtil;
import com.aibookkeeping.vo.LoginVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void register(RegisterRequest request) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        if (userMapper.selectOne(wrapper) != null) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getUsername());
        userMapper.insert(user);

        log.info("User registered: {}", request.getUsername());
    }

    @Override
    public LoginVO login(LoginRequest request) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());

        // 存入 Redis，支持主动失效
        redisTemplate.opsForValue().set("token:user:" + user.getId(), token, 7, TimeUnit.DAYS);

        return LoginVO.builder()
                .token(token)
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .build();
    }

    @Override
    public void logout(String token) {
        try {
            Long userId = jwtUtil.getUserId(token);
            redisTemplate.opsForValue().set("token:blacklist:" + token, "1", 7, TimeUnit.DAYS);
            redisTemplate.delete("token:user:" + userId);
            log.info("User logged out: userId={}", userId);
        } catch (Exception e) {
            log.warn("Failed to logout: {}", e.getMessage());
        }
    }
}
