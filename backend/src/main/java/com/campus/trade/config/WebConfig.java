package com.campus.trade.config;

import com.campus.trade.security.AuthInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Web 配置：拦截器、上传目录静态映射、跨域 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebConfig.class);

    private final AuthInterceptor authInterceptor;
    private final String uploadDir;

    public WebConfig(AuthInterceptor authInterceptor, @Value("${app.upload.dir:uploads}") String uploadDir) {
        this.authInterceptor = authInterceptor;
        this.uploadDir = uploadDir;
        try {
            Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            log.info("图片上传目录：{}", dir);
        } catch (IOException e) {
            log.warn("创建上传目录失败：{}", e.getMessage());
        }
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor).addPathPatterns("/api/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 用户上传的商品图片（保存在运行目录下的 uploads 文件夹）
        String location = Paths.get(uploadDir).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/uploads/**").addResourceLocations(location);
        // 初始数据用的示例图片（随代码一起提交，放在 resources/seed-images 下）
        registry.addResourceHandler("/seed/**").addResourceLocations("classpath:/seed-images/");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 前端开发服务器（Vite）默认是 5173，端口被占用时 Vite 会自动 +1，这里放行本机任意端口
        registry.addMapping("/**")
                .allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
