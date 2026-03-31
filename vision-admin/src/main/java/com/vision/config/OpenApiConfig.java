package com.vision.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 配置
 * 生成 API 文档
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Vision Analysis Platform API")
                        .version("1.0.0")
                        .description("视觉分析平台 RESTful API 文档")
                        .contact(new Contact()
                                .name("Vision Team")
                                .email("vision@example.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(List.of(
                        new Server().url("http://localhost:26330").description("本地开发环境"),
                        new Server().url("/api/v1").description("生产环境")));
    }
}
