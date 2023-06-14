package com.keray.common.cache;

import cn.hutool.core.map.MapUtil;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.Map;

/**
 * @author by keray
 * date:2019/8/30 13:01
 */
@ConfigurationProperties(prefix = "keray.cache")
@Configuration
public class CacheConstants {

    public static final String S10 = "s10";
    public static final String S30 = "s30";
    public static final String M1 = "m1";
    public static final String M10 = "m10";
    public static final String M30 = "m30";
    public static final String H1 = "h1";
    public static final String H10 = "h10";
    public static final String D1 = "d1";
    public static final String D5 = "d5";
    public static final String D10 = "d10";
    public static final String D30 = "d30";
    public static final String Y1 = "y1";
    public static final String ALWAYS = "ALWAYS";


    @Getter
    @Setter
    private Map<String, Long> map;

    private final Map<String, Long> DEFAULT = MapUtil.<String, Long>builder()
            .put(S10, 10000L)
            .put(S30, 30000L)
            .put(M1, 60000L)
            .put(M10, 600000L)
            .put(M30, 1800000L)
            .put(H1, 3600000L)
            .put(H10, 36000000L)
            .put(D1, 86400000L)
            .put(D5, 432000000L)
            .put(D10, 864000000L)
            .put(D30, 2592000000L)
            .put(Y1, 31536000000L)
            .put(ALWAYS, -1L)
            .build();

    @PostConstruct
    public void init() {
        if (map == null) {
            map = DEFAULT;
        }
        for (var item : DEFAULT.entrySet()) {
            if (!map.containsKey(item.getKey())) {
                map.put(item.getKey(), item.getValue());
            }
        }
    }
}
