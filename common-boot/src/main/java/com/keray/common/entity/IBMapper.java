package com.keray.common.entity;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.TypeUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.enums.SqlMethod;
import com.baomidou.mybatisplus.core.exceptions.MybatisPlusException;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.core.toolkit.ExceptionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.keray.common.IContext;
import com.keray.common.SpringContextHolder;
import com.keray.common.Wrappers;
import com.keray.common.annotation.BaseDbInsert;
import com.keray.common.utils.UUIDUtil;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.SqlSession;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author by keray
 * date:2019/7/25 16:56
 */
public interface IBMapper<T extends IBEntity<T>> extends BaseMapper<T> {


    /**
     * 根据 ID 删除
     *
     * @param id 主键ID
     */
    @Deprecated
    int deleteById(Serializable id);

    /**
     * 删除（根据ID 批量删除）
     *
     * @param idList 主键ID列表(不能为 null 以及 empty)
     */
    @Deprecated
    int deleteBatchIds(@Param(Constants.COLLECTION) Collection<?> idList);


    /**
     * 根据 ID 修改
     *
     * @param entity 实体对象
     */
    @Deprecated
    int updateById(@Param(Constants.ENTITY) T entity);


    /**
     * 根据 ID 查询
     *
     * @param id 主键ID
     */
    @Deprecated
    T selectById(Serializable id);

    /**
     * 查询（根据ID 批量查询）
     *
     * @param idList 主键ID列表(不能为 null 以及 empty)
     */
    @Deprecated
    List<T> selectBatchIds(@Param(Constants.COLLECTION) Collection<? extends Serializable> idList);

    default T selectFirst(@Param(Constants.WRAPPER) Wrapper<T> queryWrapper) {
        if (queryWrapper == null) {
            queryWrapper = Wrappers.lambdaQuery();
        }
        if (queryWrapper instanceof LambdaQueryWrapper<T> q) {
            q.last(" limit 1");
        } else if (queryWrapper instanceof QueryWrapper<T> q) {
            q.last(" limit 1");
        }
        List<T> ts = this.selectList(queryWrapper);
        if (CollectionUtils.isNotEmpty(ts)) {
            return ts.get(0);
        }
        return null;
    }

    /**
     * 插入一条记录
     *
     * @param entity 实体对象
     */
    int insert(T entity);


    /**
     * 根据 columnMap 条件，删除记录
     *
     * @param columnMap 表字段 map 对象
     */
    int deleteByMap(@Param(Constants.COLUMN_MAP) Map<String, Object> columnMap);

    /**
     * 根据 entity 条件，删除记录
     *
     * @param queryWrapper 实体对象封装操作类（可以为 null,里面的 entity 用于生成 where 语句）
     */
    int delete(@Param(Constants.WRAPPER) Wrapper<T> queryWrapper);


    /**
     * 根据 whereEntity 条件，更新记录
     *
     * @param entity        实体对象 (set 条件值,可以为 null)
     * @param updateWrapper 实体对象封装操作类（可以为 null,里面的 entity 用于生成 where 语句）
     */
    int update(@Param(Constants.ENTITY) T entity, @Param(Constants.WRAPPER) Wrapper<T> updateWrapper);


    /**
     * 查询（根据 columnMap 条件）
     *
     * @param columnMap 表字段 map 对象
     */
    List<T> selectByMap(@Param(Constants.COLUMN_MAP) Map<String, Object> columnMap);

    /**
     * 根据 entity 条件，查询一条记录
     *
     * @param queryWrapper 实体对象封装操作类（可以为 null）
     */
    default T selectOne(@Param(Constants.WRAPPER) Wrapper<T> queryWrapper) {
        if (queryWrapper == null) {
            queryWrapper = Wrappers.lambdaQuery();
        }
        if (queryWrapper instanceof LambdaQueryWrapper<T> q) {
            q.last(" limit 2");
        } else if (queryWrapper instanceof QueryWrapper<T> q) {
            q.last(" limit 2");
        }

        List<T> ts = this.selectList(queryWrapper);
        if (CollectionUtils.isNotEmpty(ts)) {
            if (ts.size() != 1) {
                throw ExceptionUtils.mpe("One record is expected, but the query result is multiple records");
            }
            return ts.get(0);
        }
        return null;
    }

    /**
     * 根据 Wrapper 条件，查询总记录数
     *
     * @param queryWrapper 实体对象封装操作类（可以为 null）
     */
    Long selectCount(@Param(Constants.WRAPPER) Wrapper<T> queryWrapper);

    /**
     * 根据 entity 条件，查询全部记录
     *
     * @param queryWrapper 实体对象封装操作类（可以为 null）
     */
    List<T> selectList(@Param(Constants.WRAPPER) Wrapper<T> queryWrapper);

    /**
     * 根据 entity 条件，查询全部记录
     *
     * @param queryWrapper 实体对象封装操作类（可以为 null）
     */
    default List<T> selectList(@Param(Constants.WRAPPER) Wrapper<T> queryWrapper, int size) {
        return this.selectPage(new Page<>(1, size), queryWrapper).getRecords();
    }

    /**
     * 根据 Wrapper 条件，查询全部记录
     *
     * @param queryWrapper 实体对象封装操作类（可以为 null）
     */
    List<Map<String, Object>> selectMaps(@Param(Constants.WRAPPER) Wrapper<T> queryWrapper);

    /**
     * 根据 Wrapper 条件，查询全部记录
     * <p>注意： 只返回第一个字段的值</p>
     *
     * @param queryWrapper 实体对象封装操作类（可以为 null）
     */
    List<Object> selectObjs(@Param(Constants.WRAPPER) Wrapper<T> queryWrapper);

    /**
     * 根据 entity 条件，查询全部记录（并翻页）
     *
     * @param page         分页查询条件（可以为 RowBounds.DEFAULT）
     * @param queryWrapper 实体对象封装操作类（可以为 null）
     */
    <E extends IPage<T>> E selectPage(E page, @Param(Constants.WRAPPER) Wrapper<T> queryWrapper);

    /**
     * 根据 Wrapper 条件，查询全部记录（并翻页）
     *
     * @param page         分页查询条件
     * @param queryWrapper 实体对象封装操作类
     */
    <E extends IPage<Map<String, Object>>> E selectMapsPage(E page, @Param(Constants.WRAPPER) Wrapper<T> queryWrapper);


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
    Double selectSum(@Param(Constants.WRAPPER) Wrapper<T> wrapper);


    /**
     * <p>
     * 插入（批量）
     * </p>
     *
     * @param entityList 实体对象列表
     * @param batchSize  插入批次数量
     * @return boolean
     */
    @Transactional(rollbackFor = Exception.class)
    default boolean insertBatch(List<T> entityList, int batchSize) {
        if (CollUtil.isEmpty(entityList)) {
            throw new IllegalArgumentException("Error: entityList must not be empty");
        }
        try (SqlSession batchSqlSession = sqlSessionBatch()) {
            int size = entityList.size();
            String sqlStatement = sqlStatement(SqlMethod.INSERT_ONE);
            for (int i = 0; i < size; i++) {
                batchSqlSession.insert(sqlStatement, entityList.get(i));
                if (i >= 1 && i % batchSize == 0) {
                    batchSqlSession.flushStatements();
                }
            }
            batchSqlSession.flushStatements();
        } catch (Throwable e) {
            throw new MybatisPlusException("Error: Cannot execute insertBatch Method. Cause", e);
        }
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    default boolean insertBatch(List<T> entityList) {
        return insertBatch(entityList, entityList.size());
    }


    default boolean exits(Wrapper<T> wrapper) {
        return selectCount(wrapper) > 0;
    }

    /**
     * <p>
     * 批量操作 SqlSession
     * </p>
     */
    default SqlSession sqlSessionBatch() {
        return SqlHelper.sqlSessionBatch(currentModelClass());
    }

    /**
     * 获取SqlStatement
     *
     * @param sqlMethod 方法
     * @return String
     */
    default String sqlStatement(SqlMethod sqlMethod) {
        return SqlHelper.table(currentModelClass()).getSqlStatement(sqlMethod.getMethod());
    }

    /**
     * <p>
     * 获取当前class
     * </p>
     */
    default Class<T> currentModelClass() {
        return (Class<T>) TypeUtil.getTypeArgument(this.getClass().getInterfaces()[0].getGenericInterfaces()[0]);
    }

}
