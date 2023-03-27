package com.keray.common.mysql;

import java.lang.annotation.*;

/**
 * @author by keray
 * date:2020/4/26 10:27 上午
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BaseDbUpdateWrapper {
}
