package com.keray.common.exception;

import com.keray.common.CommonResultCode;

public class QPSFailException extends Exception implements CodeException {

    public QPSFailException() {
        super(CommonResultCode.limitedAccess.getMessage());
    }

    public QPSFailException(String message) {
        super(message);
    }


    @Override
    public int getCode() {
        return CommonResultCode.limitedAccess.getCode();
    }

}
