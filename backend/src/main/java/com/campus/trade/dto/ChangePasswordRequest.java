package com.campus.trade.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "修改密码请求")
public record ChangePasswordRequest(

        @Schema(description = "原密码", example = "123456")
        @NotBlank(message = "原密码不能为空")
        String oldPassword,

        @Schema(description = "新密码，6-20 位", example = "abc123456")
        @NotBlank(message = "新密码不能为空")
        @Size(min = 6, max = 20, message = "新密码长度必须为 6-20 位")
        String newPassword
) {
}
