package com.keray.common.gateway.limit;

import cn.hutool.core.util.StrUtil;
import com.keray.common.IUserContext;
import com.keray.common.annotation.RateLimiterApi;
import com.keray.common.exception.QPSFailException;
import com.keray.common.qps.spring.MemoryRateLimiterBean;
import com.keray.common.qps.spring.RateLimiterBean;
import com.keray.common.util.MoreUriPatternMatcher;
import com.keray.common.utils.IpAuthUtil;
import com.keray.common.utils.MD5Util;
import org.apache.http.protocol.UriPatternMatcher;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.HandlerMethod;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public abstract class AbstractRateLimiterInterceptor implements RateLimiterInterceptor {

    @Resource
    protected IUserContext<?> userContext;

    public abstract QpsConfig getQpsConfig();

    protected String annDataGetKey(RateLimiterApi data) {
        if (data.target() == RateLimiterApiTarget.namespace) {
            return data.namespace();
        } else if (data.target() == RateLimiterApiTarget.ip) {
            return userContext.currentIp();
        } else if (data.target() == RateLimiterApiTarget.user) {
            if (userContext.loginStatus()) return userContext.currentUserId();
            return userContext.getDuid();
        } else {
            return userContext.currentUserId();
        }
    }

    @Override
    public boolean interceptorConsumer(NativeWebRequest request, HandlerMethod handler, Map<String, QpsData> releaseList) throws InterruptedException, QPSFailException {
        var req = request.getNativeRequest(HttpServletRequest.class);
        if (req == null || "keray".equals(req.getHeader("keray"))) return false;
        var ip = userContext.currentIp();
        var hadWork = false;
        //先对ip的QPS控制  如果非0.0.0.0/0ip  *型URL匹配上后 后面的流控不继续
        {
            var data = getQpsConfig().getData();
            Map<String, QpsData> ipData = null;
            String ipKey = ip;
            for (var entity : data.entrySet()) {
                if (IpAuthUtil.ipInIps(ip, List.of(entity.getKey()))) {
                    ipData = entity.getValue();
                    ipKey = entity.getKey();
                    break;
                }
            }
            var flag = clientWord(ipData, req.getRequestURI(), ip, ipKey, "IP_QPS", releaseList);
            // 不是通用ip匹配上的
            if (!"0.0.0.0/0".equals(ipKey)) {
                hadWork = flag;
            }
        }
        if (!hadWork) {
            // url的QPS控制
            var urlData = getQpsConfig().getUrlData();
            hadWork = urlQps(urlData, uriVal(urlData, req.getRequestURI(), false),
                    ip, req, handler, null, false);

        }
        return hadWork;
    }

    @Override
    public void interceptor(RateLimiterApi data, NativeWebRequest request, HandlerMethod handler, Map<String, QpsData> releaseList) throws InterruptedException, QPSFailException {
        var uuid = annDataGetKey(data);
        interceptor(uuid, data, request, handler, releaseList);
    }

    public final void interceptor(String uuid, RateLimiterApi data, NativeWebRequest request, HandlerMethod handler, Map<String, QpsData> releaseList) throws InterruptedException, QPSFailException {
        if (StrUtil.isNotEmpty(uuid)) {
            try {
                this.getBean(data.bean()).acquire(uuid, data.namespace(), data.maxRate(), 1, data.millisecond(), data.appointCron(), data.recoveryCount(),
                        data.rejectStrategy(), data.waitTime(), data.waitSpeed(), data.needRelease());
                if (data.needRelease()) releaseList.put(uuid, QpsData.of(data));
            } catch (QPSFailException e) {
                if (StrUtil.isNotBlank(data.rejectMessage()))
                    throw new QPSFailException(data.limitType() == RateLimitType.system, data.rejectMessage());
                throw new QPSFailException(data.limitType() == RateLimitType.system);
            }
        }
    }

    @Override
    public void release(String key, QpsData qpsData, NativeWebRequest request, HandlerMethod handler) throws InterruptedException {
        if (StrUtil.isNotEmpty(key)) {
            this.getBean(qpsData.getBean()).release(key, qpsData.getNamespace(), qpsData.getMaxRate(), qpsData.getReleaseCnt());
        }
    }

    /**
     * @param clientData
     * @param path
     * @param key
     * @throws QPSFailException
     * @throws InterruptedException
     */
    public boolean clientWord(Map<String, QpsData> clientData, String path, String key, String groupKey, String group, Map<String, QpsData> releaseList) throws QPSFailException, InterruptedException {
        if (clientData == null) return false;
        // 通用接口QPS流控限制
        var value = clientData.get("*");
        if (value != null) {
            RateLimiterBean rateLimiter = this.getBean(value.getBean());
            var k = value.getTarget() == RateLimiterApiTarget.ip ? groupKey : key;
            // 一秒后才会产生最大令牌数的令牌
            try {
                rateLimiter.acquire(k, value.getNamespace(group), value.getMaxRate(), 1, value.getMillisecond(), value.getAppointCron(),
                        value.getRecoveryCount(), value.getRejectStrategy(), value.getWaitTime(), value.getWaitSpeed());
                if (value.isNeedRelease()) {
                    var cp = value.copy();
                    cp.setNamespace(value.getNamespace(group));
                    releaseList.put(k, cp);
                }
            } catch (QPSFailException e) {
                throw new QPSFailException(value.getLimitType() == RateLimitType.system, value.getRejectMessage());
            }
        }
        // 判断客户端当前接口是否达到上限
        value = uriVal(clientData, path, false);
        String namespace = null;
        if (value != null && value.getNamespace() != null) {
            // URI固定分配空间
            namespace = value.getNamespace();
            value = uriVal(clientData, value.getNamespace(), true);
        }
        if (value != null) {
            if (namespace == null) {
                // 每个URI分配一个空间
                namespace = MD5Util.MD5Encode(path);
            }
            RateLimiterBean rateLimiter = this.getBean(value.getBean());
            var k = value.getTarget() == RateLimiterApiTarget.ip ? groupKey : key;
            try {
                rateLimiter.acquire(k, group + namespace, value.getMaxRate(), 1, value.getMillisecond(), value.getAppointCron(),
                        value.getRecoveryCount(), value.getRejectStrategy(), value.getWaitTime(), value.getWaitSpeed());
                if (value.isNeedRelease()) {
                    var cp = value.copy();
                    cp.setNamespace(group + namespace);
                    releaseList.put(k, cp);
                }
                return true;
            } catch (QPSFailException e) {
                throw new QPSFailException(value.getLimitType() == RateLimitType.system, value.getRejectMessage());
            }
        }
        return false;
    }


    protected boolean urlQps(Map<String, LinkedList<QpsData>> urlData,
                             LinkedList<QpsData> list, String ip, HttpServletRequest req, HandlerMethod handler, String namespace, boolean isNamespace) throws InterruptedException, QPSFailException {
        var hadWork = false;
        if (list != null) {
            // 基于配置文件的QPS控制已经处理，设置信号让基于注解的处理无效  数组为空表示这个接口放行
            hadWork = true;
            for (var value : list) {
                RateLimiterBean rateLimiter = this.getBean(value.getBean());
                namespace = namespace == null ? value.getNamespace() : namespace;
                var key = userContext.loginStatus() ? userContext.currentUserId() : userContext.getDuid();
                if (value.getTarget() == RateLimiterApiTarget.ip) {
                    key = ip;
                } else if (value.getTarget() == RateLimiterApiTarget.namespace) {
                    key = isNamespace ? namespace : value.getNamespace();
                    // 空间名配置掉了时返回没有处理过
                    if (StrUtil.isEmpty(key)) return false;
                }
                if (StrUtil.isEmpty(namespace)) {
                    namespace = MD5Util.MD5Encode(req.getRequestURI());
                } else if (!isNamespace) {
                    var nameList = urlData.get(namespace);
                    if (nameList != null) {
                        urlQps(urlData, nameList, ip, req, handler, namespace, true);
                        return true;
                    }
                }
                try {
                    rateLimiter.acquire(key, namespace, value.getMaxRate(), 1, value.getMillisecond(), value.getAppointCron(),
                            value.getRecoveryCount(), value.getRejectStrategy(), value.getWaitTime(), value.getWaitSpeed());
                } catch (QPSFailException e) {
                    throw new QPSFailException(value.getLimitType() == RateLimitType.system, value.getRejectMessage());
                }
            }
        }
        return hadWork;
    }


    /**
     * 非空间名获取时只匹配已/开头的配置
     * @param data
     * @param key
     * @param namespace
     * @param <T>
     * @return
     */
    protected static <T> T uriVal(Map<String, T> data, String key, boolean namespace) {
        if (namespace) return data.get(key);
        var matcher = getMatcher(data);
        return matcher.lookup(key);
    }

    protected static <T> UriPatternMatcher<T> getMatcher(Map<String, T> data) {
        var matcher = new MoreUriPatternMatcher<T>();
        data.forEach((k, v) -> {
            // 过滤掉空间设置
            if (k.startsWith("/")) {
                matcher.register(k, v);
            }
        });
        return matcher;
    }

    protected abstract RateLimiterBean getBean(String name);
}
