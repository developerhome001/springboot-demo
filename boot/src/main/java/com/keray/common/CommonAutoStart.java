package com.keray.common;

import com.keray.common.diamond.DiamondManger;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

@ComponentScan("com.keray.common")
@Configuration
public class CommonAutoStart {

    @Resource
    private DiamondManger DiamondManger;
}
