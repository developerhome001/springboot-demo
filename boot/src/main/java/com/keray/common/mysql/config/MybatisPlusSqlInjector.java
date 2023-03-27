package com.keray.common.mysql.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.enums.SqlMethod;
import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.injector.AbstractSqlInjector;
import com.baomidou.mybatisplus.core.injector.ISqlInjector;
import com.baomidou.mybatisplus.core.injector.methods.*;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.toolkit.sql.SqlScriptUtils;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.keray.common.IContext;
import com.keray.common.entity.IBSMapper;
import com.keray.common.entity.IBaseMapper;
import com.keray.common.service.mapper.MybatisPlusCacheMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.scripting.defaults.RawSqlSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.aspectj.lang.annotation.Aspect;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author by keray
 * date:2019/7/25 16:33
 */
@Configuration
@Aspect
@Slf4j
@ConditionalOnClass(MybatisConfiguration.class)
@MapperScan("com.keray.common.service.mapper")
public class MybatisPlusSqlInjector {

    private final IContext userContext;

    private final ApplicationContext applicationContext;


    public MybatisPlusSqlInjector(IContext userContext, ApplicationContext applicationContext) {
        this.userContext = userContext;
        this.applicationContext = applicationContext;
    }

    @Bean
    @ConditionalOnMissingBean(ISqlInjector.class)
    @Primary
    public ISqlInjector sqlInjector() {
        return new AbstractSqlInjector() {
            private boolean base = false;

            @Override
            public void inspectInject(MapperBuilderAssistant builderAssistant, Class<?> mapperClass) {
                if (!base) {
                    base = true;
                    SqlSessionFactory sqlSessionFactory = applicationContext.getBean(SqlSessionFactory.class);
                    var configuration = sqlSessionFactory.getConfiguration();
                    configuration.addMapper(MybatisPlusCacheMapper.class);
                }
                super.inspectInject(builderAssistant, mapperClass);
            }

            @Override
            public List<AbstractMethod> getMethodList(Class<?> mapperClass, TableInfo tableInfo) {
                List<AbstractMethod> result = new LinkedList<>(
                        Arrays.asList(
                                new Insert(),
                                new DeleteByMap(),
                                new Delete(),
                                new com.baomidou.mybatisplus.core.injector.methods.Update(),
                                new ISelectByMap(),
                                new SelectCount(),
                                new ISelectList(),
                                new ISelectMaps(),
                                new SelectObjs(),
                                new ISelectPage(),
                                new ISelectMapsPage(),
                                new ISelectSum()
                        )
                );
                if (IBSMapper.class.isAssignableFrom(mapperClass)) {
                    result.addAll(Arrays.asList(
                            new DeleteById(),
                            new DeleteBatchByIds(),
                            new UpdateById(),
                            new ISelectById(),
                            new ISelectBatchByIds()));
                }
                if (IBaseMapper.class.isAssignableFrom(mapperClass)) {
                    result.addAll(Arrays.asList(
                            new PhysicsDelete(),
                            new PhysicsDeleteById(),
                            new PhysicsDeleteBatchIds(),
                            new PhysicsDeleteByMap()));
                }
                return result;
            }
        };
    }


    @Bean
    @ConditionalOnMissingBean(MybatisPlusInterceptor.class)
    public MybatisPlusInterceptor paginationInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        PaginationInnerInterceptor paginationInnerInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        paginationInnerInterceptor.setMaxLimit(1000L);
        interceptor.addInnerInterceptor(paginationInnerInterceptor);
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
        return interceptor;
    }

    private static final class ISelectById extends BaseAbstractLogicMethod {

        public ISelectById() {
            super("selectById");
        }

        @Override
        public MappedStatement injectMappedStatement(Class<?> mapperClass, Class<?> modelClass, TableInfo tableInfo) {
            SqlMethod sqlMethod = SqlMethod.SELECT_BY_ID;
            SqlSource sqlSource = new RawSqlSource(configuration, String.format(sqlMethod.getSql(),
                    sqlSelectColumns(tableInfo, false),
                    tableInfo.getTableName(), tableInfo.getKeyColumn(), tableInfo.getKeyProperty(),
                    tableInfo.getLogicDeleteSql(true, true)), Object.class);
            return this.addSelectMappedStatementForTable(mapperClass, sqlMethod.getMethod(), sqlSource, tableInfo);
        }
    }

    private static final class ISelectBatchByIds extends BaseAbstractLogicMethod {

        public ISelectBatchByIds() {
            super("selectBatchIds");
        }

        @Override
        public MappedStatement injectMappedStatement(Class<?> mapperClass, Class<?> modelClass, TableInfo tableInfo) {
            SqlMethod sqlMethod = SqlMethod.SELECT_BATCH_BY_IDS;
            SqlSource sqlSource = languageDriver.createSqlSource(configuration, String.format(sqlMethod.getSql(),
                    sqlSelectColumns(tableInfo, false), tableInfo.getTableName(), tableInfo.getKeyColumn(),
                    SqlScriptUtils.convertForeach("#{item}", COLLECTION, null, "item", COMMA),
                    tableInfo.getLogicDeleteSql(true, true)), Object.class);
            return addSelectMappedStatementForTable(mapperClass, sqlMethod.getMethod(), sqlSource, tableInfo);
        }
    }

    private static final class ISelectByMap extends BaseAbstractLogicMethod {

        public ISelectByMap() {
            super("selectByMap");
        }

        @Override
        public MappedStatement injectMappedStatement(Class<?> mapperClass, Class<?> modelClass, TableInfo tableInfo) {
            SqlMethod sqlMethod = SqlMethod.SELECT_BY_MAP;
            String sql = String.format(sqlMethod.getSql(), sqlSelectColumns(tableInfo, false),
                    tableInfo.getTableName(), sqlWhereByMap(tableInfo));
            SqlSource sqlSource = languageDriver.createSqlSource(configuration, sql, Map.class);
            return this.addSelectMappedStatementForTable(mapperClass, sqlMethod.getMethod(), sqlSource, tableInfo);
        }
    }

    private static final class ISelectList extends BaseAbstractLogicMethod {
        public ISelectList() {
            super("selectList");
        }

        @Override
        public MappedStatement injectMappedStatement(Class<?> mapperClass, Class<?> modelClass, TableInfo tableInfo) {
            SqlMethod sqlMethod = SqlMethod.SELECT_LIST;
            String sql = String.format(sqlMethod.getSql(), sqlFirst(), sqlSelectColumns(tableInfo, true),
                    tableInfo.getTableName(), sqlWhereEntityWrapper(true, tableInfo), sqlOrderBy(tableInfo), sqlComment());
            SqlSource sqlSource = languageDriver.createSqlSource(configuration, sql, modelClass);
            return this.addSelectMappedStatementForTable(mapperClass, sqlMethod.getMethod(), sqlSource, tableInfo);
        }
    }

    private static final class ISelectPage extends BaseAbstractLogicMethod {
        public ISelectPage() {
            super("selectPage");
        }

        @Override
        public MappedStatement injectMappedStatement(Class<?> mapperClass, Class<?> modelClass, TableInfo tableInfo) {
            SqlMethod sqlMethod = SqlMethod.SELECT_PAGE;
            String sql = String.format(sqlMethod.getSql(), sqlFirst(), sqlSelectColumns(tableInfo, true),
                    tableInfo.getTableName(), sqlWhereEntityWrapper(true, tableInfo), sqlOrderBy(tableInfo), sqlComment());
            SqlSource sqlSource = languageDriver.createSqlSource(configuration, sql, modelClass);
            return this.addSelectMappedStatementForTable(mapperClass, sqlMethod.getMethod(), sqlSource, tableInfo);
        }
    }

    private static final class ISelectMaps extends BaseAbstractLogicMethod {
        public ISelectMaps() {
            super("selectMaps");
        }

        @Override
        public MappedStatement injectMappedStatement(Class<?> mapperClass, Class<?> modelClass, TableInfo tableInfo) {
            SqlMethod sqlMethod = SqlMethod.SELECT_MAPS;
            String sql = String.format(sqlMethod.getSql(), sqlFirst(), sqlSelectColumns(tableInfo, true),
                    tableInfo.getTableName(), sqlWhereEntityWrapper(true, tableInfo), sqlOrderBy(tableInfo), sqlComment());
            SqlSource sqlSource = languageDriver.createSqlSource(configuration, sql, modelClass);
            return this.addSelectMappedStatementForOther(mapperClass, sqlMethod.getMethod(), sqlSource, Map.class);
        }
    }

    private static final class ISelectMapsPage extends BaseAbstractLogicMethod {
        public ISelectMapsPage() {
            super("selectMapsPage");
        }

        @Override
        public MappedStatement injectMappedStatement(Class<?> mapperClass, Class<?> modelClass, TableInfo tableInfo) {
            SqlMethod sqlMethod = SqlMethod.SELECT_MAPS_PAGE;
            String sql = String.format(sqlMethod.getSql(), sqlFirst(), sqlSelectColumns(tableInfo, true),
                    tableInfo.getTableName(), sqlWhereEntityWrapper(true, tableInfo), sqlOrderBy(tableInfo), sqlComment());
            SqlSource sqlSource = languageDriver.createSqlSource(configuration, sql, modelClass);
            return this.addSelectMappedStatementForOther(mapperClass, sqlMethod.getMethod(), sqlSource, Map.class);
        }
    }

    private static final class ISelectSum extends AbstractMethod {
        public ISelectSum() {
            super("selectSum");
        }

        @Override
        public MappedStatement injectMappedStatement(Class<?> mapperClass, Class<?> modelClass, TableInfo tableInfo) {
            String methodName = "selectSum";
            String sqlTemp = "<script>\nSELECT sum(%s) FROM %s %s\n</script>";
            String sql = String.format(sqlTemp, sqlSelectColumns(tableInfo, true), tableInfo.getTableName(),
                    sqlWhereEntityWrapper(true, tableInfo));
            SqlSource sqlSource = languageDriver.createSqlSource(configuration, sql, modelClass);
            return addSelectMappedStatementForOther(mapperClass, methodName, sqlSource, Double.class);
        }
    }


    private static final class PhysicsDelete extends AbstractMethod {
        public PhysicsDelete() {
            super("delete");
        }

        @Override
        public MappedStatement injectMappedStatement(Class<?> mapperClass, Class<?> modelClass, TableInfo tableInfo) {
            SqlMethod sqlMethod = SqlMethod.DELETE;
            String sql = String.format(sqlMethod.getSql(), tableInfo.getTableName(),
                    sqlWhereEntityWrapper(true, tableInfo),
                    sqlComment());
            SqlSource sqlSource = languageDriver.createSqlSource(configuration, sql, modelClass);
            return this.addDeleteMappedStatement(mapperClass, "physicsDelete", sqlSource);
        }
    }

    private static final class PhysicsDeleteBatchIds extends AbstractMethod {
        public PhysicsDeleteBatchIds() {
            super("deleteBatchIds");
        }


        @Override
        public MappedStatement injectMappedStatement(Class<?> mapperClass, Class<?> modelClass, TableInfo tableInfo) {

            SqlMethod sqlMethod = SqlMethod.DELETE_BATCH_BY_IDS;
            String sql = String.format(sqlMethod.getSql(), tableInfo.getTableName(), tableInfo.getKeyColumn(),
                    SqlScriptUtils.convertForeach("#{item}", COLLECTION, null, "item", COMMA));
            SqlSource sqlSource = languageDriver.createSqlSource(configuration, sql, Object.class);
            return this.addDeleteMappedStatement(mapperClass, "physicsDeleteBatchIds", sqlSource);
        }
    }

    private static final class PhysicsDeleteById extends AbstractMethod {
        public PhysicsDeleteById() {
            super("deleteById");
        }


        @Override
        public MappedStatement injectMappedStatement(Class<?> mapperClass, Class<?> modelClass, TableInfo tableInfo) {

            SqlMethod sqlMethod = SqlMethod.DELETE_BY_ID;
            String sql = String.format(sqlMethod.getSql(), tableInfo.getTableName(), tableInfo.getKeyColumn(),
                    tableInfo.getKeyProperty());
            SqlSource sqlSource = languageDriver.createSqlSource(configuration, sql, Object.class);
            return this.addDeleteMappedStatement(mapperClass, "physicsDeleteById", sqlSource);
        }
    }

    private static final class PhysicsDeleteByMap extends AbstractMethod {
        public PhysicsDeleteByMap() {
            super("deleteByMap");
        }


        @Override
        public MappedStatement injectMappedStatement(Class<?> mapperClass, Class<?> modelClass, TableInfo tableInfo) {
            SqlMethod sqlMethod = SqlMethod.DELETE_BY_MAP;
            String sql = String.format(sqlMethod.getSql(), tableInfo.getTableName(), this.sqlWhereByMap(tableInfo));
            SqlSource sqlSource = languageDriver.createSqlSource(configuration, sql, Map.class);
            return this.addDeleteMappedStatement(mapperClass, "physicsDeleteByMap", sqlSource);
        }
    }


}

abstract class BaseAbstractLogicMethod extends AbstractMethod {


    public BaseAbstractLogicMethod(String methodName) {
        super(methodName);
    }

    /**
     * SQL 查询所有表字段
     *
     * @param table        表信息
     * @param queryWrapper 是否为使用 queryWrapper 查询
     * @return sql 脚本
     */
    @Override
    protected String sqlSelectColumns(TableInfo table, boolean queryWrapper) {
        /* 假设存在 resultMap 映射返回 */
        String selectColumns = ASTERISK;
        if (!queryWrapper) {
            return selectColumns;
        }
        return SqlScriptUtils.convertChoose(String.format("%s != null and %s != null", WRAPPER, Q_WRAPPER_SQL_SELECT),
                SqlScriptUtils.unSafeParam(Q_WRAPPER_SQL_SELECT), selectColumns);
    }
}
