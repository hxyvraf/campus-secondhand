package com.campus.trade.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "仅返回主键 ID 的统一响应")
public record IdResponse(

        @Schema(description = "新增记录的主键 ID", example = "21")
        Long id
) {
}
