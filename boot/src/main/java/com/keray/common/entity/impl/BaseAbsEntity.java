package com.keray.common.entity.impl;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.keray.common.entity.IBaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author by keray
 * date:2019/7/25 15:33
 */
@Getter
@Setter
public abstract class BaseAbsEntity<T extends BaseAbsEntity<T, ID>, ID extends Serializable> extends BSDUAbsEntity<T, ID> implements IBaseEntity<T, ID> {

    /**
     * 是否删除
     */
    @TableLogic(delval = "1", value = "0")
    private Boolean deleted = false;

    /**
     * 删除时间
     */
    private LocalDateTime deleteTime;


    public IBaseEntity<T, ID> clearBaseField() {
        this.setDeleted(null);
        this.setDeleteTime(null);
        this.setModifyTime(null);
        this.setModifyBy(null);
        this.setCreatedBy(null);
        this.setCreatedTime(null);
        return this;
    }
}
