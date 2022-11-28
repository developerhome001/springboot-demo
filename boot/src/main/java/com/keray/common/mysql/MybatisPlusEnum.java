package com.keray.common.mysql;

import com.baomidou.mybatisplus.annotation.IEnum;

import java.io.Serializable;

/**
 * @author by keray
 * date:2019/11/7 11:03
 */
public interface MybatisPlusEnum<T extends Serializable> extends IEnum<T> {
    /**
     * <p>
     * <h3>作者 keray</h3>
     * <h3>时间： 2019/11/7 11:44</h3>
     * 获取mybatis plus枚举类数据库映射值
     * </p>
     *
     * @param
     * @return <p> {@link Serializable} </p>
     * @throws
     */
    T getCode();

    @Override
    default T getValue() {
        return getCode();
    }
}
