package com.keray.common.file;

import cn.hutool.core.util.StrUtil;

import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;

/**
 * @author by keray
 * date:2021/4/8 5:22 下午
 */
public interface FileUploadPlugins {
    /**
     * <p>
     * <h3>作者 keray</h3>
     * <h3>时间： 2020/9/19 10:04 上午</h3>
     * 输入流上传
     * </p>
     *
     * @param inputStream 输入流
     * @param fileName    文件名
     * @return <p> {@link } </p>
     * @throws
     */
    String streamUploadSync(InputStream inputStream, String fileName);

    Future<String> streamUploadASync(InputStream inputStream, String fileName);

    UploadResult streamUploadBack(InputStream inputStream, String fileName);

    /**
     * <p>
     * <h3>作者 keray</h3>
     * <h3>时间： 2020/9/19 10:04 上午</h3>
     * 批量上传
     * </p>
     *
     * @param uploads 上传列表
     * @return <p> {@link } </p>
     * @throws
     */
    String[] streamUploadSync(MultiInputUpload... uploads);

    Future<String[]> streamUploadASync(MultiInputUpload... uploads);

    UploadResult[] streamUploadBack(MultiInputUpload... uploads);

    /**
     * <p>
     * <h3>作者 keray</h3>
     * <h3>时间： 2021/4/8 5:36 下午</h3>
     * 网络文件上传
     * </p>
     *
     * @param url 文件url
     * @return <p> {@link UploadResult} </p>
     * @throws
     */
    String webFileUploadSync(String url);

    Future<String> webFileUploadASync(String url);

    UploadResult webFileUploadBack(String url);

    /**
     * <p>
     * <h3>作者 keray</h3>
     * <h3>时间： 2021/4/8 5:36 下午</h3>
     * 网络文件上传
     * </p>
     *
     * @param url      文件url
     * @param fileName 重新命名
     * @return <p> {@link UploadResult} </p>
     * @throws
     */
    String webFileUploadSync(String url, String fileName);

    Future<String> webFileUploadASync(String url, String fileName);

    UploadResult webFileUploadBack(String url, String fileName);


    /**
     * <p>
     * <h3>作者 keray</h3>
     * <h3>时间： 2021/4/8 5:36 下午</h3>
     * 网络文件批量上传
     * </p>
     *
     * @param urls
     * @return <p> {@link UploadResult[]} </p>
     * @throws
     */
    String[] webFileUploadSync(String... urls);

    Future<String[]> webFileUploadASync(String... urls);

    UploadResult[] webFileUploadBack(String... urls);

    String taskProgress(String taskId);

    default String getWebFileName(String url) {
        if (StrUtil.isBlank(url) || !url.startsWith("http")) {
            throw new IllegalStateException("webFile mast start with http:" + url);
        }
        List<String> res = new LinkedList<>();
        String[] s = url.split("/");
        for (int i = 3; i < s.length; i++) {
            res.add(s[i]);
        }
        return String.join("/", res);
    }
}
