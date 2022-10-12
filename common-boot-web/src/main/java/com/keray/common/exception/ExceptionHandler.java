package com.keray.common.exception;

import com.keray.common.Result;

public interface ExceptionHandler<E extends Throwable> {
    boolean supper(Throwable e);

    Result<?> errorHandler(E error);

}
