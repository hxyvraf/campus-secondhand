package com.campus.trade.security;

/** 当前登录用户（由 JWT 解析得到） */
public record LoginUser(Long userId, String username, String role) {

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}
