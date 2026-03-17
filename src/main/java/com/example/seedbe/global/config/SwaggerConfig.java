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
        String socialLoginGuide = """
                        ## 📌 소셜 로그인 테스트 방법
                        Swagger에서 직접 실행(`Try it out`)은 OAuth2 리다이렉트 흐름 특성상 동작하지 않습니다.
                        대신 **브라우저 주소창**에서 아래 URL로 이동하세요:
                        
                        - 로컬 Kakao 로그인: [http://localhost:8080/oauth2/authorization/kakao](http://localhost:8080/oauth2/authorization/kakao)
                        - 배포 Kakao 로그인: [https://api.seedlab.cloud/oauth2/authorization/kakao](https://api.seedlab.cloud/oauth2/authorization/kakao)
                        - 로컬 Google 로그인: [http://localhost:8080/oauth2/authorization/google](http://localhost:8080/oauth2/authorization/google)
                        - 배포 Google 로그인: [https://api.seedlab.cloud/oauth2/authorization/google](https://api.seedlab.cloud/oauth2/authorization/google)
                        
                        ## 📌 스웨거 테스트시 유의점
                        바로 밑에 보이는 Servers에서 로컬환경이면 로컬주소를 선택해서, 배포환경이면 배포주소를 선택하고 테스트해주세요.
                        """;

        Server localServer = new Server().url("http://localhost:8080").description("Local Development Server (HTTP)");
        Server prodServer = new Server().url("https://api.seedlab.cloud").description("Production Server (HTTPS)");


        return new OpenAPI()
                .servers(List.of(localServer, prodServer))
                .info(new Info()
                        .title("SEED API 명세서")
                        .description(socialLoginGuide)
                        .version("v1.0.0"));
    }
}