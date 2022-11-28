package com.keray.common.exception;

import com.keray.common.CommonResultCode;
import com.keray.common.Result;

/**
 * 具有code-message的错误处理
 */
public class CodeExceptionHandler implements ExceptionHandler<Throwable> {

    @Override
    public boolean supper(Throwable e) {
        return e instanceof CodeException;
    }

    @Override
    public Result<?> errorHandler(Throwable error) {
        CodeException ex = (CodeException) error;
        if (ex.getCode() == CommonResultCode.unknown.getCode() && ex.getCause() instanceof CodeException) {
            return errorHandler(ex.getCause());
        }
        return Result.fail(ex.getCode(), ex.getMessage(), error);
    }
}
