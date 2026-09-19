package com.campus.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.trade.common.BusinessException;
import com.campus.trade.common.ResultCode;
import com.campus.trade.constant.UserRole;
import com.campus.trade.dto.ChangePasswordRequest;
import com.campus.trade.dto.LoginRequest;
import com.campus.trade.dto.LoginResponse;
import com.campus.trade.dto.RegisterRequest;
import com.campus.trade.entity.User;
import com.campus.trade.mapper.UserMapper;
import com.campus.trade.security.JwtUtil;
import com.campus.trade.vo.UserVO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 认证服务：注册、登录、当前用户、修改密码 */
@Service
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserService userService;

    public AuthService(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtUtil jwtUtil, UserService userService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.userService = userService;
    }

    @Transactional
    public UserVO register(RegisterRequest request) {
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.username()));
        if (exists != null && exists > 0) {
            throw new BusinessException(ResultCode.USERNAME_EXISTS);
        }
        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setNickname(request.nickname());
        user.setPhone(blankToNull(request.phone()));
        user.setEmail(blankToNull(request.email()));
        user.setSchool(blankToNull(request.school()));
        user.setRole(UserRole.USER);
        user.setStatus(1);
        userMapper.insert(user);
        return userService.toVO(user, true);
    }

    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.username()));
        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ResultCode.LOGIN_FAILED);
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }
        String token = jwtUtil.generate(user.getId(), user.getUsername(), user.getRole());
        return new LoginResponse(token, "Bearer", jwtUtil.getExpireSeconds(), userService.toVO(user, true));
    }

    public UserVO currentUser(Long userId) {
        return userService.getPublicProfile(userId, userId, true);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userService.getUserOrThrow(userId);
        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "原密码不正确");
        }
        if (request.oldPassword().equals(request.newPassword())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "新密码不能与原密码相同");
        }
        User update = new User();
        update.setId(userId);
        update.setPassword(passwordEncoder.encode(request.newPassword()));
        userMapper.updateById(update);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
