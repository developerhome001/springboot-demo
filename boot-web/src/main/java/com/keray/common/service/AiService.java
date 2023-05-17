package com.keray.common.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.ECKeyUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.SM2;
import com.alibaba.fastjson2.JSON;
import com.keray.common.diamond.Diamond;
import com.keray.common.util.HttpWebUtil;
import com.keray.common.util.RotateImage;
import com.keray.common.utils.MD5Util;
import org.bouncycastle.crypto.engines.SM2Engine;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class AiService {

    @Diamond("ai.code.url")
    private List<String> codeUrls = List.of("https://cdn.caishi.cn/cdn/191b0fda3839194f6128b9df2d54dabd.jpeg");

    public void setCodeUrls(String json) {
        if (StrUtil.isEmpty(json)) return;
        List<String> ls = JSON.parseArray(json, String.class);
        if (ls.isEmpty()) codeUrls = List.of("https://cdn.caishi.cn/cdn/191b0fda3839194f6128b9df2d54dabd.jpeg");
        else codeUrls = ls;
    }

    private static final SM2 sm2;

    private static final ECPrivateKeyParameters ecPrivateKeyParameters;
    private static final ECPublicKeyParameters ecPublicKeyParameters;


    private static final String publicKey = "045eb9fa522f45e838c2544df64431ad8b4c03495ffa1b37a28fd6c0b18fd1499e1bcaf7661d4dcb2c44b84d639d8f7c41b2614a7b59bf606827e3ece9ca4c278c";
    private static final String privateKey = "0087dc29281aa53e100200a887ef54e7bea9e99be0a8eccc5759724482d9ce1994";


    static {
        ecPrivateKeyParameters = ECKeyUtil.decodePrivateKeyParams(SecureUtil.decode(privateKey));
        ecPublicKeyParameters = ECKeyUtil.decodePublicKeyParams(SecureUtil.decode(publicKey));
        sm2 = new SM2(ecPrivateKeyParameters, ecPublicKeyParameters);
        sm2.setMode(SM2Engine.Mode.C1C2C3);
    }

    private static final String aiCodeSign = "xfvfdxhvsiqjq";


    @Resource
    private RedisTemplate<String, Object> redisTemplate;


    private final static String AI_CODE = "qps-token";

    /**
     * 校验aiCode是否合法
     */
    public String aiCodeCheck(HttpServletRequest request, String ip, String duid) {
        var aiCode = HttpWebUtil.getCookieValue(request, AI_CODE);
        if (StrUtil.isNotBlank(aiCode)) {
            var str = sm2.decryptStr(aiCode, KeyType.PrivateKey).split("_");
            var now = System.currentTimeMillis();
            var time = Long.parseLong(str[0]);
            // 如果aicode超过305(实际有效期300秒)秒有效期就丢弃
            return now - time <= 305_000 && str[1].equals(ip) && str[2].equals(duid) && str[3].equals(aiCodeSign) ? aiCode : null;
        }
        return null;
    }

    /**
     * 生成的code时效性5分钟
     */
    public String generateAiCode(String ip, String duid) {
        return sm2.encryptBase64(String.format("%d_%s_%s_%s", System.currentTimeMillis(), ip, duid, aiCodeSign), KeyType.PublicKey);
    }


    /**
     * 生成的code无时效性
     */
    public String generateAiCodeLongTime(String ip, String duid) {
        return MD5Util.MD5Encode(String.format("%s_%s_%s", ip, duid, aiCodeSign));
    }


    /**
     * 生成旋转图片验证
     *
     * @return
     * @throws IOException
     */
    public BufferedImage generateRotateImage(String ip, String duid) throws IOException {
        var result = RotateImage.generate(codeUrls);
        var key = MD5Util.MD5Encode(String.format("%s_%s", ip, duid));
        redisTemplate.opsForValue().set("AI_CODE_ANGEL:" + key, result.getAngel(), 1, TimeUnit.MINUTES);
        return result.getImage();
    }

    /**
     * 校验旋转图片验证
     *
     * @param angel 用户输入旋转角度
     * @param ip    用户ip
     * @param duid  用户duid
     * @return 校验通过后会把key返回，否则返回null
     */
    public boolean checkRotateImage(int angel, String ip, String duid, HttpServletResponse response) {
        var key = MD5Util.MD5Encode(String.format("%s_%s", ip, duid));
        var rightAngel = (Integer) redisTemplate.opsForValue().get("AI_CODE_ANGEL:" + key);
        if (rightAngel == null) return false;
        if (RotateImage.imageCodeCheck(rightAngel, angel)) {
            // 保存aiCode 有效期5分钟
            writeAiCode(response, generateAiCode(ip, duid));
            return true;
        }
        // 验证失败删除缓存 防止暴力破解 前端重新获取
        redisTemplate.delete(key);
        return false;
    }

    public void writeAiCode(HttpServletResponse response, String aiCode) {
        HttpWebUtil.addCookie(response, AI_CODE, aiCode, 300);
    }
}
