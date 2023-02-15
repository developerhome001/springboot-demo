package com.keray.common.gateway.limit;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.keray.common.diamond.Diamond;
import lombok.Getter;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

@Configuration(proxyBeanMethods = false)
public class QpsConfig {

    /**
     * ip限制数据
     */
    @Getter
    @Diamond(key = "QpsConfig.data")
    private Map<String/*ip 支持ip表达式 支持单ip ip范围 ip段*/, Map<String/* 请求路径 *匹配所有接口 */, QpsData>> data = new HashMap<>();


    /**
     * url限制数据
     */
    @Getter
    @Diamond(key = "QpsConfig.urlData")
    private Map<String/*url表达式*/, LinkedList<QpsData>> urlData = new HashMap<>();

    public synchronized void setData(String value) {
        if (StrUtil.isEmpty(value)) {
            this.data = new HashMap<>();
            return;
        }
        Map<String, Map<String, QpsData>> data = new HashMap<>();
        var map = JSON.parseObject(value);
        for (var k : map.keySet()) {
            var item = map.getJSONObject(k);
            Map<String, QpsData> m = new HashMap<>();
            for (var k1 : item.keySet()) {
                m.put(k1, item.getObject(k1, QpsData.class));
            }
            data.put(k, m);
        }
        this.data = data;
    }

    public synchronized void setUrlData(String value) {
        if (StrUtil.isEmpty(value)) {
            this.urlData = new HashMap<>();
            return;
        }
        Map<String, LinkedList<QpsData>> data = new HashMap<>();
        var map = JSON.parseObject(value);
        for (var k : map.keySet()) {
            data.put(k, new LinkedList<>(map.getJSONArray(k).toJavaList(QpsData.class)));
        }
        this.urlData = data;

    }
}