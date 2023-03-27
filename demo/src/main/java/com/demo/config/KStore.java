package com.demo.config;

import com.keray.common.diamond.Store;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@Primary
public class KStore implements Store {
    @Override
    public void save(String key, String value) {

    }

    @Override
    public String getValue(String key) {
        return null;
    }
}
