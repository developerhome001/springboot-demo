package com.keray.common.entity;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.keray.common.CommonResultCode;
import com.keray.common.Wrappers;
import com.keray.common.exception.BizRuntimeException;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author by keray
 * date:2021/3/25 5:38 下午
 */
public interface BSService<T extends IBSEntity<T, ID>, ID extends Serializable> extends BService<T> {

    default ID generateId(T entity) {
        return null;
    }

    /**
     * 获取基础模块操作mapper
     *
     * @return
     */
    IBSMapper<T, ID> getMapper();


    /**
     * 修改
     *
     * @param entity update实体
     * @return
     */
    default T update(T entity) {
        if (ObjectUtil.isEmpty(entity.getId())) {
            throw new BizRuntimeException("update必须拥有Id", CommonResultCode.dataChangeError.getCode());
        }
        return getMapper().updateById(entity) == 1 ? entity : null;
    }

    /**
     * 基础实体修改
     * 不推荐重写
     *
     * @param entity update实体
     * @return
     */
    default T simpleUpdate(T entity) {
        if (ObjectUtil.isEmpty(entity.getId())) {
            throw new BizRuntimeException("update必须拥有Id", CommonResultCode.dataChangeError.getCode());
        }
        return getMapper().updateById(entity) == 1 ? entity : null;
    }

    /**
     * 删除
     *
     * @param id 实体id
     * @return
     */
    default Boolean delete(ID id) {
        if (!this.canDelete(Collections.singletonList(id))) {
            throw new BizRuntimeException(CommonResultCode.dataNotAllowDelete);
        }
        return getMapper().deleteById(id) == 1;
    }

    /**
     * 批量逻辑删除
     *
     * @param ids 实体id
     * @return
     */
    @Transactional(rollbackFor = RuntimeException.class)
    default Boolean delete(List<ID> ids) {
        if (CollUtil.isEmpty(ids)) {
            throw new BizRuntimeException(CommonResultCode.illegalArgument);
        }
        ids = ids.stream().distinct().collect(Collectors.toList());
        if (!this.canDelete(ids)) {
            throw new BizRuntimeException(CommonResultCode.dataNotAllowDelete);
        }
        if (ids.size() == 1) {
            return getMapper().deleteById(ids.get(0)) == 1;
        }
        if (getMapper().deleteBatchIds(ids) != ids.size()) {
            throw new BizRuntimeException(CommonResultCode.dataChangeError);
        }
        return true;
    }

    /**
     * 通过id查询数据
     *
     * @param id 实体id
     * @return T
     */
    default T getById(ID id) {
        if (ObjectUtil.isEmpty(id)) {
            return null;
        }
        return getMapper().selectById(id);
    }

    default List<T> selectBatchIds(List<ID> idList) {
        return getMapper().selectBatchIds(idList);
    }

    /**
     * <p>
     * <h3>作者 keray</h3>
     * <h3>时间： 2019/9/16 14:14</h3>
     * 是否存在id
     * </p>
     *
     * @param id
     * @return <p> {@link boolean} </p>
     * @throws
     */
    default boolean contains(ID id) {
        return getMapper().contains(id);
    }

    /**
     * <p>
     * <h3>作者 keray</h3>
     * <h3>时间： 2019/11/25 10:24 AM</h3>
     * 实体删除时校验是否能被删除
     * </p>
     *
     * @param keys ids | codes | 其他  子类自行实现
     * @return <p> {@link boolean} </p>
     * @throws
     */
    default Boolean canDelete(List<ID> keys) {
        return getMapper().canDelete(keys);
    }

    @Override
    default T insert(T entity) {
        if (entity.getId() == null) {
            entity.setId(generateId(entity));
        }
        return BService.super.insert(entity);
    }

    @Override
    default boolean insertBatch(List<T> entityList, int batchSize) {
        for (var t : entityList) {
            t.setId(generateId(t));
        }
        return BService.super.insertBatch(entityList, batchSize);
    }

    default boolean insertBatch(List<T> entityList) {
        return this.insertBatch(entityList, entityList.size());
    }
}
