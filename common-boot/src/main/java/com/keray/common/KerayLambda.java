package com.keray.common;

import com.baomidou.mybatisplus.core.conditions.AbstractLambdaWrapper;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.baomidou.mybatisplus.core.toolkit.LambdaUtils;
import com.baomidou.mybatisplus.core.toolkit.support.ColumnCache;
import com.baomidou.mybatisplus.core.toolkit.support.LambdaMeta;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.keray.common.entity.MybatisBaseCacheModel;
import com.keray.common.support.DataEncryptionHandler;
import org.apache.ibatis.reflection.property.PropertyNamer;

import java.io.Serializable;
import java.util.Map;

/**
 * @author by keray
 * date:2021/5/10 4:15 下午
 */
public class KerayLambda {

    public static String getColumn(LambdaMeta lambda, boolean onlyColumn, Class<?> entityClazz, Map<String, Map<String, ColumnCache>> cache) {
        Class<?> aClass = lambda.getInstantiatedClass();
        String fieldName = PropertyNamer.methodToProperty(lambda.getImplMethodName());
        Class<?> clazz = entityClazz == null ? aClass : entityClazz;
        Map<String, ColumnCache> cacheMap = columnCacheMap(clazz, cache);
        Assert.notNull(cacheMap, "can not find lambda cache for this entity [%s]", clazz);
        ColumnCache columnCache = cacheMap.get(LambdaUtils.formatKey(fieldName));
        Assert.notNull(columnCache, "can not find lambda cache for this entity [%s] filed[%s]", clazz, fieldName);
        return onlyColumn ? columnCache.getColumn() : columnCache.getColumnSelect();
    }

    private static Map<String, ColumnCache> columnCacheMap(Class<?> clazz, Map<String, Map<String, ColumnCache>> cache) {
        Map<String, ColumnCache> data = cache.get(clazz.getName());
        if (data != null) {
            return data;
        }
        data = LambdaUtils.getColumnMap(clazz);
        if (data == null && clazz.equals(MybatisBaseCacheModel.class)) {
            LambdaUtils.installCache(TableInfoHelper.getTableInfo(clazz));
            data = LambdaUtils.getColumnMap(clazz);
        }
        if (data == null) {
            data = LambdaUtils.getColumnMap(MybatisBaseCacheModel.class);
            if (data == null) {
                LambdaUtils.installCache(TableInfoHelper.getTableInfo(clazz));
            }
            data = LambdaUtils.getColumnMap(MybatisBaseCacheModel.class);
        }
        if (data != null) {
            cache.put(clazz.getName(), data);
        }
        return data;
    }


    public static FieldEncryption fieldIEncryption(SFunction<?, ?> column, AbstractLambdaWrapper<?, ?> wrapper, Map<String, Map<String, ColumnCache>> cache) {
        Class<?> clazz = wrapper.getEntityClass();
        var k = LambdaUtils.extract(column);
        if (clazz == null) clazz = k.getInstantiatedClass();
        var columnName = KerayLambda.getColumn(k, true, wrapper.getEntityClass(), cache);
        return new FieldEncryption(columnName, KerayLambda.fieldIEncryption(columnName, clazz));
    }

    public static boolean fieldIEncryption(String columnName, Class<?> clazz) {
        var tableInfo = TableInfoHelper.getTableInfo(clazz);
        if (tableInfo == null) return false;
        TableFieldInfo fieldInfo = null;
        for (var field : tableInfo.getFieldList()) {
            if (field.getColumn().equals(columnName)) {
                fieldInfo = field;
            }
        }
        if (fieldInfo == null) return false;
        return DataEncryptionHandler.class.equals(fieldInfo.getTypeHandler());
    }

    public record FieldEncryption(String columnName, boolean encryptionFlag) implements Serializable {
    }
}
