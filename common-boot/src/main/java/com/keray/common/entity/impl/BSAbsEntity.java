package com.keray.common.entity.impl;

import com.keray.common.entity.IBSEntity;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @author by keray
 * date:2020/7/15 9:38 上午
 */
@Getter
@Setter
public abstract class BSAbsEntity<T extends BSAbsEntity<T, ID>, ID extends Serializable> extends BEntity<T> implements IBSEntity<T, ID> {

    public abstract ID getId();

    public abstract void setId(ID id);

    @Override
    public Serializable pkVal() {
        return this.getId();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof BSAbsEntity)) {
            return false;
        } else {
            return getId().equals(((BSAbsEntity) obj).getId());
        }
    }

}
