package com.keray.common.gateway.downgrade;

import com.keray.common.CommonResultCode;
import com.keray.common.IContext;
import com.keray.common.IUserContext;
import com.keray.common.Result;
import com.keray.common.exception.BizRuntimeException;
import com.keray.common.handler.ServletInvocableHandlerMethodCallback;
import com.keray.common.handler.ServletInvocableHandlerMethodHandler;
import com.keray.common.keray.KerayServletInvocableHandlerMethod;
import com.keray.common.threadpool.MemorySafeLinkedBlockingQueue;
import com.keray.common.threadpool.SysThreadPool;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.HandlerMethod;

import javax.annotation.Resource;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 接口降级服务
 */
@Configuration
@Slf4j
public class ApiDowngradeServletInvocableHandlerMethodHandler implements ServletInvocableHandlerMethodHandler {

    @Resource
    private ApiDowngradeRegister apiDowngradeRegister;

    @Resource
    private IContext context;

    private static final AtomicInteger COUNT = new AtomicInteger(0);

    private static final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(10, 10, 10,
            TimeUnit.SECONDS, new LinkedBlockingDeque<>(10000),
            r -> {
                Thread t = new Thread(r);
                t.setName("api-downgrade-write-" + COUNT.getAndIncrement());
                return t;
            });


    /**
     * 链表头部
     */
    private final Node header = new Node();

    /**
     * 链表尾部
     */
    private volatile Node tail = header;

    /**
     * 链表操作锁
     */
    private final Object clock = new Object();


    public ApiDowngradeServletInvocableHandlerMethodHandler() {
        // 已经超时
        Runnable run = () -> {
            while (!Thread.currentThread().isInterrupted()) {
                var time = System.currentTimeMillis();
                Node before = header;
                for (var node = header.next; node != null; node = node.next) {
                    var timeout = node.ani.timeout();
                    if (time - node.time > timeout) {
                        // 已经超时
                        try {
                            var n = remove(before, node);
                            threadPoolExecutor.execute(() -> requestTimeout(n));
                        } catch (Throwable ignore) {
                        }
                        // 移除节点后，before不变 还是以前的before
                        // 移除节点后还需要将当前遍历的node设置为before  在node = node.next才能遍历到下一个节点
                        node = before;
                        continue;
                    }
                    before = node;
                }
            }
        };
        new Thread(run, "TIMEOUT_MONITOR_THREAD").start();
    }

    /**
     * 顺序应该在apilog之后  Exception之前
     * 这里设置在QPS限制之前没问题，因为系统限制时还需要降级处理
     *
     * @return
     */
    @Override
    public int getOrder() {
        return 150;
    }

    @Override
    public Object work(HandlerMethod handlerMethod, Object[] args, NativeWebRequest request, ServletInvocableHandlerMethodCallback callback) throws Exception {
        var ani = handlerMethod.getMethodAnnotation(ApiDowngrade.class);
        // 没有服务器降级的接口不处理
        if (ani == null) return callback.get();
        var node = new Node();
        node.time = System.currentTimeMillis();
        node.handlerMethod = handlerMethod;
        node.args = args;
        node.ani = ani;
        node.request = request;
        node.thread = Thread.currentThread();
        node.context = context.export();
        put(node);
        Result result = (Result) callback.get();
        node.finish = true;
        if (node.timeout) {
            // 超时了直接将code设置为超时code 并将result修改为fail便于apilog记录日志
            if (result instanceof Result.SuccessResult<?> sr) {
                return Result.fail(sr.getData(), CommonResultCode.timeoutOk.getCode(), CommonResultCode.timeoutOk.getMessage(), null);
            }
            // 超时了本来就是异常  超时了就不需要获取配置的降级数据  保留原先的message 和code
            result.setMessage(String.format("接口超时降级 code=%s msg=%s", result.getCode(), result.getMessage()));
            // 设置为timeoutOk 使得com.keray.common.keray.KerayServletInvocableHandlerMethod.invokeAndHandle方法不在对返回值处理
            // 因为超时后socket已经返回数据并关闭了
            result.setCode(CommonResultCode.timeoutOk.getCode());
            return result;
        }
        // 如果接口时成功的不处理降级
        if (result instanceof Result.SuccessResult<?>) return result;
        Result.FailResult fail = (Result.FailResult) result;
        var code = fail.getCode();
        // 如果忽略降级这个错误的code  直接返回
        for (var i : ani.ignoreCodes()) if (code == i) return result;
        // 用户使用QPS限制时不降级  404资源未找到时不降级  超时任何情况下都降级
        if (code == CommonResultCode.notFund.getCode() ||
                code == CommonResultCode.limitedAccess.getCode()) return result;
        return returnData(ani, fail, request, args, handlerMethod);
    }

    /**
     * 请求超时处理
     *
     * @param node 数据
     */
    private void requestTimeout(Node node) {
        // 如果请求已经正常完成 直接返回
        if (node.finish) return;
        // 设置当前请求超时
        node.timeout = true;
        // 直接给socket写入降级数据
        var handler = node.handlerMethod;
        if (handler instanceof KerayServletInvocableHandlerMethod kerayServletInvocableHandlerMethod) {
            try {
                context.importConf(node.context);
                // 读取降级的数据
                var returnData = returnData(node.ani, Result.fail(CommonResultCode.timeoutOk), node.request, node.args, node.handlerMethod);
                returnData.setCode(CommonResultCode.timeoutOk.getCode());
                kerayServletInvocableHandlerMethod.breakpointReturn(returnData);
                var res = node.request.getNativeResponse(ServletResponse.class);
                // 直接关闭socket
                if (res != null) res.getOutputStream().close();
            } catch (Exception e) {
                try {
                    HttpServletResponse response = node.request.getNativeResponse(HttpServletResponse.class);
                    if (response == null) {
                        var r = node.request.getNativeResponse(ServletResponse.class);
                        if (r != null)
                            r.getOutputStream().close();
                        return;
                    }
                    response.setStatus(402);
                    response.getOutputStream().flush();
                    response.getOutputStream().close();
                } catch (Exception ex) {
                    log.error("接口降级失败", ex);
                }
            } finally {
                node.thread.interrupt();
            }
        }
    }

    private Result.FailResult returnData(ApiDowngrade ani, Result.FailResult fail, NativeWebRequest request, Object[] args, HandlerMethod handlerMethod) {
        // 开始降级
        var clazz = ani.handler();
        var instance = apiDowngradeRegister.getRegister(clazz);
        try {
            var resultObject = instance.handler(ani, fail, request, args, handlerMethod);
            // 将降级的对象设置为fail  但是code设置成功的code
            // 设置为fail为了日志记录  code设置为成功为了前端无感知
            if (resultObject instanceof Result.SuccessResult<?> sr) {
                // 强制将result的code设置为降级成功的code
                return Result.fail(sr.getData(),
                        CommonResultCode.subOk.getCode(),
                        fail.getMessage(),
                        fail.getError()
                );
            } else if (resultObject instanceof Result.FailResult<?, ?> fr) {
                return Result.fail(fr.getData(),
                        CommonResultCode.subOk.getCode(),
                        fr.getMessage(),
                        fr.getError()
                );
            }
            return Result.fail(resultObject, CommonResultCode.subOk.getCode(), fail.getMessage(), fail.getError());
        } catch (Throwable throwable) {
            // 如果降级也处理失败  直接返回原对象
            log.error("接口降级处理失败", throwable);
            return fail;
        }
    }

    /**
     * 链表尾部添加
     *
     * @param node
     */
    private void put(Node node) {
        synchronized (clock) {
            tail.next = node;
            tail = node;
        }
    }

    /**
     * 移除节点
     *
     * @param before 前面的节点
     * @param node   移除的节点
     * @return
     */
    private Node remove(Node before, Node node) {
        synchronized (clock) {
            before.next = node.next;
            node.next = null;
            // 如果node本身是tail  需要将before设置为tail
            if (before.next == null) tail = before;
        }
        return node;
    }


    /**
     * 监听对象
     */
    private static class Node {
        private final Object lock = new Object();
        private Node next;

        /**
         * 计时时间起点
         */
        private long time;

        /**
         * 注解
         */
        private ApiDowngrade ani;

        private NativeWebRequest request;

        private Object[] args;

        HandlerMethod handlerMethod;

        private Thread thread;

        /**
         * 是否已经超时
         */
        private volatile boolean timeout;

        /**
         * 请求是否完成
         */
        private volatile boolean finish;

        /**
         * 线程上下文
         */
        private Map<String, Object> context;
    }
}
