package com.example.seedbe.global.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;

public class CookieUtil {

    private static final String PROD_DOMAIN = "seedlab.cloud";

    private static ResponseCookie generateCookie(String name, String value, int maxAge, boolean isSecure, String path) {
        // 확장성을 위한 서브도메인 공유 정책
        String domain = isSecure ? PROD_DOMAIN : null;

        return ResponseCookie.from(name, value)
                .path(path)
                .domain(domain)
                .httpOnly(true)
                .maxAge(maxAge)
                .secure(isSecure)
                .sameSite("Lax")
                .build();
    }

    public static void addCookie(HttpServletResponse response, String name, String value, int maxAge, boolean isSecure, String path) {
        ResponseCookie cookie = generateCookie(name, value, maxAge, isSecure,  path);
        response.addHeader("Set-Cookie", cookie.toString());
    }

    public static void deleteCookie(HttpServletResponse response, String name, boolean isSecure, String path) {
        ResponseCookie deleteCookie = generateCookie(name, "", 0, isSecure, path);
        response.addHeader("Set-Cookie", deleteCookie.toString());
    }
}
