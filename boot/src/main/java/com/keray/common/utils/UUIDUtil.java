package com.keray.common.utils;

import cn.hutool.core.lang.generator.SnowflakeGenerator;
import cn.hutool.core.net.Ipv4Util;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


@Slf4j
public class UUIDUtil {
    public static SnowflakeGenerator SNOWFLAKE_GENERATOR;

    static {
        // 0-5位 为wordId  5-10位为dataId
        log.info("hostIp:{}", CommonUtil.hostIp(true));
        long ipVal = Ipv4Util.ipv4ToLong(CommonUtil.hostIp(true));
        log.info("雪花id:{} {}", ipVal & 31, (ipVal >> 5) & 31);
        SNOWFLAKE_GENERATOR = new SnowflakeGenerator(ipVal & 31, (ipVal >> 5) & 31);
    }

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

}
