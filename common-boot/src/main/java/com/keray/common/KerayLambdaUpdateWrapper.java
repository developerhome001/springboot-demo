package com.keray.common;

import com.baomidou.mybatisplus.core.conditions.ISqlSegment;
import com.baomidou.mybatisplus.core.conditions.SharedString;
import com.baomidou.mybatisplus.core.conditions.segments.MergeSegments;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.Update;
import com.baomidou.mybatisplus.core.enums.SqlKeyword;
import com.baomidou.mybatisplus.core.enums.SqlLike;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.core.toolkit.LambdaUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.sql.SqlUtils;
import com.baomidou.mybatisplus.core.toolkit.support.ColumnCache;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.keray.common.entity.IBEntity;
import com.keray.common.support.DataEncryptionHandler;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author by keray
 * date:2021/5/10 4:13 下午
 */
public class KerayLambdaUpdateWrapper<T extends IBEntity<T>> extends LambdaUpdateWrapper<T>
        implements Update<LambdaUpdateWrapper<T>, SFunction<T, ?>> {

    private Map<String, Map<String, ColumnCache>> cache = new HashMap<>();
    private final List<String> sqlSet;

    protected String columnToString(SFunction<T, ?> column, boolean onlyColumn) {
        return KerayLambda.getColumn(LambdaUtils.extract(column), onlyColumn, getEntityClass(), cache);
    }

    public KerayLambdaUpdateWrapper() {
        this((T) null);
    }

    /**
     * 不建议直接 new 该实例，使用 Wrappers.lambdaQuery(entity)
     */
    public KerayLambdaUpdateWrapper(T entity) {
        super.setEntity(entity);
        super.initNeed();
        this.sqlSet = new LinkedList<>();
    }

    /**
     * 不建议直接 new 该实例，使用 Wrappers.lambdaQuery(entity)
     */
    public KerayLambdaUpdateWrapper(Class<T> entityClass) {
        super.setEntityClass(entityClass);
        super.initNeed();
        this.sqlSet = new LinkedList<>();
    }

    KerayLambdaUpdateWrapper(T entity, Class<T> entityClass, List<String> sqlSet, AtomicInteger paramNameSeq,
                             Map<String, Object> paramNameValuePairs, MergeSegments mergeSegments, SharedString paramAlias,
                             SharedString lastSql, SharedString sqlComment, SharedString sqlFirst) {
        super.setEntity(entity);
        super.setEntityClass(entityClass);
        this.sqlSet = sqlSet;
        this.paramNameSeq = paramNameSeq;
        this.paramNameValuePairs = paramNameValuePairs;
        this.expression = mergeSegments;
        this.paramAlias = paramAlias;
        this.lastSql = lastSql;
        this.sqlComment = sqlComment;
        this.sqlFirst = sqlFirst;
    }

    protected LambdaUpdateWrapper<T> likeValue(boolean condition, SqlKeyword keyword, SFunction<T, ?> column, Object val, SqlLike sqlLike) {
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


    protected LambdaUpdateWrapper<T> addCondition(boolean condition, SFunction<T, ?> column, SqlKeyword sqlKeyword, Object val) {
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
    public LambdaUpdateWrapper<T> in(boolean condition, SFunction<T, ?> column, Object... values) {
        return maybeDo(condition, () -> {
            if (valuesProcess(condition, column, true, values)) {
                return;
            }
            super.in(condition, column, values);
        });
    }

    @Override
    public LambdaUpdateWrapper<T> in(boolean condition, SFunction<T, ?> column, Collection<?> coll) {
        return maybeDo(condition, () -> {
            if (collProcess(condition, column, true, coll)) {
                return;
            }
            super.in(condition, column, coll);
        });
    }

    @Override
    public LambdaUpdateWrapper<T> notIn(boolean condition, SFunction<T, ?> column, Object... values) {
        return maybeDo(condition, () -> {
            if (valuesProcess(condition, column, false, values)) {
                return;
            }
            super.notIn(condition, column, values);
        });
    }

    @Override
    public LambdaUpdateWrapper<T> notIn(boolean condition, SFunction<T, ?> column, Collection<?> coll) {
        return maybeDo(condition, () -> {
            if (collProcess(condition, column, false, coll)) {
                return;
            }
            super.notIn(condition, column, coll);
        });
    }

    private boolean collProcess(boolean condition, SFunction<T, ?> column, boolean in, Collection<?> coll) {
        if (coll != null) {
            if (coll.size() > 1) {
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
            addCondition(condition, column, in ? SqlKeyword.EQ : SqlKeyword.NE, coll.iterator().next());
            return true;
        }
        return false;
    }

    private boolean valuesProcess(boolean condition, SFunction<T, ?> column, boolean in, Object... values) {
        if (values != null) {
            if (values.length > 1) {
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
            addCondition(condition, column, in ? SqlKeyword.EQ : SqlKeyword.NE, values[0]);
            return true;
        }
        return false;
    }

    @Override
    public LambdaUpdateWrapper<T> set(boolean condition, SFunction<T, ?> column, Object val, String mapping) {
        return maybeDo(condition, () -> {
            if (!(val instanceof String result)) {
                sqlSet.add(columnToString(column) + Constants.EQUALS + formatParam(mapping, val));
                return;
            }
            var columnMate = KerayLambda.fieldIEncryption(column, this, cache);
            if (columnMate.encryptionFlag()) {
                result = DataEncryptionHandler.encrypt(result);
            }
            String sql = formatParam(mapping, result);
            sqlSet.add(columnMate.columnName() + Constants.EQUALS + sql);
        });
    }

    @Override
    public LambdaUpdateWrapper<T> setSql(boolean condition, String sql) {
        if (condition && StringUtils.isNotBlank(sql)) {
            sqlSet.add(sql);
        }
        return typedThis;
    }

    @Override
    public String getSqlSet() {
        if (CollectionUtils.isEmpty(sqlSet)) {
            return null;
        }
        return String.join(Constants.COMMA, sqlSet);
    }

    @Override
    protected KerayLambdaUpdateWrapper<T> instance() {
        return new KerayLambdaUpdateWrapper<>(getEntity(), getEntityClass(), new LinkedList<>(), paramNameSeq, paramNameValuePairs,
                new MergeSegments(), paramAlias, SharedString.emptyString(), SharedString.emptyString(), SharedString.emptyString());
    }

    @Override
    public void clear() {
        super.clear();
        sqlSet.clear();
    }

}
