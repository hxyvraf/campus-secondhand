package com.campus.trade.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 订单 */
@Data
@TableName("trade_order")
public class TradeOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long productId;

    private Long buyerId;

    private Long sellerId;

    private BigDecimal amount;

    /** PENDING_CONFIRM / CONFIRMED / COMPLETED / CANCELED / TIMEOUT */
    private String status;

    private String remark;

    private String cancelReason;

    /** 买家下单后卖家需在该时间前确认，否则自动关闭 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime confirmedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime finishedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime canceledAt;
}
