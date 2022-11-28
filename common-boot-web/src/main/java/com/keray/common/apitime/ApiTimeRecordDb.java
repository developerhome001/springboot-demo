package com.keray.common.apitime;

import java.io.Serializable;

/**
 * @author by keray
 * date:2019/12/4 3:24 PM
 */
public interface ApiTimeRecordDb {
    Serializable build(ApiTimeRecordData source);
    Serializable insert(Serializable t);
}
