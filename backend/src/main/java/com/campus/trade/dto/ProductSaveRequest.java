package com.campus.trade.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "发布/编辑商品请求")
public record ProductSaveRequest(

        @Schema(description = "商品标题，1-50 字", example = "九成新《软件测试技术》教材")
        @NotBlank(message = "商品标题不能为空")
        @Size(min = 1, max = 50, message = "商品标题长度必须为 1-50 字")
        String title,

        @Schema(description = "商品描述，最多 1000 字", example = "只翻过几页，笔记很少，无破损。")
        @Size(max = 1000, message = "商品描述不能超过 1000 字")
        String description,

        @Schema(description = "售价，0.01-999999.99", example = "25.00")
        @NotNull(message = "价格不能为空")
        @DecimalMin(value = "0.01", message = "价格不能低于 0.01 元")
        @DecimalMax(value = "999999.99", message = "价格不能超过 999999.99 元")
        BigDecimal price,

        @Schema(description = "原价，可不填", example = "59.00")
        @DecimalMin(value = "0.01", message = "原价不能低于 0.01 元")
        @DecimalMax(value = "999999.99", message = "原价不能超过 999999.99 元")
        BigDecimal originalPrice,

        @Schema(description = "分类 ID", example = "1")
        @NotNull(message = "商品分类不能为空")
        Long categoryId,

        @Schema(description = "成色：全新 / 九成新 / 八成新 / 七成新及以下", example = "九成新")
        @NotBlank(message = "成色不能为空")
        @Pattern(regexp = "^(全新|九成新|八成新|七成新及以下)$", message = "成色只能为：全新、九成新、八成新、七成新及以下")
        String conditionLevel,

        @Schema(description = "交易地点", example = "三号宿舍楼下")
        @Size(max = 50, message = "交易地点不能超过 50 字")
        String tradePlace,

        @Schema(description = "商品图片地址列表，最多 5 张（先调上传接口拿到地址）")
        @Size(max = 5, message = "商品图片最多 5 张")
        List<String> imageUrls
) {
}
