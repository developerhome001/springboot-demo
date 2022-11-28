package com.keray.common.sms;

import com.keray.common.exception.BizException;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author by keray
 * date:2021/4/8 5:21 下午
 */
public interface SmsPlugins {

    /**
     * <p>
     * <h3>作者 keray</h3>
     * <h3>时间： 2021/4/8 5:45 下午</h3>
     * 发送短信验
     * </p>
     *
     * @param phone 手机号
     * @return <p> {@link SmsStatus} </p>
     * @throws
     */
    SmsStatus send(String phone, Map<String, Object> data);

    Future<SmsStatus> sendAsync(String phone, Map<String, Object> data);

    void sendCall(String phone, Map<String, Object> data, SmsSendCallback callback);

    /**
     * <p>
     * <h3>作者 keray</h3>
     * <h3>时间： 2021/4/8 5:46 下午</h3>
     * 批量发送短信
     * </p>
     *
     * @param phoneList 手机号列表
     * @return <p> {@link List<SmsSendResult>} </p>
     * @throws
     */
    List<SmsSendResult> send(List<String> phoneList, Map<String, Object> data);

    List<Future<SmsSendResult>> sendAsync(List<String> phoneList, Map<String, Object> data);

    void sendCall(List<String> phoneList, Map<String, Object> data, SmsSendCallback callback) ;
}
