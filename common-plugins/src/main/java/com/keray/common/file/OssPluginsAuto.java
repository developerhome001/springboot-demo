package com.keray.common.file;

import com.keray.common.SpringContextHolder;
import com.keray.common.exception.BizRuntimeException;

/**
 * @author by keray
 * date:2021/4/21 4:25 下午
 */
public class OssPluginsAuto {

    public static OssPlugins getOssPluginsByType(String type) {
        if ("middle".equals(type)) {
            return SpringContextHolder.getBean("middleOssPlugins");
        } else if ("cdn".equals(type)) {
            return SpringContextHolder.getBean("cdnOssPlugins");
        } else if ("disk".equals(type)) {
            return SpringContextHolder.getBean("diskOssPlugins");
        } else {
            throw new BizRuntimeException();
        }
    }
}
