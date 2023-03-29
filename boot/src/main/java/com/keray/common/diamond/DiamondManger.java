package com.keray.common.diamond;

import cn.hutool.core.util.StrUtil;
import com.keray.common.diamond.handler.DiamondHandler;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.data.util.ProxyUtils;

import javax.annotation.PostConstruct;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Configuration
@Order(Integer.MIN_VALUE)
public class DiamondManger implements BeanPostProcessor {


    private final static Map<String, Node> FIELD_MAP = new HashMap<>(32);
    private final static Map<String, Object> FIELD_DEF_MAP = new HashMap<>(32);

    private final static Map<Class, ValueHandler> HANDLER_CACHE = new ConcurrentHashMap<>(32);

    private final Store store;
    private final DiamondHandler handler;

    private boolean hadOK = false;

    public DiamondManger(ObjectProvider<Store> store, ObjectProvider<DiamondHandler> handler) {
        this.store = store.getIfAvailable();
        this.handler = handler.getIfAvailable();
    }

    @PostConstruct
    public void init() {
        if (store != null && handler != null) {
            log.info("动态配置管理开启");
        }
    }

    @SneakyThrows
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        hadOK = true;
        if (store != null && handler != null) {
            process(bean);
        }
        return BeanPostProcessor.super.postProcessBeforeInitialization(bean, beanName);
    }


    @SneakyThrows
    public Object handler(Object bean) {
        if (hadOK) return bean;
        if (store != null && handler != null) {
            process(bean);
        }
        return bean;
    }

    private void process(Object bean) throws Exception {
        var clazz = ProxyUtils.getUserClass(bean);
        var fields = clazz.getDeclaredFields();
        for (var field : fields) {
            var ani = field.getAnnotation(Diamond.class);
            if (ani == null) continue;
            var key = ani.value();
            // 获取默认值并保存默认值
            {
                field.setAccessible(true);
                FIELD_DEF_MAP.put(key, field.get(bean));
            }
            var value = store.getValue(key);
            FIELD_MAP.put(key, new Node(bean, field));
            log.warn("新增diamond字段 {}", key);
            // 报错key到类，字段的映射
            diamondChange(key, value);
        }
    }

    /**
     * 数据改变时触发
     *
     * @param key
     * @param value
     * @throws Exception
     */
    public void diamondChange(String key, String value) throws Exception {
        // 根据key找到对呀的类和field
        var node = FIELD_MAP.get(key);
        if (node == null) {
            throw new IllegalArgumentException("为找到key对应的字段");
        }
        log.warn("diamond配置变化 {}=>{}", key, value);
        store.save(key, value);
        var field = node.field;
        var clazz = node.bean.getClass();
        if (value == null) {
            var def = FIELD_DEF_MAP.get(key);
            // 如果有默认值 设置默认值
            if (def != null) {
                log.warn("diamond设置默认值 {}=>{}", key, def);
                field.set(node.bean, def);
                return;
            }
        }
        // 获取set方法
        var setName = StrUtil.upperFirstAndAddPre(StrUtil.toCamelCase(field.getName()), "set");
        try {
            // 寻找字符串的set方法
            var setMethod = clazz.getMethod(setName, String.class);
            setMethod.invoke(node.bean, value);
        } catch (NoSuchMethodException ignore) {
            var ani = field.getAnnotation(Diamond.class);
            var handler = ani.handler();
            var handlerObj = HANDLER_CACHE.computeIfAbsent(handler, clz -> {
                try {
                    return (ValueHandler) clz.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            // 寻找原生的set方法
            var setMethod = clazz.getMethod(setName, field.getType());
            setMethod.invoke(node.bean, handlerObj.decode(value, field.getType()));
        }
    }

    @AllArgsConstructor
    @NoArgsConstructor
    protected static class Node {
        Object bean;

        Field field;
    }

}
