package com.keray.common.entity.impl;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Getter;
import lombok.Setter;

/**
 * @author by keray
 * date:2019/7/25 15:33
 */
@Getter
@Setter
public class BaseEntity<T extends BaseEntity<T>> extends BaseAbsEntity<T, String> {
    /**
     * 主键id
     */
    @TableId(type = IdType.INPUT)
    private String id;

}