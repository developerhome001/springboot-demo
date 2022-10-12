package com.keray.common;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@MapperScan("com.keray.common.service.mapper")
@ComponentScan("com.keray.common")
@Configuration
public class CommonAutoStart {


}
