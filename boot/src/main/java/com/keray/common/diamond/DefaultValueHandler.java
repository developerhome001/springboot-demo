package com.keray.common.diamond;

import com.alibaba.fastjson.JSON;

public class DefaultValueHandler implements ValueHandler {
    @Override
    public Object decode(String value, Class<?> clazz) {
        if (value == null) return null;
        return JSON.parseObject(value, clazz);
    }

    @Override
    public String encode(Object obj) {
        if (obj == null) return null;
        return JSON.toJSONString(obj);
    }

}
