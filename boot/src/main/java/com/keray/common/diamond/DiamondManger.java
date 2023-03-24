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
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.data.util.ProxyUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Configuration
@ConditionalOnBean({Store.class, DiamondHandler.class})
@Order(Integer.MIN_VALUE)
public class DiamondManger {

    @Resource
    private Store store;

    @Resource
    private DiamondHandler handler;

    public DiamondManger() {
        log.info("动态配置管理开启");
    }

    @AllArgsConstructor
    @NoArgsConstructor
    protected static class Node {
        Object bean;

        Field field;
    }

    private final static Map<String, Node> FIELD_MAP = new HashMap<>(32);

    private final static Map<Class, ValueHandler> HANDLER_CACHE = new ConcurrentHashMap<>(32);


    @SneakyThrows
    public Object handler(Object bean) {
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
            var key = ani.key();
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

}
