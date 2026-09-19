package com.campus.trade.security;

import com.campus.trade.common.BusinessException;
import com.campus.trade.common.ResultCode;
import com.campus.trade.entity.User;
import com.campus.trade.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录拦截器：
 * 1) 白名单接口允许匿名访问，但带了合法 token 时也会解析出登录用户（例如商品详情要返回是否已收藏）；
 * 2) 其他 /api/** 接口必须带合法 token，否则 401；
 * 3) token 合法但账号已被管理员禁用时返回 403（40302）。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    public AuthInterceptor(JwtUtil jwtUtil, UserMapper userMapper) {
        this.jwtUtil = jwtUtil;
        this.userMapper = userMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        String path = request.getRequestURI();
        String method = request.getMethod();
        boolean publicApi = isPublicApi(method, path);

        String token = resolveToken(request);
        LoginUser loginUser = null;
        if (token != null) {
            try {
                loginUser = jwtUtil.parse(token);
            } catch (Exception e) {
                if (!publicApi) {
                    throw new BusinessException(ResultCode.UNAUTHORIZED, "登录状态无效或已过期，请重新登录");
                }
            }
        }

        if (loginUser == null) {
            if (!publicApi) {
                throw new BusinessException(ResultCode.UNAUTHORIZED);
            }
            return true;
        }

        User user = userMapper.selectById(loginUser.userId());
        if (user == null) {
            if (!publicApi) {
                throw new BusinessException(ResultCode.UNAUTHORIZED, "账号不存在，请重新登录");
            }
            return true;
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }

        // 以数据库中的最新角色为准，管理员调整角色后立即生效
        UserContext.set(new LoginUser(user.getId(), user.getUsername(), user.getRole()));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7).trim();
            return token.isEmpty() ? null : token;
        }
        return null;
    }

    /** 匿名可访问的接口白名单 */
    private boolean isPublicApi(String method, String path) {
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }
        if ("POST".equalsIgnoreCase(method)) {
            return "/api/auth/register".equals(path) || "/api/auth/login".equals(path);
        }
        if ("GET".equalsIgnoreCase(method)) {
            if ("/api/products".equals(path) || "/api/categories".equals(path)) {
                return true;
            }
            // /api/products/{id}：匿名可看商品详情（/api/products/mine 例外，它需要登录）
            if (path.startsWith("/api/products/")) {
                String segment = path.substring("/api/products/".length());
                if (!segment.isEmpty() && segment.indexOf('/') < 0) {
                    return !"mine".equals(segment);
                }
                return false;
            }
            // /api/users/{id} 与 /api/users/{id}/products：匿名可查看用户主页
            if (path.startsWith("/api/users/")) {
                String rest = path.substring("/api/users/".length());
                if (rest.isEmpty()) {
                    return false;
                }
                int slash = rest.indexOf('/');
                return slash < 0 || "/products".equals(rest.substring(slash));
            }
        }
        return false;
    }
}
