package com.keray.common;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author by keray
 * date:2020/9/24 9:59 下午
 */
public class AliyunPlugins implements IPlugins {
    protected final AliyunConfig aliyunConfig;
    public AliyunPlugins(AliyunConfig aliyunConfig) {
        this.aliyunConfig = aliyunConfig;
    }
}
