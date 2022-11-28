package com.keray.common.service.model;

import com.keray.common.entity.IBSEntity;

import java.io.Serializable;

/**
 * @author by keray
 * date:2021/4/29 1:57 下午
 */
public interface NameModel<T extends NameModel<T, ID>, ID extends Serializable> extends IBSEntity<T, ID> {
    String getName();
}
