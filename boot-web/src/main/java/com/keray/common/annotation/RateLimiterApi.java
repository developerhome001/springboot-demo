package com.keray.common.annotation;

import com.keray.common.gateway.limit.RateLimiterApiTarget;
import com.keray.common.qps.RejectStrategy;
import com.keray.common.qps.spring.RateLimiterBean;
import com.keray.common.qps.spring.RedisRateLimiterBean;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimiterApi {

    /**
     * 令牌桶空间
     *
     * @return
     */
    String namespace();

    /**
     * 空间内令牌桶最大令牌数量
     *
     * @return
     */
    int maxRate();

    /**
     * 下一次产生令牌的时间间隔（毫秒）
     * 当存在指定时间时该值无效
     *
     * @return
     */
    int millisecond() default 1000;

    /**
     * 指定时间恢复
     *
     * @return
     */
    String appointCron() default "";

    /**
     * 下一次产生令牌的令牌数量
     *
     * @return
     */
    int recoveryCount() default 1;

    /**
     * 令牌限制目标
     * user基于用户id限制
     * ip基于客户端ip限制
     * duid基于客户端设备id限制
     *
     * @return
     */
    RateLimiterApiTarget target() default RateLimiterApiTarget.user;

    /**
     * 限制策略 令牌获取失败的处理
     *
     * @return
     */
    RejectStrategy rejectStrategy() default RejectStrategy.throw_exception;

    /**
     * QPS拒接错误文案
     *
     * @return
     */
    String rejectMessage() default "";

    /**
     * 拒绝时等待的时间
     *
     * @return
     */
    int waitTime() default 5000;

    /**
     * 拒绝等待时的唤醒间隔时间
     *
     * @return
     */
    int waitSpeed() default 50;

    /**
     * 默认的令牌桶
     *
     * @return
     */
    String bean() default "redisRateLimiterBean";

}
