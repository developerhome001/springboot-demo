package com.keray.common.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.keray.common.SpringContextHolder;
import com.keray.common.utils.ImageCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.redis.core.RedisTemplate;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.time.Duration;

/**
 * 图片旋转工具类
 */
public class RotateImage {

    private static final String CODE_UUID_COOKIE_KEY = "ai_code";

    private static RedisTemplate<String, String> redisTemplate;

    static {
        RotateImage.redisTemplate = SpringContextHolder.getBean(RedisTemplate.class);
    }

    @Deprecated
    public static BufferedImage generate(HttpServletRequest request, HttpServletResponse response, java.util.List<String> urls) throws IOException {
        if (CollUtil.isEmpty(urls)) {
            throw new RuntimeException();
        }
        int angel = RandomUtil.randomInt(5, 360);
        var codeUid = "ai_code:" + RandomUtil.randomString(32);
        HttpWebUtil.addCookie(response, CODE_UUID_COOKIE_KEY, codeUid, 1200);
        String url = urls.get(RandomUtil.randomInt(0, urls.size()));
        redisTemplate.opsForValue().set(codeUid, String.valueOf(angel), Duration.ofMinutes(20));
        return ImageCode.rotate(ImageIO.read(new URL(url)), angel);
    }

    @Deprecated
    public static boolean imageCodeCheck(int angel, HttpServletRequest request) {
        var codeUid = HttpWebUtil.getCookieValue(request, CODE_UUID_COOKIE_KEY);
        if (StrUtil.isEmpty(codeUid)) return false;
        boolean result = false;
        try {
            var r = redisTemplate.opsForValue().get(codeUid);
            if (r == null) return false;
            var rightAngel = Integer.parseInt(r);
            for (int i = angel - 5; i < angel + 5 && !result; i++) {
                result = i == -rightAngel;
                result = result || i == 360 - rightAngel;
            }
            if (result) {
                redisTemplate.opsForValue().set(codeUid, "true", Duration.ofMinutes(10));
            } else {
                redisTemplate.delete(codeUid);
            }
        } catch (Exception ignore) {
        }

        return result;
    }

    @Deprecated
    public static boolean codeCheckPass(HttpServletRequest request) {
        var codeUid = HttpWebUtil.getCookieValue(request, CODE_UUID_COOKIE_KEY);
        if (StrUtil.isEmpty(codeUid)) return false;
        var redisVal = redisTemplate.opsForValue().get(codeUid);
        if (!"true".equals(redisVal)) {
            redisTemplate.delete(codeUid);
            return false;
        }
        return true;
    }

    @Deprecated
    public static String getAiCode(HttpServletRequest request) {
        return HttpWebUtil.getCookieValue(request, CODE_UUID_COOKIE_KEY);
    }


    @Getter
    @Setter
    public static class RotateImageData {
        private int angel;

        private BufferedImage image;
    }


    /**
     * 生成图片验证码
     *
     * @param urls 图片地址
     * @return 旋转后的图片
     * @throws IOException
     */
    public static RotateImageData generate(java.util.List<String> urls) throws IOException {
        if (CollUtil.isEmpty(urls)) {
            throw new RuntimeException();
        }
        int angel = RandomUtil.randomInt(5, 360);
        String url = urls.get(RandomUtil.randomInt(0, urls.size()));
        var image = ImageCode.rotate(ImageIO.read(new URL(url)), angel);
        var result = new RotateImageData();
        result.setAngel(angel);
        result.setImage(image);
        return result;
    }

    /**
     * 校验验证码角度
     *
     * @param rightAngel 正确的旋转角度
     * @param inputAngel 输入的旋转角度
     * @return
     */
    public static boolean imageCodeCheck(int rightAngel, int inputAngel) {
        for (int i = inputAngel - 5; i < inputAngel + 5; i++) {
            if (i == -rightAngel || i == 360 - rightAngel) return true;
        }
        return false;
    }
}
