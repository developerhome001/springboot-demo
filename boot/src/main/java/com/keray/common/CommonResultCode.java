package com.keray.common;

import lombok.Getter;

/**
 * @author by keray
 * date:2019/8/1 11:54
 * 返回code
 */
public enum CommonResultCode implements ResultCode {
    //全局异常
    unknown(Integer.parseInt(System.getProperty("RESULT_ERROR", "-1")), "未知错误，请重试"),
    settingError(2, "后端指定异常，前端直接提示"),
    ok(Integer.parseInt(System.getProperty("RESULT_OK", "1")), "OK"),
    illegalArgument(10001, "数据输入错误，请重新输入"),
    dataChangeError(10002, "数据提交失败，请重新输入"),
    limitedAccess(10003, "您的访问过于频繁，请稍后再试"),
    argumentNotPresent(10004, "数据输入错误，请重新输入"),
    dataNotAllowDelete(10005, "删除失败，请检查数据"),
    ;


    @Getter
    private final int code;
    @Getter
    private final String message;

    CommonResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
