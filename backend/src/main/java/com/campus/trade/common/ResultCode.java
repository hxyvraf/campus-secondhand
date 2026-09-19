package com.campus.trade.common;

import org.springframework.http.HttpStatus;

/**
 * 业务返回码与对应 HTTP 状态码。
 *
 * <p>约定：HTTP 状态码表示协议层结果，code 表示业务层结果，两者同时返回，便于接口测试做双重断言。</p>
 */
public enum ResultCode {

    SUCCESS(200, "操作成功", HttpStatus.OK),

    PARAM_ERROR(40001, "参数校验失败", HttpStatus.BAD_REQUEST),
    FILE_TYPE_NOT_SUPPORTED(40002, "文件类型不支持，仅允许上传 jpg/jpeg/png", HttpStatus.BAD_REQUEST),
    FILE_SIZE_EXCEEDED(40003, "文件大小超过限制", HttpStatus.BAD_REQUEST),

    UNAUTHORIZED(40101, "未登录或登录已过期，请重新登录", HttpStatus.UNAUTHORIZED),
    LOGIN_FAILED(40102, "用户名或密码错误", HttpStatus.UNAUTHORIZED),

    FORBIDDEN(40301, "无权限操作该资源", HttpStatus.FORBIDDEN),
    USER_DISABLED(40302, "账号已被禁用，请联系管理员", HttpStatus.FORBIDDEN),
    NEED_ADMIN(40303, "需要管理员权限", HttpStatus.FORBIDDEN),

    NOT_FOUND(40401, "资源不存在", HttpStatus.NOT_FOUND),
    METHOD_NOT_ALLOWED(40500, "请求方法不支持", HttpStatus.METHOD_NOT_ALLOWED),
    MEDIA_TYPE_NOT_SUPPORTED(41500, "请求 Content-Type 不支持，请使用 application/json", HttpStatus.UNSUPPORTED_MEDIA_TYPE),

    DUPLICATE_ORDER(40901, "你已对该商品下过订单，请勿重复提交", HttpStatus.CONFLICT),
    ORDER_STATUS_ERROR(40902, "当前状态不允许该操作", HttpStatus.CONFLICT),
    PRODUCT_NOT_TRADABLE(40903, "商品当前不可交易", HttpStatus.CONFLICT),
    USERNAME_EXISTS(40904, "用户名已存在", HttpStatus.CONFLICT),

    SERVER_ERROR(50000, "服务器内部错误", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    ResultCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
