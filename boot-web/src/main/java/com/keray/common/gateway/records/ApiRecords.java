package com.keray.common.gateway.records;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiRecords {
    /**
     * 接口记录的uri  默认取request的uri
     * @return
     */
    String value();
}
