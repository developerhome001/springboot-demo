package com.keray.common.gateway.limit;

import com.keray.common.annotation.QpsIgnore;
import com.keray.common.annotation.QpsPublicIpIgnore;
import com.keray.common.annotation.QpsPublicUrlIgnore;
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
     * 流控阶段
     */
    enum RateLimiterStep {
        start, // 开始阶段
        consumer, // 配置数据限制阶段
        ip, // ip控制阶段
        url, // url控制阶段
        annotation // 注解控制阶段
    }


    /**
     * 检验请求是否可以跳过流控
     *
     * @param step    流控阶段
     * @param value   流控阶段的值
     * @param request 请求
     * @param handler 处理器
     * @return
     */
    default boolean requestIgnoreRateLimiter(AbstractRateLimiterInterceptor.RateLimiterStep step, Object value, NativeWebRequest request, HandlerMethod handler) {
        if (step == RateLimiterStep.start) {
            // 特殊QPS放行
            // 忽略注解放行
            return "keray".equals(request.getHeader("keray")) || handler.hasMethodAnnotation(QpsIgnore.class);
        }
        if (step == RateLimiterStep.ip) {
            if ("0.0.0.0/0".equals(value)) {
                return handler.hasMethodAnnotation(QpsPublicIpIgnore.class);
            }
        } else if (step == RateLimiterStep.url) {
            if ("*".equals(value)) {
                return handler.hasMethodAnnotation(QpsPublicUrlIgnore.class);
            }
        }
        return false;
    }


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
