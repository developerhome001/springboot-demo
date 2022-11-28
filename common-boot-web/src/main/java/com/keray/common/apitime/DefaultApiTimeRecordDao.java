package com.keray.common.apitime;

import org.springframework.data.mongodb.core.MongoTemplate;

import javax.annotation.Resource;
import java.io.Serializable;

/**
 * @author by keray
 * date:2019/12/4 3:24 PM
 */

public class DefaultApiTimeRecordDao implements ApiTimeRecordDb {

    @Resource
    private MongoTemplate mongoTemplate;

    @Override
    public ApiTimeRecordData build(ApiTimeRecordData source) {
        return source;
    }

    @Override
    public Serializable insert(Serializable apiTimeRecordData) {
        return mongoTemplate.insert(apiTimeRecordData);
    }
}
