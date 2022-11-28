package com.keray.common.entity;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.keray.common.mysql.LogicalDataMapper;
import com.keray.common.Wrappers;
import com.keray.common.mysql.BaseDbUpdateWrapper;
import org.apache.ibatis.annotations.Param;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;

/**
 * @author by keray
 * date:2019/7/25 16:56
 */
public interface IBaseMapper<T extends IBaseEntity<T, ID>, ID extends Serializable> extends IBSDUMapper<T, ID>, LogicalDataMapper<T, ID> {


    /**
     * 删除（根据ID 批量删除）
     *
     * @param id 主键ID列表(不能为 null 以及 empty)
     */
    @Override
    default int deleteById(Serializable id) {
        return this.delete(Wrappers.<T>update().eq("id", id));
    }

    /**
     * 删除（根据ID 批量删除）
     *
     * @param idList 主键ID列表(不能为 null 以及 empty)
     */
    @Override
    default int deleteBatchIds(@Param(Constants.COLLECTION) Collection<?> idList) {
        return this.delete(Wrappers.<T>update().in("id", idList));
    }


    @Deprecated
    @Override
    default int delete(Wrapper<T> wrapper) {
        throw new IllegalArgumentException("delete只允许传入LambdaUpdateWrapper类型 now= " + wrapper.getClass());
    }

    @BaseDbUpdateWrapper
    default int delete(UpdateWrapper<T> wrapper) {
        wrapper.set("deleted", true).set("delete_time", LocalDateTime.now());
        return this.update(null, wrapper);
    }

    @BaseDbUpdateWrapper
    default int delete(LambdaUpdateWrapper<T> wrapper) {
        wrapper.set(T::getDeleted, true).set(T::getDeleteTime, LocalDateTime.now());
        return this.update(null, wrapper);
    }


    /**
     * 根据 columnMap 条件，删除记录
     *
     * @param columnMap 表字段 map 对象
     */
    @Override
    default int deleteByMap(@Param(Constants.COLUMN_MAP) Map<String, Object> columnMap) {
        throw new RuntimeException("不支持");
    }

    /**
     * 根据 ID 删除
     *
     * @param id 主键ID
     */
    int physicsDeleteById(Serializable id);

    /**
     * 删除（根据ID 批量删除）
     *
     * @param idList 主键ID列表(不能为 null 以及 empty)
     */
    int physicsDeleteBatchIds(@Param(Constants.COLLECTION) Collection<? extends Serializable> idList);


    /**
     * 根据 columnMap 条件，删除记录
     *
     * @param columnMap 表字段 map 对象
     */
    int physicsDeleteByMap(@Param(Constants.COLUMN_MAP) Map<String, Object> columnMap);

    /**
     * 根据 entity 条件，删除记录
     *
     * @param queryWrapper 实体对象封装操作类（可以为 null,里面的 entity 用于生成 where 语句）
     */
    int physicsDelete(@Param(Constants.WRAPPER) Wrapper<T> queryWrapper);

}
