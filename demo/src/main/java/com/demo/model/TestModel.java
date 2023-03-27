package com.demo.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.keray.common.entity.impl.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("sys_dict_type")
public class TestModel extends BaseEntity<TestModel> {
    private String name;
    private String code;
}
