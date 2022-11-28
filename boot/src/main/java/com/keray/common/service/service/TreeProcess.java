package com.keray.common.service.service;

public interface TreeProcess<T> {
    void apply(T child, T parent);
}
