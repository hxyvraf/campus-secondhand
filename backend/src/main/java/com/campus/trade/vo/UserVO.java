package com.campus.trade.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** 用户公开资料（不含密码、手机号等敏感信息；本人查看时返回完整资料） */
@Data
@Schema(description = "用户信息")
public class UserVO {

    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String school;

    @Schema(description = "仅本人或管理员可见")
    private String phone;

    @Schema(description = "仅本人或管理员可见")
    private String email;

    private String role;
    private Integer status;
    private String createdAt;

    @Schema(description = "在售商品数量")
    private Long onSaleCount;
}
