package com.keray.common.gateway.limit;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.keray.common.IUserContext;
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
import lombok.Setter;
import org.apache.http.protocol.UriPatternMatcher;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.HandlerMethod;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

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

    @Getter
    @Setter
    public static class CustomQpsRes {
        private Map<String, LinkedList<QpsData>> value;
        private String key;
        private String namespace;
    }

    /**
     * 返回自定义流控数据  可以根据用户角色来控制流控
     */
    protected CustomQpsRes customQpsResList(NativeWebRequest request, HandlerMethod handler, Map<String, Map<String, LinkedList<QpsData>>> data) {
        return null;
    }

    @Override
    public boolean interceptorConsumer(NativeWebRequest request, HandlerMethod handler, List<QpsData> releaseList) throws InterruptedException, QPSFailException {
        var req = request.getNativeRequest(HttpServletRequest.class);
        if (req == null || "keray".equals(req.getHeader("keray"))) return false;
        var ip = userContext.currentIp();
        var hadWork = false;
        // 自定义流控部分处理
        all:
        {
            var customRes = customQpsResList(request, handler, getQpsConfig().getCustomData());
            if (customRes == null) {
                break all;
            }
            if (MapUtil.isEmpty(customRes.getValue())) {
                hadWork = true;
                break all;
            }
            if (customRes.getKey() == null) throw new RuntimeException("自定义流控key不能为空");
            Function<QpsData, String> nsFunc = value -> customRes.getNamespace() == null ? MD5Util.MD5Encode(req.getRequestURI()) : customRes.getNamespace();
            var urlData = customRes.getValue();
            var list = urlData.get("*");
            hadWork = qps(urlData, list, "CUS_QPS_ALL", v -> customRes.getKey(), nsFunc, releaseList);
            var f = qps(urlData, list, "CUS_QPS", v -> customRes.getKey(), nsFunc, releaseList);
            hadWork = hadWork || f;
        }
        //先对ip的QPS控制  如果非0.0.0.0/0ip  *型URL匹配上后 后面的流控不继续
        all:
        if (!hadWork) {
            var data = getQpsConfig().getData();
            Map<String, LinkedList<QpsData>> ipData = null;
            String ipKey = ip;
            for (var entity : data.entrySet()) {
                if (IpAuthUtil.ipInIps(ip, List.of(entity.getKey()))) {
                    ipData = entity.getValue();
                    ipKey = entity.getKey();
                    break;
                }
            }
            if (ipData == null || requestIgnoreRateLimiter(RateLimiterStep.ip, ipKey, request, handler))
                break all;
            // url通配限制
            var list = ipData.get("*");
            String finalIpKey = ipKey;
            Function<QpsData, String> keyFunc = value -> {
                var key = userContext.loginStatus() ? userContext.currentUserId() : userContext.getDuid();
                if (value.getTarget() == RateLimiterApiTarget.user) {
                    key = ip;
                }
                if (value.getTarget() == RateLimiterApiTarget.ip) {
                    key = finalIpKey;
                }
                return key;
            };
            Function<QpsData, String> nsFunc = value -> MD5Util.MD5Encode(req.getRequestURI());
            var flag = false;
            if (!requestIgnoreRateLimiter(RateLimiterStep.url, "*", request, handler)) {
                flag = qps(ipData, list, "IP_QPS_ALL", keyFunc, nsFunc, releaseList);
            }
            // 指定url的QPS控制
            list = uriVal(ipData, req.getRequestURI());
            if (!requestIgnoreRateLimiter(RateLimiterStep.url, req.getRequestURI(), request, handler)) {
                var f = qps(ipData, list, "IP_QPS", keyFunc, nsFunc, releaseList);
                flag = flag || f;
            }
            // 不是通用ip匹配上的
            if (!"0.0.0.0/0".equals(ipKey)) {
                hadWork = flag;
            }
        }
        // 直接URL流控处理
        if (!hadWork) {
            Function<QpsData, String> keyFunc = value -> {
                var key = userContext.loginStatus() ? userContext.currentUserId() : userContext.getDuid();
                if (value.getTarget() == RateLimiterApiTarget.ip) {
                    key = ip;
                }
                return key;
            };
            Function<QpsData, String> nsFunc = value -> MD5Util.MD5Encode(req.getRequestURI());
            var urlData = getQpsConfig().getUrlData();
            // url通配限制
            var list = urlData.get("*");
            if (!requestIgnoreRateLimiter(RateLimiterStep.url, "*", request, handler)) {
                qps(urlData, list, "URL_QPS_ALL", keyFunc, nsFunc, releaseList);
            }
            // 指定url的QPS控制
            list = uriVal(urlData, req.getRequestURI());
            if (!requestIgnoreRateLimiter(RateLimiterStep.url, req.getRequestURI(), request, handler)) {
                hadWork = qps(urlData, list, "URL_QPS", keyFunc, nsFunc, releaseList);
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

    public boolean qps(Map<String, LinkedList<QpsData>> urlData, LinkedList<QpsData> list, String group, Function<QpsData, String> keyFunc, Function<QpsData, String> nsFunc, List<QpsData> releaseList) throws InterruptedException, QPSFailException {
        var hadWork = false;
        if (list != null) {
            // 基于配置文件的QPS控制已经处理，设置信号让基于注解的处理无效  数组为空表示这个接口放行
            hadWork = true;
            for (var value : list) {
                var rateLimiter = this.getBean(value.getBean());
                var namespace = value.getNamespace() == null ? nsFunc.apply(value) : value.getNamespace();
                var key = keyFunc.apply(value);
                if (value.getTarget() == RateLimiterApiTarget.namespace) {
                    key = namespace;
                    // 空间名配置掉了时返回没有处理过
                    if (StrUtil.isEmpty(key)) return false;
                }
                // 只有当前没设置maxRate时才当做指定namespace的策略限制
                else if (value.getMaxRate() == 0) {
                    var nameList = urlData.get(namespace);
                    if (nameList != null) {
                        return qps(urlData, nameList, group, keyFunc, v -> namespace, releaseList);
                    }
                }
                try {
                    var ns = String.format("%s:%s", group, namespace);
                    var params = value.toParams().setKey(key).setNamespace(ns);
                    rateLimiter.acquire(params);
                    if (value.isNeedRelease()) {
                        var cp = value.copy(key);
                        cp.setNamespace(ns);
                        releaseList.add(cp);
                    }
                } catch (QPSFailException e) {
                    throw new QPSFailException(value.getLimitType() == RateLimitType.system, value.getRejectMessage(), e.getParams());
                }
            }
        }
        return hadWork;
    }

    public static <T> T uriVal(Map<String, T> data, String key) {
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
