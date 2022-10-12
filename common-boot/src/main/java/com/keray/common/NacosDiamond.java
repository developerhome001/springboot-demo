package com.keray.common;

import org.apache.logging.slf4j.SLF4JLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author by keray
 * date:2021/4/2 12:35 下午
 */
public interface NacosDiamond {

    Logger log = LoggerFactory
            .getLogger(SLF4JLogger.class);

    default void beforeReBind() {
        log.info("配置更新：class={},hash={}", this.getClass(), this.hashCode());
    }

}
