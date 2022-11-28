package com.keray.common.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author by keray
 * date:2019/10/14 14:00
 */
public interface IBaseEntity<T extends IBaseEntity<T, ID>, ID extends Serializable> extends IBSDUEntity<T, ID> {

    @JsonIgnore
    Boolean getDeleted();

    void setDeleted(Boolean deleted);

    @JsonIgnore
    LocalDateTime getDeleteTime();

    void setDeleteTime(LocalDateTime deleteTime);

    default IBaseEntity clearBaseField() {
        return this;
    }
}
