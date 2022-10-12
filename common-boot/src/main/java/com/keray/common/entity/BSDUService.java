package com.keray.common.entity;

import java.io.Serializable;

/**
 * @author by keray
 * date:2019/7/25 16:03
 */
public interface BSDUService<T extends IBSDUEntity<T, ID>, ID extends Serializable> extends BSDService<T, ID> {
    /**
     * 获取基础模块操作mapper
     *
     * @return
     */
    @Override
    IBSDUMapper<T, ID> getMapper();


}
