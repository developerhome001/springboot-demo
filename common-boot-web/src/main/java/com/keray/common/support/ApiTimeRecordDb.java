package com.keray.common.support;

import com.keray.common.support.time.model.ApiTimeRecordData;

import java.io.Serializable;

/**
 * @author by keray
 * date:2019/12/4 3:24 PM
 */
public interface ApiTimeRecordDb {
    Serializable build(ApiTimeRecordData source);
    Serializable insert(Serializable t);
}
