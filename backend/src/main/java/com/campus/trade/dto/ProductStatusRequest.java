package com.campus.trade.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "商品上下架请求")
public record ProductStatusRequest(

        @Schema(description = "目标状态：ON_SALE 上架 / OFF_SHELF 下架", example = "OFF_SHELF")
        @NotBlank(message = "目标状态不能为空")
        @Pattern(regexp = "^(ON_SALE|OFF_SHELF)$", message = "目标状态只能为 ON_SALE 或 OFF_SHELF")
        String status
) {
}
