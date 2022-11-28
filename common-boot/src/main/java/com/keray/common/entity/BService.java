package com.keray.common.entity;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.keray.common.CommonResultCode;
import com.keray.common.mysql.KerayLambdaQueryWrapper;
import com.keray.common.SpringContextHolder;
import com.keray.common.Wrappers;
import com.keray.common.exception.BizRuntimeException;
import com.keray.common.service.service.SortService;
import org.apache.ibatis.annotations.Param;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author by keray
 * date:2021/3/25 5:38 下午
 */
public interface BService<T extends IBEntity<T>> {


    /**
     * 获取基础模块操作mapper
     *
     * @return
     */
    IBMapper<T> getMapper();

    default <S> S owner(Class<S> clazz) {
        return SpringContextHolder.getBean(clazz);
    }

    default KerayLambdaQueryWrapper<T> wrappers() {
        return Wrappers.lambdaQuery();
    }

    /**
     * 添加
     *
     * @param entity insert实体
     * @return
     */
    default T insert(T entity) {
        return getMapper().insert(entity) == 1 ? entity : null;
    }

    /**
     * <p>
     * <h3>作者 keray</h3>
     * <h3>时间： 2019/10/24 17:43</h3>
     * 查询sum
     * </p>
     *
     * @param wrapper
     * @return <p> {@link String} </p>
     * @throws
     */
    default Double selectSum(Wrapper<T> wrapper) {
        var res = getMapper().selectSum(wrapper);
        return res == null ? 0 : res;
    }

    /**
     * <p>
     * 插入（批量）
     * </p>
     *
     * @param entityList 实体对象列表
     * @param batchSize  插入批次数量
     * @return boolean
     */
    default boolean insertBatch(List<T> entityList, int batchSize) {
        return getMapper().insertBatch(entityList, batchSize);
    }
    default boolean insertBatch(List<T> entityList) {
        return getMapper().insertBatch(entityList, entityList.size());
    }



    default boolean update(T entity, Wrapper<T> updateWrapper) {
        if (updateWrapper == null) {
            throw new BizRuntimeException("update必须拥有有查询条件", CommonResultCode.dataChangeError.getCode());
        }
        return getMapper().update(entity, updateWrapper) == 1;
    }

    default Integer batchUpdate(T entity, Wrapper<T> updateWrapper) {
        if (updateWrapper == null) {
            throw new BizRuntimeException("update必须拥有有查询条件", CommonResultCode.dataChangeError.getCode());
        }
        return getMapper().update(entity, updateWrapper);
    }


    default int delete(Wrapper<T> queryWrapper) {
        if (queryWrapper == null) {
            throw new BizRuntimeException("删除条件不能为null", CommonResultCode.dataChangeError.getCode());
        }
        return getMapper().delete(queryWrapper);
    }


    /**
     * 分页查询
     *
     * @param pager 分页参数
     * @return 分页数据
     */
    default IPage<T> page(Page<T> pager) {
        return this.page(pager, null);
    }

    /**
     * 分页查询
     *
     * @param pager        分页参数
     * @param queryWrapper
     * @return 分页数据
     */
    default IPage<T> page(Page<T> pager, @Param(Constants.WRAPPER) Wrapper<T> queryWrapper) {
        return getMapper().selectPage(pager, queryWrapper);
    }


    /**
     * 根据 entity 条件，查询全部记录
     *
     * @param queryWrapper 实体对象封装操作类（可以为 null）
     * @return
     */
    default List<T> selectList(@Param(Constants.WRAPPER) Wrapper<T> queryWrapper) {
        return getMapper().selectList(queryWrapper);
    }


    /**
     * 根据 entity 条件，查询全部记录
     *
     * @param queryWrapper 实体对象封装操作类（可以为 null）
     * @return
     */
    default List<T> selectList(@Param(Constants.WRAPPER) Wrapper<T> queryWrapper, int size) {
        return getMapper().selectList(queryWrapper, size);
    }


    /**
     * 根据 entity 条件，查询一条记录
     *
     * @param queryWrapper 实体对象封装操作类（可以为 null）
     * @return
     */
    default T selectOne(@Param(Constants.WRAPPER) Wrapper<T> queryWrapper) {
        return getMapper().selectOne(queryWrapper);
    }

    default T selectFirst(@Param(Constants.WRAPPER) Wrapper<T> queryWrapper) {
        return getMapper().selectFirst(queryWrapper);
    }


    /**
     * 根据 Wrapper 条件，查询总记录数
     *
     * @param queryWrapper 实体对象封装操作类（可以为 null）
     */
    default Long selectCount(@Param(Constants.WRAPPER) Wrapper<T> queryWrapper) {
        return getMapper().selectCount(queryWrapper);
    }


    default Page<T> pageProcessing(Page<T> page) {
        if (page.getSize() > 1000) {
            page.setSize(1000);
        }
        List<OrderItem> orders = page.orders();
        if (CollUtil.isEmpty(orders)) {
            page.setRecords(Collections.emptyList());
        }
        orders = page.orders();
        LinkedList<OrderItem> newOrders = new LinkedList<>(orders);
        if (this instanceof SortService && orders.stream().noneMatch(c -> "sort".equals(c.getColumn()))) {
            newOrders.add(OrderItem.asc("sort"));
        }
        if (this instanceof BSDService<?, ?> && orders.stream().noneMatch(c -> "created_time".equals(c.getColumn()))) {
            newOrders.add(OrderItem.desc("created_time"));
        }
        page.setOrders(newOrders);
        return page;
    }

    default Wrapper<T> wrapperProcessing(Wrapper<T> wrapper) {
        if (wrapper == null) {
            wrapper = Wrappers.lambdaQuery();
        }
        if (wrapper instanceof QueryWrapper && this instanceof SortService)
            ((QueryWrapper<T>) wrapper).orderByAsc("sort");
        if (wrapper instanceof QueryWrapper && this instanceof BSDService<?, ?>)
            ((QueryWrapper<T>) wrapper).orderByDesc("created_time");
        if (
                (wrapper instanceof KerayLambdaQueryWrapper && (this instanceof BSDService<?, ?> || this instanceof SortService)) ||
                        (wrapper.getEntity() != null && wrapper instanceof LambdaQueryWrapper && (this instanceof BSDService<?, ?> || this instanceof SortService))
        ) {
            if (this instanceof SortService)
                ((LambdaQueryWrapper<MybatisBaseCacheModel>) wrapper).orderByAsc(MybatisBaseCacheModel::getSort);
            if (this instanceof BSDService<?, ?>)
                ((LambdaQueryWrapper<MybatisBaseCacheModel>) wrapper).orderByDesc(MybatisBaseCacheModel::getCreatedTime);
        }
        return wrapper;
    }
}
