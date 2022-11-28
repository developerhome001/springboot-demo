package com.keray.common.handler;

import org.springframework.core.Ordered;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * @author by keray
 * date:2020/6/3 9:34 上午
 * 接口调用前后拦截位置
 * 聚合了 {@link HandlerInterceptor} 的preHandle postHandler 方法 可以在方法执行前 执行后进行处理
 * 该接口的好处是能在同一个方法里拿到接口入参和返回参数，这样避免了在postHandler里拿入参时需要用ThreadLocal来存储入参
 * 该接口重写work方法时必须保证调用一次callback.get()，必须仅一次。  callback.get()返回的值为接口的返回值
 * 可以参考
 * {@link ApiLogServletInvocableHandlerMethodHandler} 对接口进行日志输出
 * <p>
 * {@link ResultServletInvocableHandlerMethodHandler} 对接口返回参数进行Result结构封装
 * <p>
 * {@link ExceptionServletInvocableHandlerMethodHandler} 对接口抛出的异常进行封装
 */
public interface ServletInvocableHandlerMethodHandler extends Ordered {

    @Override
    default int getOrder() {
        return Integer.MAX_VALUE;
    }

    default Object work(HandlerMethod handlerMethod, Object[] args, NativeWebRequest request, ServletInvocableHandlerMethodCallback callback) throws Exception {
        return callback.get();
    }
}
