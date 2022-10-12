package com.keray.common;

import com.keray.common.handler.ApiJsonParamResolver;
import org.springframework.core.MethodParameter;

/**
 * @author by keray
 * date:2021/7/9 10:31 上午
 * 接口参数校验 接口参数解析完成后调用
 * 调用位置{@link ApiJsonParamResolver#resolveArgument}
 */
public interface ArgCheckHandler<C extends Object> {
    /**
     *<p>
     *   <h3>作者 keray</h3>
     *   <h3>时间： 2021/9/9 12:02 下午</h3>
     *   返回true表示支持的校验参数
     *</p>
     * @param parameter
     * @param org
     * @return <p> {@link boolean} </p>
     * @throws
     */
    boolean support(MethodParameter parameter, Object org);

    /**
     *<p>
     *   <h3>作者 keray</h3>
     *   <h3>时间： 2021/9/9 12:02 下午</h3>
     *   返回false校验失败，校验失败抛出异常
     *</p>
     * @param parameter
     * @param val
     * @return <p> {@link boolean} </p>
     * @throws
     */
    boolean check(MethodParameter parameter, C val);
}
