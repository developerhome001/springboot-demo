package com.keray.common.mysql.config;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusProperties;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.Update;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.enums.SqlMethod;
import com.baomidou.mybatisplus.core.exceptions.MybatisPlusException;
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
import com.keray.common.entity.*;
import com.keray.common.entity.impl.BSDEntity;
import com.keray.common.mysql.handler.LocalDateTimeTypeHandler;
import com.keray.common.mysql.handler.LocalDateTypeHandler;
import com.keray.common.mysql.handler.LocalTimeTypeHandler;
import com.keray.common.service.mapper.MybatisPlusCacheMapper;
import com.keray.common.mysql.handler.StringEncryptionHandler;
import com.keray.common.utils.UUIDUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.scripting.defaults.RawSqlSource;
import org.apache.ibatis.type.JdbcType;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author by keray
 * date:2019/7/25 16:33
 */
@Configuration
@Aspect
@Slf4j
@ConditionalOnClass(MybatisPlusProperties.class)
@MapperScan("com.keray.common.service.mapper")
public class MybatisPlusSqlInjector implements BeanPostProcessor {

    private final IContext userContext;


    @Resource
    private MybatisPlusProperties mybatisPlusProperties;

    public MybatisPlusSqlInjector(IContext userContext) {
        this.userContext = userContext;
    }

    @PostConstruct
    public void init() {
        mybatisPlusProperties.getConfiguration().getTypeHandlerRegistry().register(LocalDateTime.class, LocalDateTimeTypeHandler.class);
        mybatisPlusProperties.getConfiguration().getTypeHandlerRegistry().register(LocalDate.class, LocalDateTypeHandler.class);
        mybatisPlusProperties.getConfiguration().getTypeHandlerRegistry().register(LocalTime.class, LocalTimeTypeHandler.class);
        mybatisPlusProperties.getConfiguration().getTypeHandlerRegistry().register(String.class, JdbcType.VARCHAR, StringEncryptionHandler.class);
        mybatisPlusProperties.getConfiguration().getTypeHandlerRegistry().register(String.class, StringEncryptionHandler.class);
    }

    @Bean
    @ConditionalOnMissingBean(ISqlInjector.class)
    @Primary
    public ISqlInjector sqlInjector() {
        return new AbstractSqlInjector() {
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


    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if ("sqlSessionFactory".equals(beanName)) {
            mybatisPlusProperties.getConfiguration().addMapper(MybatisPlusCacheMapper.class);
        }
        return BeanPostProcessor.super.postProcessAfterInitialization(bean, beanName);
    }

    @Pointcut("@annotation(com.keray.common.mysql.BaseDbUpdateModel)")
    public void updateById() {
    }

    @Pointcut("@annotation(com.keray.common.mysql.BaseDbUpdateWrapper)")
    public void update() {
    }

    @Pointcut("@annotation(com.keray.common.mysql.BaseDbInsert)")
    public void insert() {
    }

    @Before("updateById()")
    public void beforeUpdateById(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        IBSDEntity model = (IBSDEntity) args[0];
        if (model instanceof IBSDUEntity) {
            IBSDUEntity entity = (IBSDUEntity) model;
            if (StrUtil.isBlank(entity.getModifyBy())) {
                entity.setModifyBy(userContext.currentUserId());
            }
        }
        var context = MybatisPlusContext.context();
        if (context == null || !context.isNoUpdateModifyTime()) {
            model.setModifyTime(LocalDateTime.now());
        }
    }

    @Before("update()")
    public void beforeUpdate(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        IBSDEntity model = null;
        Update update;
        if (args.length == 1) {
            update = (Update) args[0];
        } else {
            model = (IBSDEntity) args[0];
            update = (Update) args[1];
        }
        var context = MybatisPlusContext.context();
        var setModifyTime = context == null || !context.isNoUpdateModifyTime();
        if (model != null) {
            model.setModifyTime(setModifyTime ? LocalDateTime.now() : null);
            if (model instanceof IBSDUEntity) {
                IBSDUEntity entity = (IBSDUEntity) model;
                if (StrUtil.isBlank(entity.getModifyBy())) {
                    entity.setModifyBy(userContext.currentUserId());
                }
                entity.setCreatedBy(null);
            }
        }
        if (update != null && model == null) {
            if (joinPoint.getTarget() instanceof IBSDUMapper) {
                if (update instanceof LambdaUpdateWrapper<?>) {
                    LambdaUpdateWrapper<IBSDUEntity> updateWrapper = (LambdaUpdateWrapper) update;
                    updateWrapper.set(setModifyTime, IBSDUEntity::getModifyTime, LocalDateTime.now())
                            .set(IBSDUEntity::getModifyBy, userContext.currentUserId());
                } else if (update instanceof UpdateWrapper) {
                    try {
                        ((UpdateWrapper<IBSDUEntity>) update).lambda()
                                .set(setModifyTime, IBSDUEntity::getModifyTime, LocalDateTime.now())
                                .set(IBSDUEntity::getModifyBy, userContext.currentUserId());
                    } catch (MybatisPlusException exception) {
                        ((UpdateWrapper<IBSDUEntity>) update)
                                .set(setModifyTime, "modify_time", LocalDateTime.now())
                                .set("modify_by", userContext.currentUserId());
                    }
                }
            } else if (joinPoint.getTarget() instanceof IBSDMapper) {
                if (update instanceof LambdaUpdateWrapper) {
                    LambdaUpdateWrapper<IBSDEntity> updateWrapper = (LambdaUpdateWrapper) update;
                    updateWrapper.set(setModifyTime, IBSDEntity::getModifyTime, LocalDateTime.now());
                } else if (update instanceof UpdateWrapper) {
                    try {
                        ((UpdateWrapper<IBSDEntity>) update).lambda()
                                .set(setModifyTime, IBSDEntity::getModifyTime, LocalDateTime.now());
                    } catch (MybatisPlusException exception) {
                        ((UpdateWrapper<BSDEntity>) update)
                                .set(setModifyTime, "modify_time", LocalDateTime.now());
                    }
                }
            }
        }
    }

    @Before("insert()")
    public void insert(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        Object data = args[0];
        Consumer<IBSEntity> work = (model) -> {
            IdType idType = model.idType();
            if (idType != IdType.AUTO) {
                if (model.idClazz() == String.class) {
                    if (ObjectUtil.isEmpty(model.getId())) {
                        model.setId(UUIDUtil.generateUUIDByTimestamp());
                    }
                } else if (model.idClazz() == Long.class) {
                    if (model.getId() == null) {
                        model.setId(UUIDUtil.generateUUID());
                    }
                } else if (model.idClazz() == Integer.class) {
                    if (model.getId() == null) {
                        model.setId(UUIDUtil.generateUUID().intValue());
                    }
                }
            }
            if (model instanceof IBSDEntity bm) {
                if (bm.getCreatedTime() == null) {
                    bm.setCreatedTime(LocalDateTime.now());
                }
                if (bm.getModifyTime() == null) {
                    bm.setModifyTime(bm.getCreatedTime());
                }
            }
            if (model instanceof IBSDUEntity im) {
                if (StrUtil.isBlank(im.getModifyBy())) {
                    im.setModifyBy(userContext.currentUserId());
                }
                if (StrUtil.isBlank(im.getCreatedBy())) {
                    im.setCreatedBy(userContext.currentUserId() == null ? "database" : userContext.currentUserId());
                }
            }
        };
        if (data instanceof IBSEntity model) {
            work.accept(model);
        } else if (data instanceof List<?> list) {
            for (var item : list) {
                if (item instanceof IBSEntity model) {
                    work.accept(model);
                }
            }
        }

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