package com.keray.common.video;

import com.keray.common.IPlugins;

/**
 * @author by keray
 * date:2021/6/28 10:22 上午
 */
public interface VideoPlugins extends IPlugins {
    Object auth(String videoFileName, String title) throws Exception;
}
