package com.keray.common.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.keray.common.entity.impl.BaseIntEntity;
import com.keray.common.service.model.base.BaseTreeModel;
import com.keray.common.service.model.base.BaseTreeModelV2;
import com.keray.common.service.model.base.SortModel;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @author by keray
 * date:2021/5/8 1:27 下午
 */
@Getter
@Setter
@TableName("dev_base_field")
public class MybatisBaseCacheModel extends BaseIntEntity<MybatisBaseCacheModel> implements SortModel<MybatisBaseCacheModel>, BaseTreeModel<MybatisBaseCacheModel, Long>, BaseTreeModelV2<MybatisBaseCacheModel, Long> {

    private Integer sort;

    private Long parentId;

    private String path;

    private Boolean leaf;


    @Override
    public MybatisBaseCacheModel getParent() {
        return null;
    }

    @Override
    public void setChildren(List<MybatisBaseCacheModel> children) {

    }

    @Override
    public List<MybatisBaseCacheModel> getChildren() {
        return null;
    }

    @Override
    public void setParent(MybatisBaseCacheModel parent) {

    }

    @Override
    public Long idCover(String strId) {
        return Long.parseLong(strId);
    }
}
