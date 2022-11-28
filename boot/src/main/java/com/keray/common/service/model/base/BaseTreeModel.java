package com.keray.common.service.model.base;

import com.keray.common.entity.IBSEntity;

import java.io.Serializable;
import java.util.List;

/**
 * @author by keray
 * date:2019/8/16 16:15
 * 树形实体接口
 */
public interface BaseTreeModel<T extends BaseTreeModel<T, ID>, ID extends Serializable> extends IBSEntity<T, ID> {

    ID getParentId();

    void setParentId(ID parentId);

    T getParent();

    void setParent(T parent);

    void setChildren(List<T> children);

    List<T> getChildren();

}
