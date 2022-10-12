package com.keray.common.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;

/**
 * @author by keray
 * date:2019/10/14 14:00
 */
public interface IBSDUEntity<T extends IBSDUEntity<T, ID>, ID extends Serializable> extends IBSDEntity<T, ID> {



    String getCreatedBy();

    void setCreatedBy(String createdBy);

    String getModifyBy();

    void setModifyBy(String modifyBy);

    default IBSDUEntity clearBaseField() {
        return this;
    }
}
