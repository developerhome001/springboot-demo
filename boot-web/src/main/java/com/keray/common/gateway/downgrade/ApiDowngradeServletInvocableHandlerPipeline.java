package com.keray.common.gateway.downgrade;

import cn.hutool.core.collection.CollUtil;
import com.keray.common.*;
import com.keray.common.exception.BizRuntimeException;
import com.keray.common.handler.ServletInvocableHandlerMethodCallback;
import com.keray.common.handler.ServletInvocableHandlerPipeline;
import com.keray.common.keray.KerayServletInvocableHandlerMethod;
import com.keray.common.threadpool.MemorySafeLinkedBlockingQueue;
import com.keray.common.threadpool.SysThreadPool;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.HandlerMethod;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.util.LinkedList;
import java.util.List;
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
public class ApiDowngradeServletInvocableHandlerPipeline implements ServletInvocableHandlerPipeline {

    public final static String HOOKS_KEY = "ApiDowngradeHooks";
    public final static String CONTEXT_NODE = "ContextNode";

    @Resource
    private ApiDowngradeRegister apiDowngradeRegister;

    @Resource
    private ApplicationContext applicationContext;

    private final List<ApiDowngradeHandler> apiDowngradeHandlers = new LinkedList<>();

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

    private final Thread backstageThread;

    public ApiDowngradeServletInvocableHandlerPipeline() {
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
        backstageThread = new Thread(run, "TIMEOUT_MONITOR_THREAD");
        backstageThread.start();
    }


    @EventListener(ApplicationStartedEvent.class)
    public void init() {
        apiDowngradeHandlers.addAll(applicationContext.getBeansOfType(ApiDowngradeHandler.class).values());
    }

    @PreDestroy
    public void shutdown() {
        backstageThread.interrupt();
        threadPoolExecutor.shutdown();
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
    public Object work(HandlerMethod handlerMethod, Object[] args, NativeWebRequest request, Map<Object, Object> workContext, ServletInvocableHandlerMethodCallback callback) throws Exception {
        var ani = handlerMethod.getMethodAnnotation(ApiDowngrade.class);
        // 没有服务器降级的接口不处理
        if (ani == null) return callback.get();
        for (var handler : apiDowngradeHandlers) {
            if (!handler.handler(ani)) return callback.get();
        }
        var node = new Node();
        node.time = System.currentTimeMillis();
        node.handlerMethod = handlerMethod;
        node.args = args;
        node.ani = ani;
        node.request = request;
        node.thread = Thread.currentThread();
        node.context = context.export();
        node.workContext = workContext;
        put(node);
        workContext.put(HOOKS_KEY, new LinkedList<>());
        workContext.put(CONTEXT_NODE, node);
        Result result = (Result) callback.get();
        node.finish = true;
        if (node.timeout) {
            // 超时了直接将code设置为超时code 并将result修改为fail便于apilog记录日志
            if (result instanceof Result.SuccessResult<?> sr) {
                return Result.fail(sr.getData(), CommonResultCode.timeoutOk.getCode(), CommonResultCode.timeoutOk.getMessage(), null);
            }
            var fail = (Result.FailResult) result;
            // 如果是中断异常 原因是超时后原执行线程被中断  中断执行点在
            // requestTimeout函数执行
            if (fail.getError() instanceof InterruptedException) {
                result.setMessage("接口执行超时被中断");
            }
            // 设置为timeoutOk 使得com.keray.common.keray.KerayServletInvocableHandlerMethod.invokeAndHandle方法不在对返回值处理
            // 因为超时后socket已经返回数据并关闭了
            result.setCode(CommonResultCode.timeoutOk.getCode());
            result.setApiDown(true);
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
        return returnData(ani, fail, request, args, handlerMethod, CommonResultCode.subOk.getCode());
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
                var returnData = returnData(node.ani, Result.fail(CommonResultCode.timeoutOk), node.request, node.args, node.handlerMethod, CommonResultCode.timeoutOk.getCode());
                kerayServletInvocableHandlerMethod.breakpointReturn(returnData);
                var res = node.request.getNativeResponse(ServletResponse.class);
                // 直接关闭socket
                if (res != null) res.getOutputStream().close();
            } catch (Exception e) {
                log.error("接口降级失败", e);
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
                    log.error("接口降级失败1", ex);
                }
            } finally {
                // 必须在finally执行中断，保证前面的socket已经写入完成
                // 在写入之前中断会导致原线程直接异常直接完毕，导致原先的流程走到socket写入流程
                // 如果原执行流程比上面的socket先写入会导致结果不准确，如果同时写入会导致异常
                // 所以必须要保证上面的socket写入完成后执行原流程中断
                // 中断原先的执行线程之前执行管道添加的勾子函数
                var hooks = (List<Runnable>) node.workContext.get(HOOKS_KEY);
                if (CollUtil.isNotEmpty(hooks)) {
                    for (var hook : hooks) hook.run();
                }
                node.thread.interrupt();
            }
        }
    }

    private Result.FailResult returnData(ApiDowngrade ani, Result.FailResult fail, NativeWebRequest request, Object[] args, HandlerMethod handlerMethod, int code) {
        // 开始降级
        var clazz = ani.handler();
        var instance = apiDowngradeRegister.getRegister(clazz);
        try {
            var resultObject = instance.handler(ani, fail, request, args, handlerMethod);
            // 将降级的对象设置为fail  但是code设置成功的code
            // 设置为fail为了日志记录  code设置为成功为了前端无感知
            if (resultObject instanceof Result.SuccessResult<?> sr) {
                // 强制将result的code设置为降级成功的code
                var r = Result.fail(sr.getData(),
                        code,
                        fail.getMessage(),
                        fail.getError()
                );
                r.setApiDown(true);
                return r;
            } else if (resultObject instanceof Result.FailResult<?, ?> fr) {
                fr.setApiDown(true);
                return fr;
            }
            var r = Result.fail(resultObject, code, fail.getMessage(), fail.getError());
            r.setApiDown(true);
            return r;
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
    @Getter
    @Setter
    public static class Node {
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
        /**
         * 管道流上下文
         */
        private Map<Object, Object> workContext;
    }
}
