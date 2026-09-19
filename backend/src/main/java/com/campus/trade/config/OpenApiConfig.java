package com.campus.trade.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger / OpenAPI 文档配置。
 *
 * <p>swagger-ui： http://localhost:8080/swagger-ui.html ，openapi 原始 JSON： http://localhost:8080/v3/api-docs</p>
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "校园二手交易平台 API",
                version = "1.0.0",
                description = "B/S 架构校园二手交易平台后端接口，包含注册登录、商品发布与管理、搜索收藏、订单管理、消息通知 5 个模块及管理端接口。"
                        + "除白名单接口外均需在请求头携带 Authorization: Bearer {token}。"
        ),
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}
