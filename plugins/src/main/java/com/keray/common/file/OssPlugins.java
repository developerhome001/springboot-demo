package com.keray.common.file;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.event.ProgressEvent;
import com.aliyun.oss.event.ProgressEventType;
import com.aliyun.oss.event.ProgressListener;
import com.aliyun.oss.internal.OSSHeaders;
import com.aliyun.oss.model.*;
import com.keray.common.AliyunConfig;
import com.keray.common.AliyunPlugins;
import com.keray.common.exception.BizRuntimeException;
import com.keray.common.utils.TimeUtil;
import com.keray.common.utils.UUIDUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * @author by keray
 * date:2021/4/8 5:22 下午
 */
@Slf4j
public class OssPlugins extends AliyunPlugins implements FileUploadPlugins, FileManagerPlugins {

    public final OssConfig ossConfig;

    private OSS ossClient;

    private final AtomicInteger threadCount = new AtomicInteger(0);
    private final ThreadPoolExecutor executor;
    private final RedisTemplate<String, String> redisTemplate;
    private final String redisPrefix = "oss:task:";

    public OssPlugins(AliyunConfig aliyunConfig, OssConfig ossConfig, RedisTemplate<String, String> redisTemplate) {
        super(aliyunConfig);
        this.ossConfig = ossConfig;
        this.redisTemplate = redisTemplate;
        executor = new ThreadPoolExecutor(ossConfig.getPollCount(), ossConfig.getPollMax(),
                1, TimeUnit.MINUTES, new LinkedBlockingDeque<>(), r -> {
            Thread t = new Thread(r);
            t.setName("oss-" + threadCount.getAndIncrement());
            return t;
        });
    }

    public synchronized OSS createClient() {
        if (ossClient == null) {
            ossClient = new OSSClientBuilder().build(ossConfig.getEndpoint(), aliyunConfig.getAccessKeyId(), aliyunConfig.getAccessKeySecret());
        }
        return ossClient;
    }

    @PreDestroy
    public void destroy() {
        if (ossClient != null) {
            ossClient.shutdown();
        }
    }


    public String streamUploadSync(InputStream inputStream, String fileName) {
        return streamUpload(inputStream, fileName, false).getPath();
    }

    public Future<String> streamUploadASync(InputStream inputStream, String fileName) {
        return executor.submit(() -> streamUploadSync(inputStream, fileName));
    }

    @Override
    public UploadResult streamUploadBack(InputStream inputStream, String fileName) {
        return streamUpload(inputStream, fileName, true);
    }

    @SneakyThrows
    public String[] streamUploadSync(MultiInputUpload... uploads) {
        return upload(uploads, (obj) -> streamUploadSync(obj.getInputStream(), obj.getFileName())).toArray(new String[0]);
    }

    public Future<String[]> streamUploadASync(MultiInputUpload... uploads) {
        return executor.submit(() -> streamUploadSync(uploads));
    }

    @Override
    public UploadResult[] streamUploadBack(MultiInputUpload... uploads) {
        return upload(uploads, obj -> streamUpload(obj.getInputStream(), obj.getFileName(), true)).toArray(new UploadResult[0]);
    }


    @SneakyThrows
    public String webFileUploadSync(String url) {
        var response = HttpUtil.createGet(url).execute();
        if (response.getStatus() != 200) throw new BizRuntimeException();
        InputStream inputStream = response.bodyStream();
        return streamUploadSync(inputStream, getWebFileName(url));
    }


    public Future<String> webFileUploadASync(String url) {
        return executor.submit(() -> webFileUploadSync(url));
    }

    @Override
    public UploadResult webFileUploadBack(String url) {
        return streamUpload(HttpUtil.createGet(url).execute().bodyStream(), getWebFileName(url), true);
    }

    @SneakyThrows
    public String webFileUploadSync(String url, String fileName) {
        InputStream inputStream = HttpUtil.createGet(url).execute().bodyStream();
        return streamUploadSync(inputStream, fileName);
    }

    @Override
    public Future<String> webFileUploadASync(String url, String fileName) {
        return executor.submit(() -> webFileUploadSync(url, fileName));
    }

    @Override
    public UploadResult webFileUploadBack(String url, String fileName) {
        return streamUpload(HttpUtil.createGet(url).execute().bodyStream(), fileName, true);
    }

    public String[] webFileUploadSync(String... urls) {
        return upload(urls, this::webFileUploadSync).toArray(new String[0]);
    }

    public Future<String[]> webFileUploadASync(String... urls) {
        return executor.submit(() -> webFileUploadSync(urls));
    }

    @Override
    public UploadResult[] webFileUploadBack(String... urls) {
        return upload(urls, url -> streamUpload(HttpUtil.createGet(url).execute().bodyStream(), getWebFileName(url), true)).toArray(new UploadResult[0]);
    }

    @Override
    public String taskProgress(String taskId) {
        return redisTemplate.opsForValue().get(redisPrefix + taskId);
    }


    private UploadResult streamUpload(InputStream inputStream, String fileName, boolean back) {
        String filePath = getFilePath(fileName);
        PutObjectRequest putObjectRequest = new PutObjectRequest(ossConfig.getBucket(), filePath, inputStream);
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setHeader(OSSHeaders.OSS_STORAGE_CLASS, StorageClass.Standard.toString());
        metadata.setObjectAcl(ossConfig.getAcl());
        putObjectRequest.setMetadata(metadata);
        return ossUpload(putObjectRequest, back);
    }

    public UploadResult ossUpload(PutObjectRequest putObjectRequest, boolean back) {
        String taskId = "UIX" + UUIDUtil.generateUUIDByTimestamp();
        if (back) {
            putObjectRequest.withProgressListener(new ProgressListener() {
                private long bytesWritten = 0;
                private long totalBytes = 0;

                @Override
                public void progressChanged(ProgressEvent progressEvent) {
                    long bytes = progressEvent.getBytes();
                    if (progressEvent.getEventType() == ProgressEventType.REQUEST_BYTE_TRANSFER_EVENT) {
                        this.bytesWritten += bytes;
                        if (this.totalBytes != -1) {
                            redisTemplate.opsForValue().set(redisPrefix + taskId, NumberUtil.round((this.bytesWritten * 100.0 / this.totalBytes), 2).toString(), Duration.ofDays(10));
                        }
                    } else if (progressEvent.getEventType() == ProgressEventType.REQUEST_CONTENT_LENGTH_EVENT) {
                        this.totalBytes = bytes;
                    }
                }
            });
            executor.execute(() -> createClient().putObject(putObjectRequest));
        } else {
            createClient().putObject(putObjectRequest);
        }
        return new UploadResult(putObjectRequest.getKey(), taskId);
    }

    public byte[] getObject(String objectName) throws IOException {
        OSSObject object = createClient().getObject(ossConfig.getBucket(), objectName);
        return object.getObjectContent().readAllBytes();
    }

    @SneakyThrows
    private <T, A> ArrayList<A> upload(T[] data, Function<T, A> function) {
        class r {
            final int index;
            final A result;

            r(int i, A r) {
                index = i;
                result = r;
            }
        }
        ExecutorCompletionService<r> completionService = new ExecutorCompletionService<>(executor);
        for (int i = 0; i < data.length; i++) {
            int finalI = i;
            completionService.submit(() -> {
                A result = function.apply(data[finalI]);
                return new r(finalI, result);
            });
        }
        ArrayList<A> result = new ArrayList<>(data.length);
        Future<r> future;
        for (int i = 0; ; i++) {
            if (i >= data.length) {
                break;
            }
            try {
                future = completionService.take();
                r r = future.get();
                result.set(r.index, r.result);
            } catch (Exception e) {
                log.error("批量上传异常", e);
            }
        }
        return result;
    }

    public String getFilePath(String fileName) {
        if (fileName.startsWith("/")) {
            if (fileName.startsWith("/" + ossConfig.getBasePath())) {
                return fileName.substring(1);
            }
            return ossConfig.getBasePath() + fileName;
        }
        String[] strings = fileName.split("\\.");
        String hz = "empty";
        if (strings.length > 1) {
            hz = strings[strings.length - 1];
        }
        return StrUtil.format("{}/{}/{}/{}",
                ossConfig.getBasePath(),
                hz,
                TimeUtil.DATE_TIME_FORMATTER_YM.format(LocalDate.now()),
                TimeUtil.DATE_TIME_FORMATTER_SN.format(LocalDateTime.now()) + "@" + fileName
        );
    }

    public static void main(String[] args) {
        System.out.println(TimeUtil.DATE_TIME_FORMATTER_YM.format(LocalDate.now()));
    }

    @Override
    public boolean deleteFile(String filePath) {
        try {
            createClient().deleteObject(ossConfig.getBucket(), filePath);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public boolean doesObjectExist(String fileName) {
        try {
            createClient().doesObjectExist(ossConfig.getBucket(), fileName);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public boolean deleteFiles(List<String> filePaths) {
        try {
            if (!filePaths.isEmpty())
                createClient().deleteObjects(new DeleteObjectsRequest(ossConfig.getBucket()).withKeys(filePaths));
        } catch (Exception e) {
            return false;
        }
        return true;
    }
}
