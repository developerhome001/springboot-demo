package com.keray.common.mysql;

import com.keray.common.entity.IBSEntity;
import com.keray.common.entity.IBSMapper;

import java.io.Serializable;

/**
 * @author by keray
 * date:2021/4/17 3:28 下午
 */
public interface LogicalDataMapper<T extends IBSEntity<T, ID>, ID extends Serializable> extends IBSMapper<T, ID> {
}
