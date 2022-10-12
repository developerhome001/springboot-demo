package com.keray.common;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;

import java.util.Map;

/**
 * @author by keray
 * date:2019/7/26 14:21
 */
public interface IContext {

    default boolean loginStatus() {
        return StrUtil.isNotEmpty(currentUserId());
    }

    /**
     * 获取当前登陆用户的Id
     *
     * @return 用户Id
     */
    String currentUserId();

    void setUserId(String userId);


    /**
     * 当前使用Ip
     *
     * @return 当前IP
     */
    String currentIp();

    void setIp(String ip);



    default Map<String, Object> export() {
        return MapUtil.<String, Object>builder()
                .put("userId", currentUserId())
                .put("ip", currentIp())
                .build();
    }

    default void importConf(Map<String, Object> map) {
        if (MapUtil.isEmpty(map)) {
            return;
        }
        setUserId((String) map.get("userId"));
        setIp((String) map.get("ip"));
    }

    default void clear() {
        setUserId(null);
        setIp(null);
    }
}
