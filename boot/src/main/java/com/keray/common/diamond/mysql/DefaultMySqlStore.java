package com.keray.common.diamond.mysql;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusProperties;
import com.keray.common.diamond.Store;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

@MapperScan(basePackages = "com.keray.common.diamond.mysql")
@Configuration
@ConditionalOnClass(MybatisPlusProperties.class)
@Slf4j
public class DefaultMySqlStore implements Store {
    @Resource
    private DiamondStoreMapper repository;

    public DefaultMySqlStore() {
        log.info("diamond MySQL存储器");
    }

    @Override
    public void save(String key, String value) {
        var entity = new DiamondStoreModel();
        entity.setId(key);
        entity.setValue(value);
        if (repository.selectById(key) != null) {
            repository.updateById(entity);
        } else {
            repository.insert(entity);
        }
    }

    @Override
    public String getValue(String key) {
        var entity = repository.selectById(key);
        if (entity == null) return null;
        return entity.getValue();
    }
}
