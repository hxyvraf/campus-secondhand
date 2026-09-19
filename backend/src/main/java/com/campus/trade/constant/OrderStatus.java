package com.campus.trade.constant;

/** 订单状态机：待卖家确认 -> 已确认(交易中) -> 已完成；待确认/已确认可取消，待确认可超时关闭 */
public enum OrderStatus {

    PENDING_CONFIRM("待卖家确认"),
    CONFIRMED("交易中"),
    COMPLETED("已完成"),
    CANCELED("已取消"),
    TIMEOUT("超时关闭");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** 未结束的订单状态（用于重复下单校验） */
    public static boolean isActive(String status) {
        return PENDING_CONFIRM.name().equals(status) || CONFIRMED.name().equals(status);
    }
}
