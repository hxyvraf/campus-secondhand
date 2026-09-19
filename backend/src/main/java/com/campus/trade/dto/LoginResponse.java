package com.campus.trade.dto;

import com.campus.trade.vo.UserVO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "登录响应")
public record LoginResponse(

        @Schema(description = "JWT 令牌，后续请求放在请求头 Authorization: Bearer {token}")
        String token,

        @Schema(description = "令牌类型", example = "Bearer")
        String tokenType,

        @Schema(description = "有效期（秒）", example = "7200")
        long expiresIn,

        @Schema(description = "登录用户信息")
        UserVO user
) {
}
