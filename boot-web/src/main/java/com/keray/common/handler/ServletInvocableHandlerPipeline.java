package com.keray.common.handler;

import org.springframework.core.Ordered;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

/**
 * @author by keray
 * date:2020/6/3 9:34 上午
 * ServletInvocableHandlerMethod的执行管道
 * 聚合了 {@link HandlerInterceptor} 的preHandle postHandler 方法 可以在方法执行前 执行后进行处理
 * 该接口的好处是能在同一个方法里拿到接口入参和返回参数，这样避免了在postHandler里拿入参时需要用ThreadLocal来存储入参
 * 该接口重写work方法时必须保证调用一次callback.get()，必须仅一次。  callback.get()返回的值为接口的返回值
 * 可以参考
 * {@link ApiLogServletInvocableHandlerPipeline} 对接口进行日志输出
 * <p>
 * {@link ResultServletInvocableHandlerPipeline} 对接口返回参数进行Result结构封装
 * <p>
 * {@link ExceptionServletInvocableHandlerPipeline} 对接口抛出的异常进行封装
 * log ->  records  -> exception -> qps -> apidown -> exception -> result -> apiTime
 * 100 ->  200      -> 300       -> 400 -> 500     -> 600       -> 700    -> 800
 */
public interface ServletInvocableHandlerPipeline extends Ordered {

    @Override
    default int getOrder() {
        return Integer.MAX_VALUE;
    }

    /**
     * 管道执行函数
     * 函数最后一行必须为
     * china.work(handlerMethod, args, request, workContext);
     * 的固定写法
     * 类似filter对象的的filter函数最后都必须执行chain.doFilter(request, response);
     *
     * @param handlerMethod
     * @param args
     * @param request
     * @param workContext   管道上下文
     * @param china
     * @return
     * @throws Exception
     */
    default Object work(HandlerMethod handlerMethod, Object[] args, NativeWebRequest request, Map<Object, Object> workContext, ServletInvocableHandlerPipelineChina china) throws Exception {
        return work(handlerMethod, args, request, workContext, () -> china.work(handlerMethod, args, request, workContext));
    }


    default Object work(HandlerMethod handlerMethod, Object[] args, NativeWebRequest request, Map<Object, Object> workContext, ServletInvocableHandlerMethodCallback call) throws Exception {
        return call.get();
    }
}
