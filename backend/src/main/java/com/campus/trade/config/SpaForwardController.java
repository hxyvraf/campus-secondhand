package com.campus.trade.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnResource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 前端打包进后端（resources/static/index.html 存在）时，把前端路由交给单页应用处理。
 * 未打包前端时该控制器不会生效，不影响前后端分离调试。
 */
@Controller
@ConditionalOnResource(resources = "classpath:/static/index.html")
public class SpaForwardController {

    @GetMapping({"/", "/login", "/register", "/products", "/products/**", "/publish", "/my-products",
            "/favorites", "/orders", "/orders/**", "/messages", "/profile", "/admin", "/admin/**"})
    public String forward() {
        return "forward:/index.html";
    }
}
