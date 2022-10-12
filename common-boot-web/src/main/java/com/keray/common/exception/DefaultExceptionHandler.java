package com.keray.common.exception;

import com.keray.common.CommonResultCode;
import com.keray.common.Result;

public class DefaultExceptionHandler implements ExceptionHandler<Throwable> {

    @Override
    public boolean supper(Throwable e) {
        return true;
    }

    @Override
    public Result<?> errorHandler(Throwable error) {
        Throwable exception = error;

        for (int i = 0; exception != null && i < 10; i++) {
            if (exception instanceof CodeException ce && ce.getCode() != CommonResultCode.unknown.getCode()) {
                return Result.fail(((CodeException) exception).getCode(), exception.getMessage(), error);
            }
            exception = exception.getCause();
        }
        return Result.fail(CommonResultCode.unknown, error);
    }
}
