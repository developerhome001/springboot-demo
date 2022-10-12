package com.keray.common.service.service.impl;

import com.keray.common.entity.BSService;
import com.keray.common.entity.IBSEntity;

/**
 * @author by keray
 * date:2019/7/25 16:03
 */
public abstract class BSServiceImpl<T extends IBSEntity<T, String>> extends BServiceImpl<T> implements BSService<T, String> {

}
