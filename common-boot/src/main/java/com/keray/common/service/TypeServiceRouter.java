package com.keray.common.service;

import com.keray.common.SpringContextHolder;
import com.keray.common.entity.BaseService;

/**
 * @author by keray
 * date:2019/10/9 14:38
 */
public class TypeServiceRouter {

    /**
     * <p>
     * <h3>作者 keray</h3>
     * <h3>时间： 2019/10/9 14:56</h3>
     * 获取通用类型 serviceBean
     * </p>
     *
     * @param typeEnum
     * @return <p> {@link BaseService <T>} </p>
     * @throws
     */
    public static <T> T router(TypeEnum<T> typeEnum) {
        return SpringContextHolder.getBean(typeEnum.getServiceBeanClass());
    }

}
