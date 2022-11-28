package com.keray.common.entity.impl;

import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.keray.common.entity.IBEntity;

import java.io.Serializable;

/**
 * @author by keray
 * date:2020/7/15 9:38 上午
 */
public class BEntity<T extends BEntity<T>> extends Model<T> implements IBEntity<T> , Serializable {

}
