package com.campus.trade.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/** 站内消息 */
@Data
@TableName("message")
public class Message {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 消息接收人 */
    private Long userId;

    /** ORDER / FAVORITE / SYSTEM */
    private String type;

    private String title;

    private String content;

    /** 关联业务 ID：订单消息=订单ID，收藏消息=商品ID */
    private Long relatedId;

    /** 0 未读，1 已读 */
    private Integer isRead;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
