package com.keray.common.filter;

import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 基于filter  在任何情况下都会执行
 */
public abstract class KerayFilter extends OncePerRequestFilter {

    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }
}
