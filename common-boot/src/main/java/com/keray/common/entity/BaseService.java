package com.keray.common.entity;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.Update;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.keray.common.CommonResultCode;
import com.keray.common.exception.BizRuntimeException;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author by keray
 * date:2019/7/25 16:03
 */
public interface BaseService<T extends IBaseEntity<T, ID>, ID extends Serializable> extends BSDUService<T, ID> {
    /**
     * 获取基础模块操作mapper
     *
     * @return
     */
    @Override
    IBaseMapper<T, ID> getMapper();

    @Deprecated
    default int delete(Wrapper<T> wrapper) {
        throw new IllegalArgumentException("delete只允许传入Update类型 now= " + wrapper.getClass());
    }

    default int delete(UpdateWrapper<T> wrapper) {
        return getMapper().delete(wrapper);
    }

    default int delete(LambdaUpdateWrapper<T> wrapper) {
        return getMapper().delete(wrapper);
    }

}
