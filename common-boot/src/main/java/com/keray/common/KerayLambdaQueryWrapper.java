package com.keray.common;

import com.baomidou.mybatisplus.core.conditions.ISqlSegment;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.enums.SqlKeyword;
import com.baomidou.mybatisplus.core.enums.SqlLike;
import com.baomidou.mybatisplus.core.toolkit.LambdaUtils;
import com.baomidou.mybatisplus.core.toolkit.sql.SqlUtils;
import com.baomidou.mybatisplus.core.toolkit.support.ColumnCache;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.keray.common.entity.IBEntity;
import com.keray.common.support.DataEncryptionHandler;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author by keray
 * date:2021/5/10 4:11 下午
 */
public class KerayLambdaQueryWrapper<T extends IBEntity<T>> extends LambdaQueryWrapper<T> {

    private Map<String, Map<String, ColumnCache>> cache = new HashMap<>();

    protected String columnToString(SFunction<T, ?> column, boolean onlyColumn) {
        return KerayLambda.getColumn(LambdaUtils.extract(column), onlyColumn, this.getEntityClass(), cache);
    }

    public KerayLambdaQueryWrapper() {
        this((T) null);
    }

    /**
     * 不建议直接 new 该实例，使用 Wrappers.lambdaQuery(entity)
     */
    public KerayLambdaQueryWrapper(T entity) {
        super.setEntity(entity);
        super.initNeed();
    }


    /**
     * 不建议直接 new 该实例，使用 Wrappers.lambdaQuery(entity)
     */
    public KerayLambdaQueryWrapper(Class<T> entityClass) {
        super.setEntityClass(entityClass);
        super.initNeed();
    }


    protected LambdaQueryWrapper<T> likeValue(boolean condition, SqlKeyword keyword, SFunction<T, ?> column, Object val, SqlLike sqlLike) {
        return maybeDo(condition, () -> {
            // 只有 前缀匹配才能处理
            if (sqlLike != SqlLike.RIGHT) {
                super.likeValue(condition, keyword, column, val, sqlLike);
                return;
            }
            Object result = val;
            var columnMate = KerayLambda.fieldIEncryption(column, this, cache);
            if (val instanceof String str && columnMate.encryptionFlag()) {
                result = DataEncryptionHandler.encryptLeftPrefix(str);
            }
            Object finalResult = result;
            appendSqlSegments((ISqlSegment) columnMate::columnName, keyword, () -> formatParam(null, SqlUtils.concatLike(finalResult, sqlLike)));
        });
    }


    protected LambdaQueryWrapper<T> addCondition(boolean condition, SFunction<T, ?> column, SqlKeyword sqlKeyword, Object val) {
        return maybeDo(condition, () -> {
            if (!(val instanceof String result) || sqlKeyword != SqlKeyword.EQ && sqlKeyword != SqlKeyword.NE) {
                super.addCondition(condition, column, sqlKeyword, val);
                return;
            }
            var columnMate = KerayLambda.fieldIEncryption(column, this, cache);
            if (columnMate.encryptionFlag()) {
                result = DataEncryptionHandler.encrypt(result);
            }
            Object finalResult = result;
            appendSqlSegments((ISqlSegment) columnMate::columnName, sqlKeyword, () -> formatParam(null, finalResult));
        });
    }

    @Override
    public LambdaQueryWrapper<T> in(boolean condition, SFunction<T, ?> column, Object... values) {
        return maybeDo(condition, () -> {
            if (valuesProcess(condition, column, true, values)) {
                return;
            }
            super.in(condition, column, values);
        });
    }

    @Override
    public LambdaQueryWrapper<T> in(boolean condition, SFunction<T, ?> column, Collection<?> coll) {
        return maybeDo(condition, () -> {
            if (collProcess(condition, column, true, coll)) {
                return;
            }
            super.in(condition, column, coll);
        });
    }

    @Override
    public LambdaQueryWrapper<T> notIn(boolean condition, SFunction<T, ?> column, Object... values) {
        return maybeDo(condition, () -> {
            if (valuesProcess(condition, column, false, values)) {
                return;
            }
            super.notIn(condition, column, values);
        });
    }

    @Override
    public LambdaQueryWrapper<T> notIn(boolean condition, SFunction<T, ?> column, Collection<?> coll) {
        return maybeDo(condition, () -> {
            if (collProcess(condition, column, false, coll)) {
                return;
            }
            super.notIn(condition, column, coll);
        });
    }

    private boolean collProcess(boolean condition, SFunction<T, ?> column, boolean in, Collection<?> coll) {
        if (coll != null) {
            if (coll.size() == 1) {
                addCondition(condition, column, in ? SqlKeyword.EQ : SqlKeyword.NE, coll.iterator().next());
                return true;
            }
            var columnMate = KerayLambda.fieldIEncryption(column, this, cache);
            Collection<?> result = coll;
            if (columnMate.encryptionFlag()) {
                result = coll.stream().map(v -> {
                    if (v instanceof String s) {
                        return DataEncryptionHandler.encrypt(s);
                    }
                    return v;
                }).toList();
            }
            appendSqlSegments((ISqlSegment) columnMate::columnName, in ? SqlKeyword.IN : SqlKeyword.NOT_IN, inExpression(result));
            return true;
        }
        return false;
    }

    private boolean valuesProcess(boolean condition, SFunction<T, ?> column, boolean in, Object... values) {
        if (values != null) {
            if (values.length == 1) {
                addCondition(condition, column, in ? SqlKeyword.EQ : SqlKeyword.NE, values[0]);
                return true;
            }
            var columnMate = KerayLambda.fieldIEncryption(column, this, cache);
            if (columnMate.encryptionFlag()) {
                for (var i = 0; i < values.length; i++) {
                    var obj = values[i];
                    if (obj instanceof String s) {
                        values[i] = DataEncryptionHandler.encrypt(s);
                    }
                }
            }
            appendSqlSegments((ISqlSegment) columnMate::columnName, in ? SqlKeyword.IN : SqlKeyword.NOT_IN, inExpression(values));
            return true;
        }
        return false;
    }

}
