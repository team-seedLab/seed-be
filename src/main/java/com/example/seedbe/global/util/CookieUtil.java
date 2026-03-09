package com.example.seedbe.global.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;

public class CookieUtil {

    private static final String PROD_DOMAIN = "seedlab.cloud";

    private static ResponseCookie generateCookie(String name, String value, int maxAge, boolean isSecure) {
        // 확장성을 위한 서브도메인 공유 정책
        String domain = isSecure ? PROD_DOMAIN : null;

        return ResponseCookie.from(name, value)
                .path("/")
                .domain(domain)
                .httpOnly(true)
                .maxAge(maxAge)
                .secure(isSecure)
                .sameSite("Lax")
                .build();
    }

    public static void addCookie(HttpServletResponse response, String name, String value, int maxAge, boolean isSecure) {
        ResponseCookie cookie = generateCookie(name, value, maxAge, isSecure);
        response.addHeader("Set-Cookie", cookie.toString());
    }

    public static void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name, boolean isSecure) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    // 값을 비우고, maxAge를 0으로 주기
                    ResponseCookie deleteCookie = generateCookie(name, "", 0, isSecure);
                    response.addHeader("Set-Cookie", deleteCookie.toString());
                    break;
                }
            }
        }
    }
}
