package com.keray.common.diamond.mysql;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.keray.common.entity.impl.BSEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("sys_diamond")
public class DiamondStoreModel extends BSEntity<DiamondStoreModel> {

    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String value;
}
