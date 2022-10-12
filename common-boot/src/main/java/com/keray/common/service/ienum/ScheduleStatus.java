package com.keray.common.service.ienum;

import com.keray.common.MybatisPlusEnum;
import lombok.Getter;

/**
 * @author by keray
 * date:2020/4/26 12:09 下午
 */
public enum ScheduleStatus implements MybatisPlusEnum<String> {
    //
    waitSubmit("等待提交"),
    waitExec("等待执行"),
    exec("执行中"),
    success("成功"),
    fail("失败"),
    waitRetry("等待重试"),
    cancel("取消")
    ;
    @Getter
    final String desc;

    @Override
    public String getCode() {
        return this.name();
    }

    ScheduleStatus(String desc) {
        this.desc = desc;
    }

}
