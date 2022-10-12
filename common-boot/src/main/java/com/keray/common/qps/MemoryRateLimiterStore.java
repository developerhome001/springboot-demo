package com.keray.common.qps;

import java.util.HashMap;
import java.util.Map;

public class MemoryRateLimiterStore implements RateLimiterStore {

    private final Map<String, String> store = new HashMap<>();

    @Override
    public String getStoreData(String key) {
        return store.get(key);
    }

    @Override
    public void setStoreData(String key, String data) {
        store.put(key, data);
    }
}
