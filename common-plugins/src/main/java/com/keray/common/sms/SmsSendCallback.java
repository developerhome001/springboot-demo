package com.keray.common.sms;

/**
 * @author by keray
 * date:2021/4/8 5:41 下午
 */
public interface SmsSendCallback {
    void applay(SmsStatus status, String phone);
}
