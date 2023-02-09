package com.keray.common.gateway.limit;

import cn.hutool.core.util.StrUtil;
import com.keray.common.qps.RejectStrategy;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class QpsData {
    /**
     * 最大令牌数
     */
    private Integer maxRate;

    /**
     * 空间名
     */
    private String namespace;

    // 严格qps限制   严格限制时使用redis分布式qps仓库 非严格模式使用单机qps限制
    private boolean strict;


    /**
     * ip范围限制时 确定是单个ip限制还是限制ip范围
     */
    private RateLimiterApiTarget target;


    /**
     * 下一次产生令牌的时间间隔（毫秒）
     * 当存在指定时间时该值无效
     */
    private int millisecond = 1000;

    /**
     * 指定时间恢复
     */
    private String appointCron;

    /**
     * 下一次产生令牌的令牌数量
     *
     * @return
     */
    private int recoveryCount = 1;

    /**
     * QPS拒绝时的提示信息
     */
    private String rejectMessage;

    /**
     * 拒绝方式
     */
    private RejectStrategy rejectStrategy = RejectStrategy.throw_exception;

    /**
     * 拒绝等待时等待时间
     */
    private int waitTime = 5000;

    /**
     * 拒绝等待时等待间隔
     */
    private int waitSpeed = 50;

    public QpsData(Integer cnt, boolean strict) {
        this.maxRate = cnt;
        this.strict = strict;
    }

    public QpsData(String namespace) {
        this.namespace = namespace;
    }

    public String getNamespace(String def) {
        return StrUtil.isNotBlank(namespace) ? namespace : def;
    }
}

