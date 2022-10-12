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
public class BSIntEntity<T extends BSIntEntity<T>> extends BSAbsEntity<T, Long> {

    @TableId(type = IdType.AUTO)
    private Long id;

}
