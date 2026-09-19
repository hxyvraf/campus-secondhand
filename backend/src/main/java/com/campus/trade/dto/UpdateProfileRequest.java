package com.campus.trade.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "更新个人资料请求")
public record UpdateProfileRequest(

        @Schema(description = "昵称", example = "小明")
        @Size(min = 1, max = 20, message = "昵称长度必须为 1-20 位")
        String nickname,

        @Schema(description = "手机号", example = "13800138000")
        @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String phone,

        @Schema(description = "邮箱", example = "test@example.com")
        @Email(message = "邮箱格式不正确")
        String email,

        @Schema(description = "头像地址", example = "/uploads/20260919/avatar.jpg")
        @Size(max = 255, message = "头像地址过长")
        String avatar,

        @Schema(description = "学校", example = "南昌职业大学")
        @Size(max = 50, message = "学校名称不能超过 50 位")
        String school
) {
}
