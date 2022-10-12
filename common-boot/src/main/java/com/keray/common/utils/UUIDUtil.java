package com.keray.common.utils;

import cn.hutool.core.lang.generator.SnowflakeGenerator;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


public class UUIDUtil {
    public static final SnowflakeGenerator SNOWFLAKE_GENERATOR = new SnowflakeGenerator();

    private UUIDUtil() {
    }

    public static String generateUUIDByTimestamp() {
        return generateUUIDByTimestamp("");
    }

    public static String generateUUIDByTimestamp(String end) {
        return generateUUID() + end;
    }

    public static Long generateUUID() {
        return SNOWFLAKE_GENERATOR.next();
    }


    public static void main(String[] args) {
        System.out.println(generateUUIDByTimestamp().length());
    }
}
