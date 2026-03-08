package com.example.seedbe.global.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class CookieUtil {

    // 쿠키 생성 (굽기)
    public static void addCookie(HttpServletResponse response, String name, String value, int maxAge, boolean isSecure) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setHttpOnly(true); // XSS 공격 방어
        cookie.setMaxAge(maxAge);
        cookie.setSecure(isSecure);
        response.addCookie(cookie);
    }

    // 쿠키 삭제 (로그아웃 시 사용)
    public static void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    cookie.setValue("");
                    cookie.setPath("/");
                    cookie.setMaxAge(0); // 수명을 0으로 만들어서 브라우저가 삭제하게 만듦
                    response.addCookie(cookie);
                }
            }
        }
    }
}
