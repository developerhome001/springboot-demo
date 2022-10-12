package com.keray.common.entity;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.keray.common.annotation.BaseDbUpdateModel;
import com.keray.common.annotation.BaseDbUpdateWrapper;
import org.apache.ibatis.annotations.Param;

import java.io.Serializable;

/**
 * @author by keray
 * date:2020/7/15 9:39 上午
 */
public interface IBSDMapper<T extends IBSDEntity<T, ID>, ID extends Serializable> extends IBSMapper<T,ID> {
    /**
     * 根据 ID 修改
     *
     * @param entity 实体对象
     * @return
     */
    @Override
    @BaseDbUpdateModel
    int updateById(@Param(Constants.ENTITY) T entity);

    /**
     * 根据 whereEntity 条件，更新记录
     *
     * @param entity        实体对象 (set 条件值,可以为 null)
     * @param updateWrapper 实体对象封装操作类（可以为 null,里面的 entity 用于生成 where 语句）
     * @return
     */
    @Override
    @BaseDbUpdateWrapper
    int update(@Param(Constants.ENTITY) T entity, @Param(Constants.WRAPPER) Wrapper<T> updateWrapper);

}
