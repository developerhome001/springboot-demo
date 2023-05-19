package com.keray.common.handler;

import com.keray.common.Result;
import com.keray.common.exception.*;
import lombok.Getter;
import lombok.Setter;
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
public class ExceptionServletInvocableHandlerPipeline<E extends Throwable> implements ServletInvocableHandlerPipeline, ExceptionHandler<E>, BeanPostProcessor {

    private final static ExceptionHandler<Throwable>[] EXCEPTION_HANDLERS = new ExceptionHandler[]{
            new IllegalArgumentExceptionHandler(),
            new CodeExceptionHandler()
    };

    @Getter
    @Setter
    private int order = 200;

    private final LinkedList<ExceptionHandler<Throwable>> CONSUMER_HANDLER = new LinkedList<>();

    private final ExceptionHandler<Throwable> defaultExceptionHandler = new DefaultExceptionHandler();

    @Override
    public Object work(HandlerMethod handlerMethod, Object[] args, NativeWebRequest request, Map<Object, Object> workContext, ServletInvocableHandlerMethodCallback callback) throws Exception {
        try {
            return callback.get();
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
        if (bean instanceof ExceptionHandler ch && !(ch instanceof ExceptionServletInvocableHandlerPipeline)) {
            CONSUMER_HANDLER.add(ch);
        }
        return BeanPostProcessor.super.postProcessBeforeInitialization(bean, beanName);
    }
}
