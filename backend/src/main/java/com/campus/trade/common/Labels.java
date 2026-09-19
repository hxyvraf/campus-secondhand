package com.campus.trade.common;

import com.campus.trade.constant.OrderStatus;
import com.campus.trade.constant.ProductStatus;

/** 状态码 -> 中文名 的转换工具，避免各层重复写 try/catch */
public final class Labels {

    private Labels() {
    }

    public static String product(String status) {
        try {
            return ProductStatus.valueOf(status).getLabel();
        } catch (Exception e) {
            return status;
        }
    }

    public static String order(String status) {
        try {
            return OrderStatus.valueOf(status).getLabel();
        } catch (Exception e) {
            return status;
        }
    }
}
