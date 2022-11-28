package com.keray.common.file;

import com.aliyun.oss.model.CannedAccessControlList;

public interface OssConfig {

    String getEndpoint();

    String getExtranetEndpoint();

    String getBucket();

    String getBasePath();

    String getRegion();

    Integer getPollCount();

    Integer getPollMax();

    CannedAccessControlList getAcl();
}
