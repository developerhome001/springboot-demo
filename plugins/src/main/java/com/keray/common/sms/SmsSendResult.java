package com.keray.common.sms;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * @author by keray
 * date:2021/4/8 5:44 下午
 */
@Getter
@Setter
@AllArgsConstructor
public class SmsSendResult {
    private String phone;
    private SmsStatus status;
}
