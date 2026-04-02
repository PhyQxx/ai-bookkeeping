package com.aibookkeeping.service.user;

import com.aibookkeeping.dto.UpdatePasswordRequest;
import com.aibookkeeping.dto.UpdateUserRequest;
import com.aibookkeeping.entity.User;
import com.aibookkeeping.exception.BusinessException;
import com.aibookkeeping.exception.ErrorCode;
import com.aibookkeeping.mapper.UserMapper;
import com.aibookkeeping.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserVO getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return convertToVO(user);
    }

    @Override
    public UserVO updateUserInfo(Long userId, UpdateUserRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }
        userMapper.updateById(user);
        log.info("User info updated: userId={}", userId);

        return convertToVO(user);
    }

    @Override
    public void updatePassword(Long userId, UpdatePasswordRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.OLD_PASSWORD_INCORRECT);
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(user);
        log.info("Password updated: userId={}", userId);
    }

    private UserVO convertToVO(User user) {
        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
