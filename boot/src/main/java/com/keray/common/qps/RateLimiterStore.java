package com.keray.common.qps;

/**
 * 令牌桶数据仓库
 */
public interface RateLimiterStore {
    /**
     *
     * 获取令牌桶数据
     *
     * @param key
     * @return
     */
    String getStoreData(String key);

    /**
     * 设置令牌桶数据
     *
     * @param key
     * @param data
     */
    void setStoreData(String key, String data);

}
