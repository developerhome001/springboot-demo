package com.keray.common.kubernetes;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.credentials.AccessTokenAuthentication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(KubernetesConfig.class)
public class KubernetesContainerService {

    private final KubernetesConfig config;

    public KubernetesContainerService(KubernetesConfig config) {
        this.config = config;
    }

    @PostConstruct
    public void getConnection() {
        io.kubernetes.client.openapi.Configuration.setDefaultApiClient(new ClientBuilder().
                setBasePath(config.getHost()).setVerifyingSsl(false).
                setAuthentication(new AccessTokenAuthentication(config.getToken())).build());
    }

    /**
     * 返回容器的CPU最大值
     *
     * @param namespace
     * @param name
     * @return 返回限制几CU
     * @throws ApiException
     */
    public Double getContainerCpuLimit(String namespace, String name) throws ApiException {
        CoreV1Api apiInstance = new CoreV1Api();
        var pod = apiInstance.readNamespacedPod(name, namespace, "true", true, true);
        if (pod == null || pod.getSpec() == null) return null;
        var containers = pod.getSpec().getContainers();
        if (containers.isEmpty()) return null;
        var container = containers.get(0);
        if (container.getResources() == null) return null;
        var limitMap = container.getResources().getLimits();
        if (limitMap == null) return null;
        var data = limitMap.get("cpu");
        if (data == null) return null;
        return data.getNumber().doubleValue();
    }

    public void appStarted(String namespace, String name) {

    }
}
