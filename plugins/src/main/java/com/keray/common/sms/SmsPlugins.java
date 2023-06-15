package com.keray.common.sms;

import com.keray.common.threadpool.SysThreadPool;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;

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
    default SmsStatus send(String phone, Map<String, Object> data) {
        return send(List.of(phone), data).get(0).getStatus();
    }


    default void sendCall(String phone, Map<String, Object> data, BiConsumer<SmsStatus, String> callback) {
        sendCall(List.of(phone), data, callback);
    }

    default Future<SmsStatus> sendAsync(String phone, Map<String, Object> data) {
        return SysThreadPool.submit(() -> sendAsync(List.of(phone), data).get(0).get().getStatus());
    }

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
    default List<SmsSendResult> send(List<String> phoneList, Map<String, Object> data) {
        var result = sendAsync(phoneList, data);
        var res = new LinkedList<SmsSendResult>();
        for (var i = 0; i < result.size(); i++) {
            var f = result.get(i);
            try {
                res.add(f.get());
            } catch (InterruptedException | ExecutionException e) {
                res.add(new SmsSendResult(phoneList.get(i), SmsStatus.fail));
            }
        }
        return res;
    }

    default void sendCall(List<String> phoneList, Map<String, Object> data, BiConsumer<SmsStatus, String> callback) {
        var result = sendAsync(phoneList, data);
        for (var i = 0; i < phoneList.size(); i++) {
            var f = result.get(i);
            try {
                var fu = f.get();
                callback.accept(fu.getStatus(), fu.getPhone());
            } catch (InterruptedException | ExecutionException e) {
                callback.accept(SmsStatus.fail, phoneList.get(i));
            }
        }
    }

    List<Future<SmsSendResult>> sendAsync(List<String> phoneList, Map<String, Object> data);
}
