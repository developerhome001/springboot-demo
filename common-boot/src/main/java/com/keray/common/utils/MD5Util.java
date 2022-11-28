package com.keray.common.utils;


import org.springframework.util.DigestUtils;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public final class MD5Util {

    public static String MD5Encode(String origin, Charset charset) {
        return getMessageDigest(origin.getBytes(charset));
    }

    public static String MD5Encode(String origin) {
        return MD5Encode(origin, StandardCharsets.UTF_8);
    }


    public static String getMessageDigest(byte[] buffer) {
        return DigestUtils.md5DigestAsHex(buffer);
    }

}
