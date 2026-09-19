package com.campus.trade.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 订单列表/详情返回对象 */
@Data
@Schema(description = "订单信息")
public class OrderVO {

    private Long id;
    private String orderNo;
    private Long productId;
    private String productTitle;
    private String coverImage;
    private String tradePlace;
    private BigDecimal amount;

    @Schema(description = "订单状态：PENDING_CONFIRM 待卖家确认 / CONFIRMED 交易中 / COMPLETED 已完成 / CANCELED 已取消 / TIMEOUT 超时关闭")
    private String status;

    @Schema(description = "订单状态中文名", example = "待卖家确认")
    private String statusLabel;

    private Long buyerId;
    private String buyerNickname;
    private Long sellerId;
    private String sellerNickname;
    private String remark;
    private String cancelReason;

    @Schema(description = "卖家确认超时时间点，超过该时间未确认订单自动关闭")
    private LocalDateTime expireAt;

    private LocalDateTime createdAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime canceledAt;
}
