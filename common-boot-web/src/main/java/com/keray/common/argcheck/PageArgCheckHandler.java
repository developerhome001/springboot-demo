package com.keray.common.argcheck;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.keray.common.ArgCheckHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;

/**
 * @author by keray
 * date:2021/7/9 10:48 上午
 */
@Configuration
public class PageArgCheckHandler implements ArgCheckHandler<Page<?>> {

    @Override
    public boolean support(MethodParameter parameter, Object org) {
        return org instanceof Page;
    }

    @Override
    public boolean check(MethodParameter parameter, Page<?> val) {
        if (CollUtil.isEmpty(val.getOrders())) {
            return true;
        }
        for (OrderItem item : val.getOrders()) {
            String column = item.getColumn();
            if (column.matches(".*\\s.*")) {
                return false;
            }
        }
        return true;
    }
}
