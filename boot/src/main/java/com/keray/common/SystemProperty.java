package com.keray.common;

public class SystemProperty {

    /**
     * result对象成功设置code值
     */
    public static final String RESULT_OK_CODE = "RESULT_OK";
    /**
     * result对象失败设置code值
     */
    public static final String RESULT_ERROR_CODE = "RESULT_ERROR";
    /**
     * header头表示appid的key
     */
    public static final String HEADER_APPID = "HEADER_APPID";


    /**
     * MemoryRateLimiterStore内存流控最大支持的key  大于后流控数据被清空重新计数
     */
    public static final String MEMORY_RATE_MAX_LEN = "MEMORY_RATE_MAX_LEN";
    /**
     * sys线程池的大小
     */
    public static final String SYS_POOL_SIZE = "SYS_POOL_SIZE";
    /**
     * duid的cookie的domain
     */
    public static final String DUID_DOMAIN = "DUID_DOMAIN";
}
