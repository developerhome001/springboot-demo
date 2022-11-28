package com.demo.config;

import com.keray.common.gateway.records.GatewayRecords;
import com.keray.common.gateway.records.RecordsContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;

import javax.servlet.http.HttpServletRequest;

@Configuration
public class DemoGatewayRecords implements GatewayRecords {
    @Override
    public int support(HandlerMethod method, HttpServletRequest request) {
        return 0;
    }

    @Override
    public void records(RecordsContext context) {

    }
}
