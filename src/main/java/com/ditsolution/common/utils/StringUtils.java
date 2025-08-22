package com.ditsolution.common.utils;


public final class StringUtils {
    private StringUtils() {}

    public static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    public static String trim(String s) {
        return s == null ? null : s.trim();
    }
}