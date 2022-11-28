package com.keray.common.entity;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author by keray
 * date:2020/7/15 9:38 上午
 */
public interface IBEntity<T extends IBEntity<T>> {
    /**
     * <p>
     * <h3>作者 keray</h3>
     * <h3>时间： 2019/9/4 15:00</h3>
     * 清除实体中的空字符串
     * </p>
     *
     * @param
     * @return <p> {@link T} </p>
     * @throws
     */
    default T clearEmptyStringField() {
        return clearEmptyStringField("");
    }

    default T clearEmptyStringField(String... forceFiled) {
        try {
            T copy = (T) this.getClass().getDeclaredConstructor().newInstance();
            BeanUtil.copyProperties(this, copy);
            return clearEmptyStringField(copy, forceFiled);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }
        return (T) this;
    }

    default T clearEmptyStringField(T copy, String... forceFiled) {
        List<String> fieldList = Arrays.asList(forceFiled);
        Class<?> clazz = copy.getClass();
        List<Field> fields = copy.scanFields(copy.getClass(), null);
        for (Field field : fields) {
            try {
                boolean setNull = false;
                if (fieldList.contains(field.getName())) {
                    setNull = true;
                }
                if (field.getType() == String.class) {
                    Method get = scanMethod(clazz, "get" + StrUtil.upperFirst(field.getName()));
                    if (get == null) {
                        continue;
                    }
                    String result = (String) get.invoke(copy);
                    if ("".equals(result)) {
                        setNull = true;
                    }
                }
                if (setNull) {
                    Method set = scanMethod(clazz, "set" + StrUtil.upperFirst(field.getName()), field.getType());
                    set.invoke(copy, (Object) null);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return copy;
    }

    default Map<String, Object> toMap() {
        return BeanUtil.beanToMap(this);
    }

    default Method scanMethod(Class<?> p, String name, Class<?>... type) throws NoSuchMethodException {
        if (p == null) {
            return null;
        }
        Method method;
        try {
            method = p.getMethod(name, type);
        } catch (NoSuchMethodException e) {
            method = null;
        }
        return method == null ? scanMethod(p.getSuperclass(), name) : method;
    }

    default List<Field> scanFields(Class<?> p, List<Field> fields) {
        if (fields == null) {
            fields = new LinkedList<>();
        }
        fields.addAll(Arrays.asList(p.getDeclaredFields()));
        if (p.getSuperclass() != null) {
            scanFields(p.getSuperclass(), fields);
        }
        return fields;
    }
}
