package com.keray.common.diamond;

public interface ValueHandler {
    Object decode(String value, Class<?> clazz);

    String encode(Object obj);
}
