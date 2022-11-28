package com.keray.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.keray.common.Result;
import com.keray.common.ResultCode;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Configuration
public class SpringMVCUtil {

    private static ObjectMapper objectMapper;

    public SpringMVCUtil(ObjectMapper objectMapper) {
        SpringMVCUtil.objectMapper = objectMapper;
    }


    public static void responseWriteWith(HttpServletResponse response, ResultCode resultCode) throws IOException {
        // 设置headers
        response.setHeader("Content-Type", "application/json; charset=UTF-8");
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        // 设置body
        try {
            response.setStatus(HttpStatus.OK.value());
            response.getOutputStream().write((objectMapper.writeValueAsBytes(Result.fail(resultCode))));
        } catch (JsonProcessingException e) {
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        response.flushBuffer();
    }
}
