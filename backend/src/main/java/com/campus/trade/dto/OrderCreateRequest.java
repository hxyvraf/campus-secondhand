package com.campus.trade.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "下单请求")
public record OrderCreateRequest(

        @Schema(description = "商品 ID", example = "1")
        @NotNull(message = "商品 ID 不能为空")
        Long productId,

        @Schema(description = "买家留言，最多 200 字", example = "今晚 7 点在三号宿舍楼下交易可以吗？")
        @Size(max = 200, message = "买家留言不能超过 200 字")
        String remark
) {
}
