package com.example.seedbe.global.util;

import java.time.format.DateTimeFormatter;

public class DateTimeUtil {
    private DateTimeUtil() {
        throw new IllegalStateException("Utility class");
    }

    // 프로젝트 전역에서 사용할 표준 날짜 포맷 상수
    public static final DateTimeFormatter GLOBAL_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
}
