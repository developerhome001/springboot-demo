package com.keray.common.exception;

import com.keray.common.CommonResultCode;

public class QPSFailException extends Exception implements CodeException {
    public QPSFailException() {
    }

    public QPSFailException(String message) {
        super(message);
    }


    @Override
    public int getCode() {
        return CommonResultCode.limitedAccess.getCode();
    }

    @Override
    public String getMessage() {
        return CommonResultCode.limitedAccess.getMessage();
    }

}
