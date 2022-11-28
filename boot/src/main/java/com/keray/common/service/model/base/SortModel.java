package com.keray.common.service.model.base;

import com.keray.common.entity.IBEntity;

/**
 * @author by keray
 * date:2020/1/9 9:54 PM
 */
public interface SortModel<T extends IBEntity<T>> extends IBEntity<T> {
    Integer getSort();
}
