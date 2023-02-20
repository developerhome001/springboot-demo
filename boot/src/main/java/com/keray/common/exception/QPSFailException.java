package com.keray.common.exception;

import com.keray.common.CommonResultCode;

public class QPSFailException extends Exception implements CodeException {

    private final boolean system;

    public QPSFailException() {
        this(CommonResultCode.limitedAccess.getMessage());
    }

    public QPSFailException(boolean system) {
        this(system, system ? CommonResultCode.systemAccess.getMessage() : CommonResultCode.limitedAccess.getMessage());
    }

    public QPSFailException(String message) {
        this(false, message);
    }

    public QPSFailException(boolean system, String message) {
        super(message);
        this.system = system;
    }


    @Override
    public int getCode() {
        return system ? CommonResultCode.systemAccess.getCode() : CommonResultCode.limitedAccess.getCode();
    }

}
