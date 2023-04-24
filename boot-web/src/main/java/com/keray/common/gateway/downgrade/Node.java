package com.keray.common.gateway.downgrade;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.method.HandlerMethod;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.util.Map;

/**
 * 监听对象
 */
@Getter
@Setter
public class Node {


    /**
     * 计时时间起点
     */
    private final long time;

    /**
     * 注解
     */
    private final ApiDowngrade ani;

    private final ServletRequest request;

    private final ServletResponse response;

    private final Object[] args;

    private final HandlerMethod handlerMethod;

    private final Thread thread;

    /**
     * 线程上下文
     */
    private final Map<String, Object> context;
    /**
     * 管道流上下文
     */
    private final Map<Object, Object> workContext;

    public Node(long time, ApiDowngrade ani, ServletRequest request, ServletResponse response,
                Object[] args, HandlerMethod handlerMethod,
                Thread thread, Map<String, Object> context, Map<Object, Object> workContext) {
        this.time = time;
        this.ani = ani;
        this.request = request;
        this.response = response;
        this.args = args;
        this.handlerMethod = handlerMethod;
        this.thread = thread;
        this.context = context;
        this.workContext = workContext;
    }

    /**
     * 是否已经超时
     */
    private boolean timeout;

    /**
     * 请求是否完成
     */
    private boolean finish;

    /**
     * 下一个节点
     */
    private Node next;

    public synchronized boolean isTimeout() {
        return timeout;
    }

    public synchronized void setTimeout(boolean timeout) {
        this.timeout = timeout;
    }

    public synchronized boolean isFinish() {
        return finish;
    }

    public synchronized void setFinish(boolean finish) {
        this.finish = finish;
    }
}

