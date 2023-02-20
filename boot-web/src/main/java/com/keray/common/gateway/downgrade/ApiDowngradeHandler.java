package com.keray.common.gateway.downgrade;

public interface ApiDowngradeHandler {
    boolean handler(ApiDowngrade downgrade);
}
