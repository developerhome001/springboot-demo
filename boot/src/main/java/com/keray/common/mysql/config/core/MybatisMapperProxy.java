package com.keray.common.mysql.config.core;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.Update;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.exceptions.MybatisPlusException;
import com.baomidou.mybatisplus.core.override.MybatisMapperMethod;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.keray.common.IContext;
import com.keray.common.SpringContextHolder;
import com.keray.common.entity.*;
import com.keray.common.entity.impl.BSDEntity;
import com.keray.common.mysql.BaseDbInsert;
import com.keray.common.mysql.BaseDbUpdateModel;
import com.keray.common.mysql.BaseDbUpdateWrapper;
import com.keray.common.mysql.config.MybatisPlusContext;
import com.keray.common.utils.UUIDUtil;
import org.apache.ibatis.binding.MapperProxy;
import org.apache.ibatis.reflection.ExceptionUtil;
import org.apache.ibatis.session.SqlSession;

import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * <p> 从 {@link MapperProxy}  copy 过来 </p>
 * <li> 使用 MybatisMapperMethod </li>
 *
 * @author miemie
 * @since 2018-06-09
 */
public class MybatisMapperProxy<T> implements InvocationHandler, Serializable {

    private static final long serialVersionUID = -5154982058833204559L;
    private static final int ALLOWED_MODES = MethodHandles.Lookup.PRIVATE | MethodHandles.Lookup.PROTECTED
            | MethodHandles.Lookup.PACKAGE | MethodHandles.Lookup.PUBLIC;
    private static final Constructor<MethodHandles.Lookup> lookupConstructor;
    private static final Method privateLookupInMethod;
    private final SqlSession sqlSession;
    private final Class<T> mapperInterface;
    private final Map<Method, MapperMethodInvoker> methodCache;

    private IContext userContext;

    public MybatisMapperProxy(SqlSession sqlSession, Class<T> mapperInterface, Map<Method, MapperMethodInvoker> methodCache) {
        this.sqlSession = sqlSession;
        this.mapperInterface = mapperInterface;
        this.methodCache = methodCache;
    }

    static {
        Method privateLookupIn;
        try {
            privateLookupIn = MethodHandles.class.getMethod("privateLookupIn", Class.class, MethodHandles.Lookup.class);
        } catch (NoSuchMethodException e) {
            privateLookupIn = null;
        }
        privateLookupInMethod = privateLookupIn;

        Constructor<MethodHandles.Lookup> lookup = null;
        if (privateLookupInMethod == null) {
            // JDK 1.8
            try {
                lookup = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, int.class);
                lookup.setAccessible(true);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(
                        "There is neither 'privateLookupIn(Class, Lookup)' nor 'Lookup(Class, int)' method in java.lang.invoke.MethodHandles.",
                        e);
            } catch (Throwable t) {
                lookup = null;
            }
        }
        lookupConstructor = lookup;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            modelProcess(proxy, method, args);
            if (Object.class.equals(method.getDeclaringClass())) {
                return method.invoke(this, args);
            } else {
                return cachedInvoker(method).invoke(proxy, method, args, sqlSession);
            }
        } catch (Throwable t) {
            throw ExceptionUtil.unwrapThrowable(t);
        }
    }


    protected void modelProcess(Object proxy, Method method, Object[] args) {
        if (userContext == null) {
            synchronized (this) {
                if (userContext == null) {
                    userContext = SpringContextHolder.getBean(IContext.class);
                }
            }
        }
        Object ani = method.getAnnotation(BaseDbUpdateModel.class);
        if (ani != null) beforeUpdateById(args);
        ani = method.getAnnotation(BaseDbUpdateWrapper.class);
        if (ani != null) beforeUpdate(args, proxy);
        ani = method.getAnnotation(BaseDbInsert.class);
        if (ani != null) insert(args);
    }

    public void beforeUpdateById(Object[] args) {
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

    public void beforeUpdate(Object[] args, Object target) {
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
            if (target instanceof IBSDUMapper) {
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
            } else if (target instanceof IBSDMapper) {
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

    public void insert(Object[] args) {
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

    private MapperMethodInvoker cachedInvoker(Method method) throws Throwable {
        try {
            return CollectionUtils.computeIfAbsent(methodCache, method, m -> {
                if (m.isDefault()) {
                    try {
                        if (privateLookupInMethod == null) {
                            return new DefaultMethodInvoker(getMethodHandleJava8(method));
                        } else {
                            return new DefaultMethodInvoker(getMethodHandleJava9(method));
                        }
                    } catch (IllegalAccessException | InstantiationException | InvocationTargetException
                             | NoSuchMethodException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    return new PlainMethodInvoker(new MybatisMapperMethod(mapperInterface, method, sqlSession.getConfiguration()));
                }
            });
        } catch (RuntimeException re) {
            Throwable cause = re.getCause();
            throw cause == null ? re : cause;
        }
    }

    private MethodHandle getMethodHandleJava9(Method method)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        final Class<?> declaringClass = method.getDeclaringClass();
        return ((MethodHandles.Lookup) privateLookupInMethod.invoke(null, declaringClass, MethodHandles.lookup())).findSpecial(
                declaringClass, method.getName(), MethodType.methodType(method.getReturnType(), method.getParameterTypes()),
                declaringClass);
    }

    private MethodHandle getMethodHandleJava8(Method method)
            throws IllegalAccessException, InstantiationException, InvocationTargetException {
        final Class<?> declaringClass = method.getDeclaringClass();
        return lookupConstructor.newInstance(declaringClass, ALLOWED_MODES).unreflectSpecial(method, declaringClass);
    }

    interface MapperMethodInvoker {
        Object invoke(Object proxy, Method method, Object[] args, SqlSession sqlSession) throws Throwable;
    }

    private static class PlainMethodInvoker implements MapperMethodInvoker {
        private final MybatisMapperMethod mapperMethod;

        public PlainMethodInvoker(MybatisMapperMethod mapperMethod) {
            super();
            this.mapperMethod = mapperMethod;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args, SqlSession sqlSession) throws Throwable {
            return mapperMethod.execute(sqlSession, args);
        }
    }

    private static class DefaultMethodInvoker implements MapperMethodInvoker {
        private final MethodHandle methodHandle;

        public DefaultMethodInvoker(MethodHandle methodHandle) {
            super();
            this.methodHandle = methodHandle;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args, SqlSession sqlSession) throws Throwable {
            return methodHandle.bindTo(proxy).invokeWithArguments(args);
        }
    }
}
