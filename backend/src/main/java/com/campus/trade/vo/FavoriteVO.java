package com.campus.trade.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 我的收藏列表项 */
@Data
@Schema(description = "收藏记录")
public class FavoriteVO {

    private Long productId;
    private String title;
    private BigDecimal price;
    private String coverImage;
    private String status;
    private String statusLabel;
    private String sellerNickname;
    private LocalDateTime favoritedAt;
}
