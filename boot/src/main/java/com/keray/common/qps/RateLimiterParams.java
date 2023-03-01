package com.keray.common.qps;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;


@Getter
@Setter
@Accessors(chain = true)
public class RateLimiterParams {

    private String uuid;
    /**
     * 令牌key
     */
    private String key;
    /**
     * 令牌桶空间名
     */
    private String namespace;
    /**
     * 最大令牌数量
     */
    private int maxRate;
    /**
     * 获取令牌数量
     */
    private int acquireCount = 1;
    /**
     * 下次产生令牌时间间隔（毫秒）
     */
    private int millisecond = 1000;
    /**
     * 在指定的Cron时间点产生令牌
     */
    private String appointCron;
    /**
     * 下次产生令牌数量
     */
    private int recoveryCount = 1;
    /**
     * 令牌限流策略
     */
    private RejectStrategy rejectStrategy;
    /**
     * 等待时间
     */
    private int waitTime = 5000;
    /**
     * 等待时间间隔
     */
    private int waitSpeed = 50;
    /**
     * 是否是可以释放的令牌桶  可以释放的令牌桶不会自己生成令牌
     */
    private boolean needRelease;

    /**
     * 令牌释放个数
     */
    private int releaseCnt = 1;
    /**
     * 释放型令牌版本 版本与前面不一致时自动将令牌扩容到最大值 保证可释放型令牌实现动态扩容
     */
    private long releaseVersion = 1;
}
