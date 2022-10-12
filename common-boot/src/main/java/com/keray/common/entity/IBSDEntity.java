package com.keray.common.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author by keray
 * date:2021/3/25 4:35 下午
 */
public interface IBSDEntity<T extends IBSDEntity<T, ID>, ID extends Serializable> extends IBSEntity<T, ID> {

    LocalDateTime getCreatedTime();

    void setCreatedTime(LocalDateTime createdTime);

    LocalDateTime getModifyTime();

    void setModifyTime(LocalDateTime modifyTime);

}
