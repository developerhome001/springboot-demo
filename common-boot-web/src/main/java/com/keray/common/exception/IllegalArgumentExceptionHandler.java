package com.keray.common.exception;

import com.keray.common.CommonResultCode;
import com.keray.common.Result;

public class IllegalArgumentExceptionHandler implements ExceptionHandler<IllegalArgumentException> {

    @Override
    public boolean supper(Throwable e) {
        return e instanceof IllegalArgumentException;
    }

    @Override
    public Result<?> errorHandler(IllegalArgumentException error) {
        return Result.fail(CommonResultCode.unknown.getCode(), error.getMessage(), error);
    }
}
