package com.keray.common;

import cn.hutool.core.map.MapUtil;
import com.keray.common.context.ThreadCacheContext;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * @author by keray
 * date:2019/7/26 14:21
 */
public interface IUserContext<T> extends IContext, ThreadCacheContext {


    HttpServletRequest currentRequest();

    void setCurrentRequest(HttpServletRequest request);


    T currentTokenData();

    void setTokenData(T tokenData);

    String getDuid();

    void setDUid(String duid);

    default Map<String, Object> export() {
        Map<String, Object> data = IContext.super.export();
        data.putAll(MapUtil.<String, Object>builder()
                .put("tokenData", currentTokenData())
                .put("request", currentRequest())
                .build());
        return data;
    }

    default void importConf(Map<String, Object> map) {
        if (MapUtil.isEmpty(map)) {
            return;
        }
        IContext.super.importConf(map);
        setTokenData((T) map.get("tokenData"));
        setCurrentRequest((HttpServletRequest) map.get("request"));
    }

    default void clear() {
        IContext.super.clear();
        setTokenData(null);
        setCurrentRequest(null);
    }

}
