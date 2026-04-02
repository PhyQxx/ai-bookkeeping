package com.aibookkeeping.service.user;

import com.aibookkeeping.dto.UpdatePasswordRequest;
import com.aibookkeeping.dto.UpdateUserRequest;
import com.aibookkeeping.vo.UserVO;

public interface UserService {

    UserVO getUserInfo(Long userId);

    UserVO updateUserInfo(Long userId, UpdateUserRequest request);

    void updatePassword(Long userId, UpdatePasswordRequest request);
}
