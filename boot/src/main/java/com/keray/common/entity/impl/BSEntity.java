package com.keray.common.entity.impl;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Getter;
import lombok.Setter;

/**
 * @author by keray
 * date:2020/7/15 9:38 上午
 */
@Getter
@Setter
public class BSEntity<T extends BSEntity<T>> extends BSAbsEntity<T, String> {

    @TableId(type = IdType.INPUT)
    private String id;

}
