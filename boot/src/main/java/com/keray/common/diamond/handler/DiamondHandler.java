package com.keray.common.diamond.handler;


/**
 * 动态配置管理
 */
public interface DiamondHandler {

    /**
     * 配置变化处理
     * @param key
     * @param value
     */
    void handler(String key, String value);
}
