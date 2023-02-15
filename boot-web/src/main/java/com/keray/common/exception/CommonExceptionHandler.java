package com.keray.common.exception;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson2.JSON;
import com.keray.common.CommonResultCode;
import com.keray.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.core.annotation.Order;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

@ControllerAdvice
@Order(0)
@Slf4j
public class CommonExceptionHandler {

    private String requestMessage(HttpServletRequest request) {
        var map = new LinkedHashMap<>();
        var en = request.getHeaderNames();
        while (en.hasMoreElements()) {
            var key = en.nextElement();
            map.put(key, request.getHeader(key));
        }
        return String.format("uri=%s,header=%s", request.getRequestURI(), JSON.toJSON(map));
    }

    @SuppressWarnings("rawtypes")
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class, ConstraintViolationException.class})
    @ResponseBody
    public Object resolveExceptionMVC(HttpServletRequest request, HttpServletResponse response, Object handler, Exception exception) {
        log.error(requestMessage(request) + "\nresolveExceptionMVC..........", exception);
        Result result = null;
        if (exception instanceof MethodArgumentNotValidException methodArgumentNotValidException) {
            BindingResult bindingResult = methodArgumentNotValidException.getBindingResult();
            // 获取校验结果
            if (bindingResult.hasErrors()) {
                // 输出错误信息
                List<FieldError> errorList = bindingResult.getFieldErrors();
                if (CollUtil.isNotEmpty(errorList)) {
                    result = Result.fail(CommonResultCode.illegalArgument.getCode(), errorList.get(0).getField() + errorList.get(0).getDefaultMessage());
                } else {
                    result = Result.fail(CommonResultCode.illegalArgument);
                }
            }
        } else if (exception instanceof BindException bindException) {
            BindingResult bindingResult = bindException.getBindingResult();
            // 获取校验结果
            if (bindingResult.hasErrors()) {
                // 输出错误信息
                List<FieldError> errorList = bindingResult.getFieldErrors();
                StringBuilder sb = new StringBuilder();
                for (FieldError fieldError : errorList) {
                    sb.append(fieldError.getField()).append(":").append(fieldError.getDefaultMessage()).append("\n");
                }
                result = Result.fail(CommonResultCode.unknown.getCode(), sb.toString());
            }
        } else if (exception instanceof ConstraintViolationException constraintViolationException) {
            Set<ConstraintViolation<?>> string = constraintViolationException
                    .getConstraintViolations();

            StringBuilder sb = new StringBuilder();
            for (ConstraintViolation<?> constraintViolation : string) {
                sb.append(constraintViolation.getMessage()).append("\n");
            }
            result = Result.fail(CommonResultCode.unknown.getCode(), sb.toString());
        }
        return result;
    }

    @ExceptionHandler({MissingServletRequestParameterException.class})
    @ResponseBody
    public Object resolveException(HttpServletRequest request, HttpServletResponse response, Object handler, MissingServletRequestParameterException exception) {
        log.warn(requestMessage(request) + "\nMissingServletRequestParameterException..........", exception);
        return Result.fail(CommonResultCode.argumentNotPresent.getCode(), exception.getParameterName());
    }

    @ExceptionHandler({IllegalArgumentException.class, HttpRequestMethodNotSupportedException.class})
    @ResponseBody
    public Object resolveExceptionParam(HttpServletRequest request, HttpServletResponse response, Object handler, Exception exception) {
        log.error(requestMessage(request) + "\nresolveExceptionParam..........", exception);

        if (exception instanceof IllegalArgumentException) {
            return Result.fail(CommonResultCode.illegalArgument.getCode(), exception.getMessage(), exception);
        } else if (exception instanceof HttpRequestMethodNotSupportedException) {
            return Result.fail(CommonResultCode.unknown.getCode(), exception.getMessage());
        }
        return Result.fail(CommonResultCode.unknown);
    }


    @ExceptionHandler({BizRuntimeException.class})
    @ResponseBody
    public Object resolveException(HttpServletRequest request, HttpServletResponse response, Object handler, BizRuntimeException exception) {
        log.error(requestMessage(request) + "\nBizRuntimeException..........", exception);
        CodeException codeException = codeException(exception);
        return Result.fail(codeException.getCode(), codeException.getMessage(), (Exception) codeException);
    }


    @ExceptionHandler({BizException.class})
    @ResponseBody
    public Object resolveException(HttpServletRequest request, HttpServletResponse response, Object handler, BizException exception) {
        log.error(requestMessage(request) + "\nBizException..........", exception);
        CodeException codeException = codeException(exception);
        return Result.fail(codeException.getCode(), codeException.getMessage(), (Exception) codeException);
    }

    @ExceptionHandler({
            ClientAbortException.class})
    @ResponseBody
    public Object resolveException(HttpServletRequest request, HttpServletResponse response, Object handler, ClientAbortException exception) {
        log.error(requestMessage(request) + "\nClientAbortException");
        return Result.fail(CommonResultCode.unknown, exception);
    }

    @ExceptionHandler({Exception.class})
    @ResponseBody
    public Object resolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception exception) {
        log.error(requestMessage(request) + "\nException..........", exception);
        if (exception instanceof CodeException codeException) {
            return Result.fail(codeException.getCode(), codeException.getMessage(), (Exception) codeException);
        }
        return Result.fail(CommonResultCode.unknown, exception);
    }

    private CodeException codeException(CodeException runtimeException) {
        Throwable child = runtimeException.getCause();
        if (child instanceof CodeException c) {
            return codeException(c);
        }
        return runtimeException;
    }

}
