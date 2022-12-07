package com.keray.common.gateway.records;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class RecordsContext {

    /**
     * 请求头
     */
    private Map<String, String> header;

    /**
     * cookie
     */
    private Map<String, List<String>> cookie;

    /**
     * 路径
     */
    private String uri;

    /**
     * 请求ip
     */
    private String ip;

    /**
     * 调用参数
     */
    private Object[] args;

    /**
     * 接口返回值
     */
    private Object result;

    /**
     * 接口开始时间
     */
    private long startTime;

    /**
     * 接口结束时间
     */
    private long endTime;

    private final Map<String, Object> other = new HashMap<>(4);

}
