package com.campus.trade.constant;

/** 站内消息类型 */
public enum MessageType {

    ORDER("订单消息"),
    FAVORITE("收藏消息"),
    SYSTEM("系统消息");

    private final String label;

    MessageType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
