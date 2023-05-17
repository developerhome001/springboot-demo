package com.keray.common.afs;

import com.aliyuncs.AcsResponse;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.afs.model.v20180112.AnalyzeNvcRequest;
import com.aliyuncs.afs.model.v20180112.AnalyzeNvcResponse;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import com.keray.common.AliyunConfig;
import com.keray.common.IPlugins;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnBean(AliyunAfsConfig.class)
public class AliyunAfsPlugin implements IPlugins {

    private final IAcsClient client;

    public AliyunAfsPlugin(AliyunAfsConfig aliyunAfsConfig, AliyunConfig aliyunConfig) throws ClientException {
        IClientProfile profile = DefaultProfile.getProfile(aliyunAfsConfig.getRegionid(), aliyunConfig.getAccessKeyId(), aliyunConfig.getAccessKeySecret());
        client = new DefaultAcsClient(profile);
        DefaultProfile.addEndpoint(aliyunAfsConfig.getRegionid(), aliyunAfsConfig.getRegionid(), "afs", "afs.aliyuncs.com");
    }

    public IAcsClient getClient() {
        return client;
    }

    public AnalyzeNvcResponse check(String code) throws ClientException {
        AnalyzeNvcRequest request = new AnalyzeNvcRequest();
        request.setData(code);
        request.setScoreJsonStr("{\"200\":\"PASS\",\"400\":\"NC\",\"800\":\"BLOCK\"}");
        return client.getAcsResponse(request);
    }
}
