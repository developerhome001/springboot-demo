package com.keray.common.exception;

import cn.hutool.core.util.StrUtil;
import com.keray.common.CommonResultCode;
import com.keray.common.qps.RateLimiterParams;

public class QPSFailException extends Exception implements CodeException {

    private final RateLimiterParams params;

    private final boolean system;

    public QPSFailException(RateLimiterParams params) {
        this(CommonResultCode.limitedAccess.getMessage(), params);
    }

    public QPSFailException(boolean system, RateLimiterParams params) {
        this(system, system ? CommonResultCode.systemAccess.getMessage() : CommonResultCode.limitedAccess.getMessage(), params);
    }

    public QPSFailException(String message, RateLimiterParams params) {
        this(false, message, params);
    }

    public QPSFailException(boolean system, String message, RateLimiterParams params) {
        super(StrUtil.isEmpty(message) ? system ? CommonResultCode.systemAccess.getMessage() : CommonResultCode.limitedAccess.getMessage() : message);
        this.system = system;
        this.params = params;
    }


    @Override
    public int getCode() {
        return system ? CommonResultCode.systemAccess.getCode() : CommonResultCode.limitedAccess.getCode();
    }

    public RateLimiterParams getParams() {
        return params;
    }
}
