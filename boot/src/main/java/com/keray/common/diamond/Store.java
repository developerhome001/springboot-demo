package com.keray.common.diamond;

public interface Store {

    void save(String key, String value);

    String getValue(String key);

}
