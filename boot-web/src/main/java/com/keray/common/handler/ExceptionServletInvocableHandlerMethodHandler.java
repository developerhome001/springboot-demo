package com.keray.common.handler;

import com.keray.common.Result;
import com.keray.common.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.HandlerMethod;

import java.util.LinkedList;
import java.util.Map;

/**
 * @author by keray
 * date:2020/9/7 9:36 下午
 */
@Slf4j
@Configuration
public class ExceptionServletInvocableHandlerMethodHandler<E extends Throwable> implements ServletInvocableHandlerMethodHandler, ExceptionHandler<E>, BeanPostProcessor {

    private final static ExceptionHandler<Throwable>[] EXCEPTION_HANDLERS = new ExceptionHandler[]{
            new IllegalArgumentExceptionHandler(),
            new CodeExceptionHandler()
    };

    private final LinkedList<ExceptionHandler<Throwable>> CONSUMER_HANDLER = new LinkedList<>();

    private final ExceptionHandler<Throwable> defaultExceptionHandler = new DefaultExceptionHandler();

    @Override
    public int getOrder() {
        return 200;
    }

    @Override
    public Object work(HandlerMethod handlerMethod, Object[] args, NativeWebRequest request, Map<Object, Object> workContext, ServletInvocableHandlerMethodCallback callback) throws Exception {
        try {
            Object result = callback.get();
            if (result instanceof Result.FailResult && ((Result.FailResult<?, ?>) result).getError() != null) {
                ExceptionHandler<Throwable> exceptionHandler = giveExceptionHandler(((Result.FailResult<?, ?>) result).getError());
                if (exceptionHandler == null) {
                    return result;
                }
                return exceptionHandler.errorHandler(((Result.FailResult<?, ?>) result).getError());
            }
            return result;
        } catch (Throwable error) {
            return errorHandler(error);
        }
    }


    @Override
    public boolean supper(Throwable e) {
        return false;
    }

    @Override
    public Result<?> errorHandler(Throwable error) {
        ExceptionHandler<Throwable> exceptionHandler = giveExceptionHandler(error);
        return exceptionHandler != null ? exceptionHandler.errorHandler(error) : defaultExceptionHandler.errorHandler(error);
    }

    private ExceptionHandler<Throwable> giveExceptionHandler(Throwable e) {
        for (ExceptionHandler<Throwable> eh : EXCEPTION_HANDLERS) {
            if (eh.supper(e)) {
                return eh;
            }
        }
        for (ExceptionHandler<Throwable> eh : CONSUMER_HANDLER) {
            if (eh.supper(e)) {
                return eh;
            }
        }
        return null;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof ExceptionHandler ch && !(ch instanceof ExceptionServletInvocableHandlerMethodHandler)) {
            CONSUMER_HANDLER.add(ch);
        }
        return BeanPostProcessor.super.postProcessBeforeInitialization(bean, beanName);
    }
}
