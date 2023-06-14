package com.keray.common.gateway.limit;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.keray.common.diamond.Diamond;
import lombok.Getter;
import org.springframework.context.annotation.Configuration;

import java.util.*;

@Configuration(proxyBeanMethods = false)
public class QpsConfig {

    /**
     * ip限制数据
     */
    @Getter
    @Diamond("QpsConfig.data")
    private Map<String/*ip 支持ip表达式 支持单ip ip范围 ip段*/, Map<String/* 请求路径 *匹配所有接口 */, LinkedList<QpsData>>> data = new HashMap<>();


    /**
     * 自定义限制数据
     */
    @Getter
    @Diamond("QpsConfig.custom")
    private Map<String/*自定义key*/, Map<String/* 请求路径 *匹配所有接口 */, LinkedList<QpsData>>> customData = new HashMap<>();


    /**
     * url限制数据
     */
    @Getter
    @Diamond("QpsConfig.urlData")
    private Map<String/*url表达式*/, LinkedList<QpsData>> urlData = new HashMap<>();

    public synchronized void setData(String value) {
        this.data = value1(value);
    }

    public synchronized void setUrlData(String value) {
        this.urlData = value(value);
    }

    public synchronized void setCustomData(String value) {
        this.customData = value1(value);
    }

    private Map<String, LinkedList<QpsData>> value(String value) {
        if (StrUtil.isEmpty(value)) {
            return new HashMap<>();
        }
        Map<String, LinkedList<QpsData>> data = new HashMap<>();
        var map = JSON.parseObject(value);
        for (var k : map.keySet()) {
            data.put(k, new LinkedList<>(map.getJSONArray(k).toJavaList(QpsData.class)));
        }
        return data;
    }

    private Map<String, Map<String, LinkedList<QpsData>>> value1(String value) {

        if (StrUtil.isEmpty(value)) {
            return new LinkedHashMap<>();
        }
        Map<String, Map<String, LinkedList<QpsData>>> data = new LinkedHashMap<>();
        var map = JSON.parseObject(value, LinkedHashMap.class);
        for (var k : map.keySet()) {
            var item = (JSONObject) map.get(k);
            Map<String, LinkedList<QpsData>> m = new LinkedHashMap<>();
            for (var k1 : item.keySet()) {
                var obj = item.get(k1);
                if (obj instanceof Collection) {
                    var list = item.getJSONArray(k1).toJavaList(QpsData.class);
                    m.put(k1, new LinkedList<>(list));
                } else {
                    LinkedList<QpsData> list = new LinkedList<>();
                    list.add(item.getObject(k1, QpsData.class));
                    m.put(k1, list);
                }
            }
            data.put(k.toString(), m);
        }
        return data;
    }
}
