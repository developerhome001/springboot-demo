package com.keray.common.service.service;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.keray.common.CommonResultCode;
import com.keray.common.entity.BSService;
import com.keray.common.entity.IBSEntity;
import com.keray.common.entity.IBSMapper;
import com.keray.common.exception.BizRuntimeException;
import com.keray.common.service.model.RouterServiceModel;
import org.apache.ibatis.annotations.Param;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author by keray
 * date:2019/12/6 2:56 PM
 */
public interface SuperRouterDataService<T extends IBSEntity<T, ID> & RouterServiceModel, P extends IBSEntity<?, ID>, ID extends Serializable> extends BSService<T, ID> {

    IBSMapper<? extends P, ID> routerMapper(T superModel);

    P routerEntity(T superModel);

    T routerEntityTrans(P entity);

    @Deprecated
    default IBSMapper<T, ID> getMapper() {
        return (IBSMapper<T, ID>) routerMapper(null);
    }

    default IBSMapper<? extends P, ID> _m(IBSMapper<? extends P, ID> m) {
        return m;
    }


    default boolean insertBatch(List<T> entityList, int batchSize) {
        IBSMapper m = _m(routerMapper(entityList.get(0)));
        List<P> list = entityList.stream().map(this::routerEntity).collect(Collectors.toList());
        return m.insertBatch(list, batchSize);
    }

    @Override
    default boolean insertBatch(List<T> entityList) {
        return this.insertBatch(entityList, entityList.size());
    }

    default boolean update(T entity, Wrapper<T> updateWrapper) {
        if (entity == null) {
            throw new BizRuntimeException("路由类型实体更新entity不能为空", CommonResultCode.dataChangeError.getCode());
        }
        if (updateWrapper == null) {
            throw new BizRuntimeException("update必须拥有有查询条件", CommonResultCode.dataChangeError.getCode());
        }
        IBSMapper m = _m(routerMapper(entity));
        return m.update(null, updateWrapper) == 1;
    }


    @Deprecated
    default int delete(Wrapper<T> queryWrapper) {
        throw new RuntimeException("not support");
    }

    @Deprecated
    default IPage<T> page(Page<T> pager) {
        throw new RuntimeException("not support");
    }

    @Deprecated
    default IPage<T> page(Page<T> pager, @Param(Constants.WRAPPER) Wrapper<T> queryWrapper) {
        throw new RuntimeException("not support");
    }


    @Deprecated
    default List<T> selectList(@Param(Constants.WRAPPER) Wrapper<T> queryWrapper) {
        throw new RuntimeException("not support");
    }


    @Deprecated
    default T selectOne(@Param(Constants.WRAPPER) Wrapper<T> queryWrapper) {
        throw new RuntimeException("not support");
    }

    @Deprecated
    default Long selectCount(@Param(Constants.WRAPPER) Wrapper<T> queryWrapper) {
        throw new RuntimeException("not support");
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
        IBSMapper m = _m(routerMapper(entity));
        return m.updateById(routerEntity(entity)) == 1 ? entity : null;
    }

    @Deprecated
    default Boolean delete(ID id) {
        throw new RuntimeException("not support");
    }

    @Deprecated
    @Transactional(rollbackFor = RuntimeException.class)
    default Boolean delete(List<ID> ids) {
        throw new RuntimeException("not support");
    }

    @Deprecated
    default T getById(ID id) {
        throw new RuntimeException("not support");
    }


    @Deprecated
    default boolean contains(ID id) {
        throw new RuntimeException("not support");
    }

    @Deprecated
    default Boolean canDelete(List<ID> keys) {
        throw new RuntimeException("not support");
    }


    /**
     * 添加
     *
     * @param entity insert实体
     * @return
     */
    default T insert(T entity) {
        IBSMapper m = _m(routerMapper(entity));
        return m.insert(routerEntity(entity)) == 1 ? entity : null;
    }

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
        IBSMapper m = _m(routerMapper(entity));
        return m.updateById(routerEntity(entity)) == 1 ? entity : null;
    }

    /**
     * 删除
     *
     * @param id 实体id
     * @return
     */
    default Boolean delete(T superModel, String id) {
        if (!this.canDelete(superModel, Collections.singletonList(id))) {
            throw new BizRuntimeException(CommonResultCode.dataNotAllowDelete);
        }
        return routerMapper(superModel).deleteById(id) == 1;
    }

    /**
     * 批量逻辑删除
     *
     * @param ids 实体id
     * @return
     */
    @Transactional(rollbackFor = RuntimeException.class)
    default Boolean delete(T superModel, Collection<? extends Serializable> ids) {
        if (!this.canDelete(superModel, ids)) {
            throw new BizRuntimeException(CommonResultCode.dataNotAllowDelete);
        }
        if (routerMapper(superModel).deleteBatchIds(ids) != ids.size()) {
            throw new BizRuntimeException(CommonResultCode.dataNotAllowDelete);
        }
        return true;
    }

    /**
     * 通过id查询数据
     *
     * @param id 实体id
     * @return T
     */
    default T getById(T superModel, String id) {
        return routerEntityTrans((P) routerMapper(superModel).selectById(id));
    }

    /**
     * 分页查询
     *
     * @param pager 分页参数
     * @return 分页数据
     */
    default IPage<T> page(T superModel, Page<T> pager) {
        IBSMapper m = _m(routerMapper(superModel));
        return m.selectPage(pager, null).convert(v -> routerEntityTrans((P) v));
    }


    /**
     * 分页查询
     *
     * @param pager        分页参数
     * @param queryWrapper
     * @return 分页数据
     */
    default IPage<T> page(T superModel, Page<T> pager, @Param(Constants.WRAPPER) Wrapper<T> queryWrapper) {
        IBSMapper m = _m(routerMapper(superModel));
        return m.selectPage(pager, queryWrapper).convert(v -> routerEntityTrans((P) v));
    }


    /**
     * 根据 entity 条件，查询全部记录
     *
     * @param queryWrapper 实体对象封装操作类（可以为 null）
     * @return
     */
    default List<T> selectList(T superModel, @Param(Constants.WRAPPER) Wrapper<T> queryWrapper) {
        IBSMapper m = _m(routerMapper(superModel));
        List<P> r = m.selectList(queryWrapper);
        return r.stream()
                .map(this::routerEntityTrans)
                .collect(Collectors.toList());
    }

    /**
     * 根据 entity 条件，查询全部记录
     *
     * @param queryWrapper 实体对象封装操作类（可以为 null）
     * @return
     */
    default List<T> selectList(T superModel, @Param(Constants.WRAPPER) Wrapper<T> queryWrapper, int size) {
        IBSMapper m = _m(routerMapper(superModel));
        List<P> r = m.selectList(queryWrapper, size);
        return r.stream()
                .map(this::routerEntityTrans)
                .collect(Collectors.toList());
    }


    /**
     * 根据 entity 条件，查询一条记录
     *
     * @param queryWrapper 实体对象封装操作类（可以为 null）
     * @return
     */
    default T selectOne(T superModel, @Param(Constants.WRAPPER) Wrapper<T> queryWrapper) {
        IBSMapper m = _m(routerMapper(superModel));
        return routerEntityTrans((P) m.selectOne(queryWrapper));
    }


    default T selectFirst(T superModel, @Param(Constants.WRAPPER) Wrapper<T> queryWrapper) {
        IBSMapper m = _m(routerMapper(superModel));
        return routerEntityTrans((P) m.selectFirst(queryWrapper));
    }

    /**
     * 根据 Wrapper 条件，查询总记录数
     *
     * @param queryWrapper 实体对象封装操作类（可以为 null）
     */
    default Long selectCount(T superModel, @Param(Constants.WRAPPER) Wrapper<T> queryWrapper) {
        IBSMapper m = _m(routerMapper(superModel));
        return m.selectCount(queryWrapper);
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
    default boolean contains(T superModel, ID id) {
        return routerMapper(superModel).contains(id);
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
    default Boolean canDelete(T superModel, Collection<? extends Serializable> keys) {
        return true;
    }
}
