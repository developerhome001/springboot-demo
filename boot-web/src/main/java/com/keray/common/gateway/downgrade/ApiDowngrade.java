package com.keray.common.gateway.downgrade;

import java.lang.annotation.*;

/**
 * 接口降级标识
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiDowngrade {

    /**
     * 接口降级忽略的codes
     * 默认情况接口降级忽略QPS控制异常的接口
     * @return
     */
    int[] ignoreCodes() default {};

    /**
     * 接口降级标识 在调用handler时使用
     * 便于handler识别是哪个接口降级
     *
     * @return
     */
    int downgradeFlag() default 0;

    /**
     * 超时降级
     * 毫秒
     *
     * @return
     */
    int timeout() default 3000;

    /**
     * 降级默认返回的json
     *
     * @return
     */
    String json() default "";

    /**
     * 降级处理器
     *
     * @return
     */
    Class<? extends DowngradeHandler> handler() default DowngradeHandler.class;

}
