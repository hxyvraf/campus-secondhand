package com.campus.trade.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 商品列表/详情返回对象 */
@Data
@Schema(description = "商品信息")
public class ProductVO {

    private Long id;
    private String title;
    private String description;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Long categoryId;
    private String categoryName;
    private String conditionLevel;
    private String tradePlace;

    @Schema(description = "商品状态：ON_SALE 在售 / LOCKED 交易中 / SOLD 已售出 / OFF_SHELF 已下架")
    private String status;

    @Schema(description = "商品状态中文名", example = "在售")
    private String statusLabel;

    private Integer viewCount;
    private Integer favoriteCount;

    @Schema(description = "封面图地址")
    private String coverImage;

    private Long sellerId;
    private String sellerNickname;
    private String sellerAvatar;

    private LocalDateTime createdAt;

    @Schema(description = "当前登录用户是否已收藏，未登录或不适用时为 null")
    private Boolean favorited;

    @Schema(description = "商品图片列表，仅详情接口返回")
    private List<String> images;
}
