package com.keray.common.entity;

import java.io.Serializable;

/**
 * @author by keray
 * date:2019/7/25 16:03
 */
public interface BSDService<T extends IBSDEntity<T, ID>, ID extends Serializable> extends BSService<T, ID> {
    /**
     * 获取基础模块操作mapper
     *
     * @return
     */
    IBSDMapper<T, ID> getMapper();


}
