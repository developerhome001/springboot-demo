package com.keray.common.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class MybatisPlusContext {


    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Context {
        private boolean noUpdateModifyTime;

        public Context setNoUpdateModifyTime(boolean noUpdateModifyTime) {
            this.noUpdateModifyTime = noUpdateModifyTime;
            return this;
        }



    }

    private static final ThreadLocal<Context> CONTEXT = new InheritableThreadLocal<>();

    public static void setContext(Context context) {
        CONTEXT.set(context);
    }

    public static void remove() {
        CONTEXT.remove();
    }

    public static Context context() {
        return CONTEXT.get();
    }
}
