package com.campus.trade.controller;

import com.campus.trade.common.Result;
import com.campus.trade.dto.ChangePasswordRequest;
import com.campus.trade.dto.LoginRequest;
import com.campus.trade.dto.LoginResponse;
import com.campus.trade.dto.RegisterRequest;
import com.campus.trade.security.UserContext;
import com.campus.trade.service.AuthService;
import com.campus.trade.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 注册登录模块 */
@Tag(name = "01-认证", description = "注册、登录、退出、当前用户、修改密码")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "用户注册", description = "用户名唯一；密码 6-20 位；用户名重复返回 409/40904")
    @PostMapping("/register")
    public Result<UserVO> register(@Valid @RequestBody RegisterRequest request) {
        return Result.ok("注册成功", authService.register(request));
    }

    @Operation(summary = "用户登录", description = "成功后返回 JWT 令牌，后续接口放在请求头 Authorization: Bearer {token}")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok("登录成功", authService.login(request));
    }

    @Operation(summary = "退出登录", description = "JWT 无状态，服务端不需要注销；前端删除本地令牌即可，该接口用于统一前端调用")
    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.ok("退出成功", null);
    }

    @Operation(summary = "获取当前登录用户信息")
    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.ok(authService.currentUser(UserContext.userId()));
    }

    @Operation(summary = "修改密码", description = "原密码不正确返回 400/40001；新密码与原密码相同也返回 400/40001")
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(UserContext.userId(), request);
        return Result.ok("密码修改成功，请重新登录", null);
    }
}
