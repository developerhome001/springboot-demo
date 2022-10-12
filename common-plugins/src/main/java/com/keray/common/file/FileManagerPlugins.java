package com.keray.common.file;

import com.keray.common.IPlugins;

import java.util.List;

/**
 * @author by keray
 * date:2021/6/24 7:03 下午
 */
public interface FileManagerPlugins extends FileUploadPlugins, IPlugins {

    boolean deleteFile(String filePath);

    boolean deleteFiles(List<String> filePaths);
}
