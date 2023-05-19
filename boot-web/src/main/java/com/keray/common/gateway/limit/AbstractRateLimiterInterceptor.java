package com.keray.common.gateway.limit;

import cn.hutool.core.util.StrUtil;
import com.keray.common.IUserContext;
import com.keray.common.annotation.QpsPublicIpIgnore;
import com.keray.common.annotation.QpsPublicUrlIgnore;
import com.keray.common.annotation.RateLimiterApi;
import com.keray.common.exception.QPSFailException;
import com.keray.common.qps.RateLimiterParams;
import com.keray.common.qps.RejectStrategy;
import com.keray.common.qps.spring.RateLimiterBean;
import com.keray.common.service.AiService;
import com.keray.common.util.MoreUriPatternMatcher;
import com.keray.common.utils.IpAuthUtil;
import com.keray.common.utils.MD5Util;
import lombok.Getter;
import org.apache.http.protocol.UriPatternMatcher;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.HandlerMethod;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class AbstractRateLimiterInterceptor implements RateLimiterInterceptor {

    @Resource
    protected IUserContext<?> userContext;

    @Resource
    protected RateLimiterBean redisRateLimiterBean;

    @Resource
    private RateLimiterBean memoryRateLimiterBean;

    @Resource
    @Getter
    private QpsConfig qpsConfig;

    @Resource
    private AiService aiService;

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

    public static RateLimiterParams rateLimiterApi2params(RateLimiterApi api) {
        return new RateLimiterParams()
                .setNamespace(api.namespace())
                .setMaxRate(api.maxRate())
                .setAcquireCount(1)
                .setMillisecond(api.millisecond())
                .setAppointCron(api.appointCron())
                .setRecoveryCount(api.recoveryCount())
                .setRejectStrategy(api.rejectStrategy())
                .setWaitTime(api.waitTime())
                .setWaitSpeed(api.waitSpeed())
                .setNeedRelease(api.needRelease())
                .setReleaseCnt(api.releaseCnt())
                ;
    }

    @Override
    public boolean interceptorConsumer(NativeWebRequest request, HandlerMethod handler, List<QpsData> releaseList) throws InterruptedException, QPSFailException {
        var req = request.getNativeRequest(HttpServletRequest.class);
        if (req == null || "keray".equals(req.getHeader("keray"))) return false;
        var ip = userContext.currentIp();
        var hadWork = false;
        //先对ip的QPS控制  如果非0.0.0.0/0ip  *型URL匹配上后 后面的流控不继续
        all:
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
            if (requestIgnoreRateLimiter(RateLimiterStep.ip, ipKey, request, handler))
                break all;
            var flag = clientWord(ipData, req.getRequestURI(), ip, ipKey, "IP_QPS", releaseList);
            // 不是通用ip匹配上的
            if (!"0.0.0.0/0".equals(ipKey)) {
                hadWork = flag;
            }
        }
        if (!hadWork) {
            var urlData = getQpsConfig().getUrlData();
            // url通配限制
            var value = urlData.get("*");
            if (!requestIgnoreRateLimiter(RateLimiterStep.url, "*", request, handler)) {
                urlQps(urlData, value, ip, req, handler, null, releaseList);
            }
            // 指定url的QPS控制
            value = uriVal(urlData, req.getRequestURI(), false);
            if (!requestIgnoreRateLimiter(RateLimiterStep.url, req.getRequestURI(), request, handler)) {
                hadWork = urlQps(urlData, value, ip, req, handler, null, releaseList);
            }
        }
        return hadWork;
    }

    @Override
    public void interceptor(RateLimiterApi data, NativeWebRequest request, HandlerMethod handler, List<QpsData> releaseList) throws InterruptedException, QPSFailException {
        var uuid = annDataGetKey(data);
        interceptor(uuid, data, request, handler, releaseList);
    }

    public final void interceptor(String uuid, RateLimiterApi data, NativeWebRequest request, HandlerMethod handler, List<QpsData> releaseList) throws InterruptedException, QPSFailException {
        if (StrUtil.isNotEmpty(uuid)) {
            this.getBean(data.bean()).acquire(rateLimiterApi2params(data).setKey(uuid));
            if (data.needRelease()) releaseList.add(QpsData.of(data, uuid));
        }
    }

    @Override
    public void release(String key, QpsData qpsData, NativeWebRequest request, HandlerMethod handler) throws InterruptedException {
        if (StrUtil.isNotEmpty(key)) {
            this.getBean(qpsData.getBean()).release(new RateLimiterParams()
                    .setKey(key)
                    .setNamespace(qpsData.getNamespace())
                    .setMaxRate(qpsData.getMaxRate())
                    .setReleaseCnt(qpsData.getReleaseCnt())
                    .setReleaseVersion(qpsData.getReleaseVersion())
            );
        }
    }


    @Override
    public boolean failCall(HttpServletRequest request, HttpServletResponse response, HandlerMethod handler, QPSFailException e) throws QPSFailException, InterruptedException {
        var code = aiService.aiCodeCheck(request, userContext.currentIp(), userContext.getDuid());
        if (code == null) return false;
        // 限制aiCode的流控 1000毫秒一次
        memoryRateLimiterBean.acquire(new RateLimiterParams()
                .setKey(code)
                .setNamespace("QPS:AI_CODE")
                .setMaxRate(20)
                .setRejectStrategy(RejectStrategy.wait)
        );
        return true;
    }


    /**
     * @param clientData
     * @param path
     * @param key
     * @throws QPSFailException
     * @throws InterruptedException
     */
    public boolean clientWord(Map<String, QpsData> clientData, String path, String key, String groupKey, String group, List<QpsData> releaseList) throws QPSFailException, InterruptedException {
        if (clientData == null) return false;
        // 通用接口QPS流控限制
        var value = clientData.get("*");
        if (value != null) {
            RateLimiterBean rateLimiter = this.getBean(value.getBean());
            var k = value.getTarget() == RateLimiterApiTarget.ip ? groupKey : key;
            // 一秒后才会产生最大令牌数的令牌
            try {
                rateLimiter.acquire(value.toParams().setKey(k).setNamespace(value.getNamespace(group)));
                if (value.isNeedRelease()) {
                    var cp = value.copy(k);
                    cp.setNamespace(value.getNamespace(group));
                    releaseList.add(cp);
                }
            } catch (QPSFailException e) {
                throw new QPSFailException(value.getLimitType() == RateLimitType.system, value.getRejectMessage(), e.getParams());
            }
        }
        // 判断客户端当前接口是否达到上限
        value = uriVal(clientData, path, false);
        String namespace = null;
        if (value != null && value.getNamespace() != null) {
            // URI固定分配空间
            namespace = value.getNamespace();
            var nvalue = uriVal(clientData, value.getNamespace(), true);
            // 如果uri设定的空间名没设置QPS数据 直接使用uri设置的数据
            if (nvalue != null) value = nvalue;
        }
        if (value != null && value.getMaxRate() > 0) {
            if (namespace == null) {
                // 每个URI分配一个空间
                namespace = MD5Util.MD5Encode(path);
            }
            RateLimiterBean rateLimiter = this.getBean(value.getBean());
            var k = value.getTarget() == RateLimiterApiTarget.ip ? groupKey : key;
            try {
                var ns = String.format("%s:%s", group, namespace);
                rateLimiter.acquire(value.toParams().setKey(k).setNamespace(ns));
                if (value.isNeedRelease()) {
                    var cp = value.copy(k);
                    cp.setNamespace(ns);
                    releaseList.add(cp);
                }
            } catch (QPSFailException e) {
                throw new QPSFailException(value.getLimitType() == RateLimitType.system, value.getRejectMessage(), e.getParams());
            }
        }
        return true;
    }


    protected boolean urlQps(Map<String, LinkedList<QpsData>> urlData,
                             LinkedList<QpsData> list, String ip, HttpServletRequest req, HandlerMethod handler, String namespace, List<QpsData> releaseList) throws InterruptedException, QPSFailException {
        var hadWork = false;
        if (list != null) {
            // 基于配置文件的QPS控制已经处理，设置信号让基于注解的处理无效  数组为空表示这个接口放行
            hadWork = true;
            for (var value : list) {
                RateLimiterBean rateLimiter = this.getBean(value.getBean());
                namespace = value.getNamespace() == null ? namespace : value.getNamespace();
                var key = userContext.loginStatus() ? userContext.currentUserId() : userContext.getDuid();
                if (value.getTarget() == RateLimiterApiTarget.ip) {
                    key = ip;
                } else if (value.getTarget() == RateLimiterApiTarget.namespace) {
                    key = namespace;
                    // 空间名配置掉了时返回没有处理过
                    if (StrUtil.isEmpty(key)) return false;
                }
                if (StrUtil.isEmpty(namespace)) {
                    namespace = MD5Util.MD5Encode(req.getRequestURI());
                }
                // 只有当前没设置maxRate时才当做指定namespace的策略限制
                else if (value.getMaxRate() == 0) {
                    var nameList = urlData.get(namespace);
                    if (nameList != null) {
                        return urlQps(urlData, nameList, ip, req, handler, namespace, releaseList);
                    }
                }
                try {
                    namespace = "URL_QPS:" + namespace;
                    var params = value.toParams().setKey(key).setNamespace(namespace);
                    rateLimiter.acquire(params);
                    if (value.isNeedRelease()) {
                        var cp = value.copy(key);
                        cp.setNamespace(namespace);
                        releaseList.add(cp);
                    }
                } catch (QPSFailException e) {
                    throw new QPSFailException(value.getLimitType() == RateLimitType.system, value.getRejectMessage(), e.getParams());
                }
            }
        }
        return hadWork;
    }


    /**
     * 非空间名获取时只匹配已/开头的配置
     *
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


    protected RateLimiterBean getBean(String name) {
        if ("redisRateLimiterBean".equals(name)) return redisRateLimiterBean;
        if ("redis".equals(name)) return redisRateLimiterBean;
        return memoryRateLimiterBean;
    }
}
