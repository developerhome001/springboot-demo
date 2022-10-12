package com.keray.common.entity.impl;

import com.keray.common.entity.IBSDUEntity;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @author by keray
 * date:2019/7/25 15:33
 */
@Getter
@Setter
public abstract class BSDUAbsEntity<T extends BSDUAbsEntity<T, ID>, ID extends Serializable> extends BSDAbsEntity<T, ID> implements IBSDUEntity<T, ID> {

    /**
     * 创建来源
     */
    private String createdBy;

    /**
     * 修改来源
     */
    private String modifyBy;

    public IBSDUEntity<T, ID> clearBaseField() {
        this.setModifyTime(null);
        this.setModifyBy(null);
        this.setCreatedBy(null);
        this.setCreatedTime(null);
        return this;
    }
}
