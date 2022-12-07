package com.keray.common.cache;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoFlushCache {

    /**
     * 多久自动刷新缓存
     *
     * @return
     */
    int time();

    /**
     * 是否启动时自动执行缓存
     * 支持初始化的函数必须无参数
     *
     * @return
     */
    boolean init() default false;

}
