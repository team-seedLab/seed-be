package com.example.seedbe.global.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;

public class CookieUtil {

    // 쿠키 생성 (굽기)
    public static void addCookie(HttpServletResponse response, String name, String value, int maxAge, boolean isSecure) {

        String domain = isSecure ? "seedlab.cloud" : null;

        ResponseCookie cookie = ResponseCookie.from(name, value)
                .path("/")
                .domain(domain)
                .httpOnly(true)
                .maxAge(maxAge)
                .secure(isSecure)
                .sameSite("Lax")
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }

    // 쿠키 삭제 (로그아웃 시 사용)
    public static void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name, boolean isSecure) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {

                    String domain = isSecure ? "seedlab.cloud" : null;

                    ResponseCookie deleteCookie = ResponseCookie.from(name, "")
                            .path("/")
                            .domain(domain)
                            .httpOnly(true)
                            .maxAge(0)
                            .secure(isSecure)
                            .sameSite("Lax")
                            .build();

                    response.addHeader("Set-Cookie", deleteCookie.toString());
                    break;
                }
            }
        }
    }
}
