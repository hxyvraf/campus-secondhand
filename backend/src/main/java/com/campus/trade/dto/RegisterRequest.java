package com.campus.trade.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "注册请求")
public record RegisterRequest(

        @Schema(description = "登录账号，4-20 位字母/数字/下划线", example = "testuser01")
        @NotBlank(message = "用户名不能为空")
        @Pattern(regexp = "^[a-zA-Z0-9_]{4,20}$", message = "用户名必须为 4-20 位字母、数字或下划线")
        String username,

        @Schema(description = "登录密码，6-20 位", example = "123456")
        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 20, message = "密码长度必须为 6-20 位")
        String password,

        @Schema(description = "昵称，1-20 位", example = "小明")
        @NotBlank(message = "昵称不能为空")
        @Size(min = 1, max = 20, message = "昵称长度必须为 1-20 位")
        String nickname,

        @Schema(description = "手机号，可不填", example = "13800138000")
        @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String phone,

        @Schema(description = "邮箱，可不填", example = "test@example.com")
        @Email(message = "邮箱格式不正确")
        String email,

        @Schema(description = "学校，可不填", example = "南昌职业大学")
        @Size(max = 50, message = "学校名称不能超过 50 位")
        String school
) {
}
