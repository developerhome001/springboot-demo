package com.keray.common.util;

import org.apache.http.protocol.UriPatternMatcher;

public class MoreUriPatternMatcher<T> extends UriPatternMatcher<T> {
    @Override
    protected boolean matchUriRequestPattern(String pattern, String path) {
        if (super.matchUriRequestPattern(pattern, path)) return true;
        // 扩展中间*表达式 比如/api/user/*/detail  匹配到/api/user/863/detail
        // 比如/api/user/vip*/detail  匹配到/api/user/vip863/detail
        // 比如/api/user/vip*00/detail  匹配到/api/user/vip86300/detail

        // 如果正则匹配
        if (pattern.startsWith("~")) {
            return path.matches(pattern.substring(1));
        }
        var ps = pattern.split("/");
        var us = path.split("/");
        for (int i = 0, u = 0; i < ps.length && u < us.length; i++, u++) {
            var pi = ps[i];
            var ui = us[u];
            var r = true;
            if (!pi.contains("*")) {
                r = ui.equals(pi);
            }
            // 如果是* 前缀 后缀匹配时直接使用父类的匹配
            else if (pi.startsWith("*") || pi.endsWith("*")) {
                r = super.matchUriRequestPattern(pi, ui);
            }
            // 中间匹配时
            else {
                var s = pi.split("\\*");
                r = ui.startsWith(s[0]) && ui.endsWith(s[1]);
            }
            if (!r) return false;
        }
        return true;
    }

    public static void main(String[] args) {
        var matcher = new MoreUriPatternMatcher<Integer>();
//        matcher.register("~/api/user/vip[\\d]{4}00/detail", 1);
//        matcher.register("/api/user/vip*00/detail", 2);
        matcher.register("/api/user/*/detail", 4);
//        matcher.register("/api/user/vip*/detail", 5);
//        matcher.register("/api/user/*00/detail", 6);
//        matcher.register("/api/user/vip111100/detail", 7);

        System.out.println(matcher.lookup("/api/user/vip111100/detail"));
    }
}
