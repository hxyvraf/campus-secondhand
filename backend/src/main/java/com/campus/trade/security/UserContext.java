package com.campus.trade.security;

import com.campus.trade.common.BusinessException;
import com.campus.trade.common.ResultCode;

/** 当前请求的登录用户上下文（ThreadLocal，请求结束后由拦截器清理） */
public final class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    /** 取当前登录用户 ID，未登录直接抛 401 */
    public static Long userId() {
        LoginUser user = HOLDER.get();
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return user.userId();
    }

    public static String role() {
        LoginUser user = HOLDER.get();
        return user == null ? null : user.role();
    }

    public static boolean isAdmin() {
        LoginUser user = HOLDER.get();
        return user != null && user.isAdmin();
    }

    /** 管理员接口统一入口校验，非管理员抛 403 */
    public static void requireAdmin() {
        LoginUser user = HOLDER.get();
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        if (!user.isAdmin()) {
            throw new BusinessException(ResultCode.NEED_ADMIN);
        }
    }
}
