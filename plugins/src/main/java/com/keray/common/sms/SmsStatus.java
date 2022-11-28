package com.keray.common.sms;

/**
 * @author by keray
 * date:2021/4/8 5:38 下午
 * 短信发送状态
 */
public enum SmsStatus {
    //    发送中
    sending,
    //    发送成功
    sendSuccess,
    //    发送失败
    fail,
    //    发送取消
    cancel
}
