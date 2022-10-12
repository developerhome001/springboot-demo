package com.keray.common;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.keray.common.entity.IBEntity;

/**
 * @author by keray
 * date:2021/5/10 4:10 下午
 */
public class Wrappers {

    private Wrappers() {
        // ignore
    }

    /**
     * 获取 QueryWrapper&lt;T&gt;
     *
     * @param <T> 实体类泛型
     * @return QueryWrapper&lt;T&gt;
     */
    public static <T> QueryWrapper<T> query() {
        return new QueryWrapper<>();
    }

    /**
     * 获取 QueryWrapper&lt;T&gt;
     *
     * @param entity 实体类
     * @param <T>    实体类泛型
     * @return QueryWrapper&lt;T&gt;
     */
    public static <T> QueryWrapper<T> query(T entity) {
        return new QueryWrapper<>(entity);
    }

    /**
     * 获取 KerayLambdaQueryWrapper&lt;T&gt;
     *
     * @param <T> 实体类泛型
     * @return KerayLambdaQueryWrapper&lt;T&gt;
     */
    public static <T extends IBEntity<T>> KerayLambdaQueryWrapper<T> lambdaQuery() {
        return new KerayLambdaQueryWrapper<>();
    }

    /**
     * 获取 KerayLambdaQueryWrapper&lt;T&gt;
     *
     * @param entity 实体类
     * @param <T>    实体类泛型
     * @return KerayLambdaQueryWrapper&lt;T&gt;
     */
    public static <T extends IBEntity<T>> KerayLambdaQueryWrapper<T> lambdaQuery(T entity) {
        return new KerayLambdaQueryWrapper<>(entity);
    }

    /**
     * 获取 KerayLambdaQueryWrapper&lt;T&gt;
     *
     * @param entityClass 实体类class
     * @param <T>         实体类泛型
     * @return KerayLambdaQueryWrapper&lt;T&gt;
     * @since 3.3.1
     */
    public static <T extends IBEntity<T>> KerayLambdaQueryWrapper<T> lambdaQuery(Class<T> entityClass) {
        return new KerayLambdaQueryWrapper<>(entityClass);
    }

    /**
     * 获取 UpdateWrapper&lt;T&gt;
     *
     * @param <T> 实体类泛型
     * @return UpdateWrapper&lt;T&gt;
     */
    public static <T> UpdateWrapper<T> update() {
        return new UpdateWrapper<>();
    }

    /**
     * 获取 UpdateWrapper&lt;T&gt;
     *
     * @param entity 实体类
     * @param <T>    实体类泛型
     * @return UpdateWrapper&lt;T&gt;
     */
    public static <T> UpdateWrapper<T> update(T entity) {
        return new UpdateWrapper<>(entity);
    }

    /**
     * 获取 KerayLambdaUpdateWrapper&lt;T&gt;
     *
     * @param <T> 实体类泛型
     * @return KerayLambdaUpdateWrapper&lt;T&gt;
     */
    public static <T extends IBEntity<T>> KerayLambdaUpdateWrapper<T> lambdaUpdate() {
        return new KerayLambdaUpdateWrapper<>();
    }

    /**
     * 获取 KerayLambdaUpdateWrapper&lt;T&gt;
     *
     * @param entity 实体类
     * @param <T>    实体类泛型
     * @return KerayLambdaUpdateWrapper&lt;T&gt;
     */
    public static <T extends IBEntity<T>> KerayLambdaUpdateWrapper<T> lambdaUpdate(T entity) {
        return new KerayLambdaUpdateWrapper<>(entity);
    }

    /**
     * 获取 KerayLambdaUpdateWrapper&lt;T&gt;
     *
     * @param entityClass 实体类class
     * @param <T>         实体类泛型
     * @return KerayLambdaUpdateWrapper&lt;T&gt;
     * @since 3.3.1
     */
    public static <T extends IBEntity<T>> KerayLambdaUpdateWrapper<T> lambdaUpdate(Class<T> entityClass) {
        return new KerayLambdaUpdateWrapper<>(entityClass);
    }
}
