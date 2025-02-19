package com.sky.service;

import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;

public interface UserService {
    /**
     * line 登入
     * @return
     */
    User lineLogin(UserLoginDTO userLoginDTO);
}
