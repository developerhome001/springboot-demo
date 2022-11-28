package com.demo.config;

import com.keray.common.IUserContext;
import org.springframework.context.annotation.Configuration;

import javax.servlet.http.HttpServletRequest;

@Configuration
public class UserContext implements IUserContext<Object> {
    @Override
    public HttpServletRequest currentRequest() {
        return null;
    }

    @Override
    public void setCurrentRequest(HttpServletRequest request) {

    }

    @Override
    public Object currentTokenData() {
        return null;
    }

    @Override
    public void setTokenData(Object tokenData) {

    }

    @Override
    public String getDuid() {
        return null;
    }

    @Override
    public void setDUid(String duid) {

    }

    @Override
    public String currentUserId() {
        return null;
    }

    @Override
    public void setUserId(String userId) {

    }

    @Override
    public String currentIp() {
        return null;
    }

    @Override
    public void setIp(String ip) {

    }
}
