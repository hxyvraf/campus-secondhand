package com.campus.trade.constant;

/** 商品状态：在售 / 已锁定（有进行中的订单）/ 已售出 / 已下架 */
public enum ProductStatus {

    ON_SALE("在售"),
    LOCKED("交易中"),
    SOLD("已售出"),
    OFF_SHELF("已下架");

    private final String label;

    ProductStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static boolean isValid(String value) {
        for (ProductStatus status : values()) {
            if (status.name().equals(value)) {
                return true;
            }
        }
        return false;
    }
}
