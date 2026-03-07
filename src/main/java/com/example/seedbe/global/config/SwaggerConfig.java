package com.example.seedbe.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        Server localServer = new Server().url("http://localhost:8080").description("Local Development Server (HTTP)");
        Server prodServer = new Server().url("https://api.seedlab.cloud").description("Production Server (HTTPS)");

        SecurityScheme accessTokenAuth = new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.COOKIE)
                .name("accessToken");

        SecurityRequirement securityRequirement = new SecurityRequirement().addList("AccessToken");

        return new OpenAPI()
                .servers(List.of(localServer, prodServer))
                .components(new Components().addSecuritySchemes("AccessToken", accessTokenAuth))
                .addSecurityItem(securityRequirement)
                .info(new Info()
                        .title("SEED API 명세서")
                        .description("SEED 프로젝트 API 명세서입니다. 상단 Servers에서 환경을 선택하세요.")
                        .version("v1.0.0"));
    }
}