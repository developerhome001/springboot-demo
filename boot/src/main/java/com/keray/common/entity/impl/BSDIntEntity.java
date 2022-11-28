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
public class BSDIntEntity<T extends BSDIntEntity<T>> extends BSDAbsEntity<T, Long>   {
    /**
     * 主键id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

}
