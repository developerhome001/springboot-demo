package com.keray.common.service;

/**
 * @author by keray
 * date:2019/10/9 14:40
 */
public interface TypeEnum<S> {
    Class<? extends S> getServiceBeanClass();
}
