package com.campus.trade.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "启用/禁用用户请求")
public record UserStatusRequest(

        @Schema(description = "1 启用，0 禁用", example = "0")
        @NotNull(message = "状态不能为空")
        @Min(value = 0, message = "状态只能为 0 或 1")
        @Max(value = 1, message = "状态只能为 0 或 1")
        Integer status
) {
}
