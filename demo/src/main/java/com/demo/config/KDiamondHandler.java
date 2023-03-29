package com.demo.config;

import com.keray.common.diamond.DiamondManger;
import com.keray.common.diamond.handler.DiamondHandler;
import lombok.SneakyThrows;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import javax.annotation.Resource;

@Configuration
public class KDiamondHandler implements DiamondHandler {

    @Resource
    @Lazy
    private DiamondManger diamondManger;

    @SneakyThrows
    @Override
    public void handler(String key, String value) {
        diamondManger.diamondChange(key, value);
    }
}
