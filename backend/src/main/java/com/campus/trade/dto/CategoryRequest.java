package com.campus.trade.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "新增/修改分类请求")
public record CategoryRequest(

        @Schema(description = "分类名称，最多 20 字", example = "考研资料")
        @NotBlank(message = "分类名称不能为空")
        @Size(max = 20, message = "分类名称不能超过 20 字")
        String name,

        @Schema(description = "排序值，越小越靠前", example = "1")
        @Min(value = 0, message = "排序值不能小于 0")
        @Max(value = 9999, message = "排序值不能大于 9999")
        Integer sort
) {
}
