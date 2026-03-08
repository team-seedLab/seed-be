package com.example.seedbe.global.security.oauth2;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "oauth2")
public record OAuth2Properties(
        String redirectUri
) {
}