package com.keray.common.diamond;

import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Diamond {

    /**
     * 值处理器
     *
     * @return
     */
    Class<? extends ValueHandler> handler() default DefaultValueHandler.class;

    String value();

    /**
     * 默认值
     */
    String def() default "";
}
