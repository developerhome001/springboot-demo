package com.keray.common.service.model.base;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.keray.common.entity.IBSEntity;

import java.io.Serializable;
import java.util.List;


/**
 * 树形数据V2
 * 根据path快速实现下级查询
 *
 * @param <T>
 * @param <ID>
 */
public interface BaseTreeModelV2<T extends BaseTreeModelV2<T, ID>, ID extends Serializable> extends BaseTreeModel<T, ID> {

    /**
     * 节点路径
     * 根节点 空字符串
     * 1/2
     *
     * @return
     */
    @JsonIgnore
    String getPath();

    @JsonIgnore
    default String getSplitStr() {
        return "/";
    }

    default void setPath(String path) {
        if (ObjectUtil.isEmpty(getParentId())) {
            var spit = path.split(getSplitStr());
            if (spit.length > 0) {
                setParentId(idCover(spit[spit.length - 1]));
            }
        }
    }


    // 叶子结点标志
    Boolean getLeaf();

    void setLeaf(Boolean leaf);

    ID idCover(String strId);

    default boolean getTop() {
        return getLevel() == 0;
    }

    default int getLevel() {
        var path = getPath();
        if (StrUtil.isBlank(path)) return 0;
        return path.split(getSplitStr()).length;
    }

    default String getOPath() {
        if (getPath() == null) return null;
        return getPath() + (getTop() ? getId() : (getSplitStr() + getId()));
    }

    default ID getTopParentId() {
        var path = getPath();
        if (StrUtil.isEmpty(path)) return getId();
        return idCover(path.split(getSplitStr())[0]);
    }
}
