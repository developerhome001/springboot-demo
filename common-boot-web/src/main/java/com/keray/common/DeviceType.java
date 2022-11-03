package com.keray.common;

import lombok.Getter;

/**
 * @author by keray
 * date:2021/4/6 10:25 上午
 */
public enum DeviceType {
    //
    unknown("未知", 0),
    android("安卓", 1),
    ios("苹果", 2),
    pc("网页", 3),
    h5("手机网页", 4),
    wxcx("微信小程序", 5),
    admin("管理端", 6),
    ;


    @Getter
    String desc;

    @Getter
    Integer code;

    DeviceType(String desc, Integer code) {
        this.desc = desc;
        this.code = code;
    }
}
