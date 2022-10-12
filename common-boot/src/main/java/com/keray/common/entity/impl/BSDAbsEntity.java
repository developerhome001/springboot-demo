package com.keray.common.entity.impl;

import com.keray.common.entity.IBSDEntity;
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
public abstract class BSDAbsEntity<T extends BSDAbsEntity<T, ID>, ID extends Serializable> extends BSAbsEntity<T, ID> implements IBSDEntity<T, ID> {

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 修改时间
     */
    private LocalDateTime modifyTime;


}
