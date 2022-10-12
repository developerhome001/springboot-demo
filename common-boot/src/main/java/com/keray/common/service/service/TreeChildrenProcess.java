package com.keray.common.service.service;

import java.util.List;

public interface TreeChildrenProcess<T> {
    void apply(List<T> child, T parent);
}
