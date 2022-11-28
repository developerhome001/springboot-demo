package com.keray.common.service.service.impl;

import com.keray.common.entity.BaseService;
import com.keray.common.entity.IBaseEntity;

/**
 * @author by keray
 * date:2020/5/30 4:55 下午
 */
public abstract class BaseServiceImpl<T extends IBaseEntity<T, String>> extends BSDUServiceImpl<T> implements BaseService<T, String> {

}
