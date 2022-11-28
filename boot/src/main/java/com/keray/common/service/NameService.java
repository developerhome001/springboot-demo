package com.keray.common.service;

import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.keray.common.Wrappers;
import com.keray.common.entity.BSService;
import com.keray.common.service.model.NameModel;

import java.io.Serializable;

/**
 * @author by keray
 * date:2021/4/29 2:00 下午
 */
public interface NameService<T extends NameModel<T, ID>, ID extends Serializable> extends BSService<T, ID> {

    default String queryName(ID id) {
        T t;
        if (SqlHelper.table(getMapper().currentModelClass()).getFieldList().stream().anyMatch(v -> "name".equals(v.getColumn()))) {
            t = selectOne(Wrappers.<T>query().select("name").eq("id", id));
        } else {
            t = getMapper().selectById(id);
        }
        if (null != t){
            return t.getName();
        }
        return null;
    }
}
