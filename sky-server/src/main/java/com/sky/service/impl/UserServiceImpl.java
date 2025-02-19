package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.constant.MessageConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.exception.LoginFailedException;
import com.sky.mapper.UserMapper;
import com.sky.properties.LineProperties;
import com.sky.service.UserService;
import com.sky.utils.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    //line 服務接口地址
    public static final String line_LOGIN = "https://api.line.me/oauth2/v2.1/verify";
    public static final String line_UserProfile = "https://api.line.me/v2/profile";
    @Autowired
    private LineProperties lineProperties;
    @Autowired
    private UserMapper userMapper;



    /**
     * Line 登入
     * @param userLoginDTO
     * @return
     */
    public User lineLogin(UserLoginDTO userLoginDTO){
        //調用line接口服務 獲得當前用戶的openid
        String accessToken = userLoginDTO.getAccess_token();
        String openid = userLoginDTO.getUser_id();
        //判斷openid 是否為空 如果為空表示登入失敗 拋出業務異常
        if(openid == null){
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }
        //判斷當前用戶是否為新用戶
        User user = userMapper.getByOpenid(openid);
        //如果是新用戶 自動完成註冊
        if(user == null){
            user = User.builder()
                    .openid(openid)
                    .createTime(LocalDateTime.now())
                    .build();
            userMapper.insert(user);
        }
        //返回這個用戶對象
        return user;
    }


}
