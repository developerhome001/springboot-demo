package com.keray.common.gateway.limit;

import com.keray.common.annotation.RateLimiterApi;
import com.keray.common.exception.QPSFailException;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.HandlerMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

public interface RateLimiterInterceptor {

    /**
     * 自定义流控
     *
     * @param request
     * @param handler
     * @param releaseList 可释放型的流控需要再成功后需要添加勾子
     * @return 自定义流控是否运行过
     * @throws InterruptedException
     * @throws QPSFailException
     */
    boolean interceptorConsumer(NativeWebRequest request, HandlerMethod handler, List<QpsData> releaseList) throws InterruptedException, QPSFailException;

    /**
     * 注解QPS拦截
     *
     * @param data
     * @param request
     * @param handler
     * @throws InterruptedException
     * @throws QPSFailException
     */
    void interceptor(RateLimiterApi data, NativeWebRequest request, HandlerMethod handler, List<QpsData> releaseList) throws InterruptedException, QPSFailException;

    /**
     * QPS执行完成后释放
     *
     * @param qpsData
     * @param request
     * @param handler
     * @throws InterruptedException
     */
    void release(String key, QpsData qpsData, NativeWebRequest request, HandlerMethod handler) throws InterruptedException;

    /**
     * QPS拒绝后的回调
     *
     * @param request
     * @param response
     * @param handler
     * @param e        失败信息
     * @return 返回true后将继续执行接口  false抛出QPS失败信息
     */
    boolean failCall(HttpServletRequest request, HttpServletResponse response, HandlerMethod handler, QPSFailException e) throws QPSFailException, InterruptedException;
}
