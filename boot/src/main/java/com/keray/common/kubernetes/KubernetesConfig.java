package com.keray.common.kubernetes;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "kubernetes")
@ConditionalOnProperty(name = "kubernetes.token")
public class KubernetesConfig {

    private String host;

    private String token;
    
}
