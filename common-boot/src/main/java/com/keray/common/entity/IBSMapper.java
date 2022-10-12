package com.keray.common.entity;

import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.keray.common.Wrappers;
import com.keray.common.annotation.BaseDbInsert;
import org.apache.ibatis.annotations.Param;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * @author by keray
 * date:2020/7/15 9:39 上午
 */
public interface IBSMapper<T extends IBSEntity<T, ID>, ID extends Serializable> extends IBMapper<T> {


    /**
     * 根据 ID 删除
     *
     * @param id 主键ID
     */
    int deleteById(Serializable id);

    /**
     * 删除（根据ID 批量删除）
     *
     * @param idList 主键ID列表(不能为 null 以及 empty)
     */
    int deleteBatchIds(@Param(Constants.COLLECTION) Collection<?> idList);


    /**
     * 根据 ID 修改
     *
     * @param entity 实体对象
     */
    int updateById(@Param(Constants.ENTITY) T entity);


    /**
     * 根据 ID 查询
     *
     * @param id 主键ID
     */
    T selectById(Serializable id);

    /**
     * 查询（根据ID 批量查询）
     *
     * @param idList 主键ID列表(不能为 null 以及 empty)
     */
    List<T> selectBatchIds(@Param(Constants.COLLECTION) Collection<? extends Serializable> idList);


    /**
     * 插入一条记录
     *
     * @param entity 实体对象
     * @return
     */
    @Override
    @BaseDbInsert
    int insert(T entity);

    @Override
    @BaseDbInsert
    @Transactional(rollbackFor = Exception.class)
    default boolean insertBatch(List<T> entityList) {
        return IBMapper.super.insertBatch(entityList);
    }

    @Override
    @BaseDbInsert
    @Transactional(rollbackFor = Exception.class)
    default boolean insertBatch(List<T> entityList, int batchSize) {
        return IBMapper.super.insertBatch(entityList, batchSize);
    }

    default boolean contains(ID id) {
        return selectCount(Wrappers.<T>query().eq("id", id)) == 1;
    }

    default Boolean canDelete(@Param("ids") List<ID> ids) {
        return true;
    }

}
