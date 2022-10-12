package com.keray.common.service.service.impl;

import com.keray.common.entity.BSDService;
import com.keray.common.entity.IBSDEntity;

/**
 * @author by keray
 * date:2019/7/25 16:03
 */
public abstract class BSDServiceImpl<T extends IBSDEntity<T, String>> extends BSServiceImpl<T> implements BSDService<T, String> {

}
