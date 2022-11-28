package com.keray.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.keray.common.utils.CommonUtil;

import java.io.Serializable;
import java.lang.reflect.Field;

/**
 * @author by keray
 * date:2020/7/15 9:39 上午
 */
public interface IBSEntity<T extends IBSEntity<T, ID>, ID extends Serializable> extends IBEntity<T> {
    ID getId();

    void setId(ID id);

    default Class<?> idClazz() {
        Class<?> clazz = this.getClass();
        try {
            return clazz.getMethod("getId").getReturnType();
        } catch (NoSuchMethodException ignore) {
            throw new RuntimeException();
        }
    }

    default IdType idType() {
        Class<?> clazz = this.getClass();
        try {
            Field field = CommonUtil.getClassField(clazz, "id");
            TableId tableId = field.getAnnotation(TableId.class);
            if (tableId == null) {
                return IdType.INPUT;
            }
            return tableId.type();
        } catch (NoSuchFieldException ignore) {
            throw new RuntimeException();
        }
    }
}
