package com.campus.trade.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "取消订单请求")
public record OrderCancelRequest(

        @Schema(description = "取消原因，最多 200 字", example = "临时有事，暂时不买了")
        @Size(max = 200, message = "取消原因不能超过 200 字")
        String reason
) {
}
