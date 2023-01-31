package com.keray.common.utils;

import cn.hutool.core.net.Ipv4Util;
import com.googlecode.ipv6.IPv6Address;
import com.keray.common.exception.BizRuntimeException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;
import java.util.List;

@Slf4j
public class IpAuthUtil {
    public enum IpType {
        //
        ipv4("ipv4", 1),
        ipv6("ipv6", 2),
        ;

        @Getter
        String desc;

        @Getter
        Integer code;

        IpType(String desc, Integer code) {
            this.desc = desc;
            this.code = code;
        }

    }

    @Getter
    @Setter
    public static class IpAuthData {

        private byte[] val;

        private byte[] min;

        private byte[] max;

        private Integer omask;

        private IpType type;

        public void setValBigInteger(BigInteger intVal) {
            this.val = bigInter2Bytes(intVal);
        }


        public void setMinVal(BigInteger intVal) {
            this.min = bigInter2Bytes(intVal);
        }


        public void setMaxVal(BigInteger intVal) {
            this.max = bigInter2Bytes(intVal);
        }

        public static byte[] bigInter2Bytes(BigInteger intVal) {
            var bytes = intVal.toByteArray();
            var len = bytes.length;
            var val = new byte[16];
            System.arraycopy(bytes, 0, val, 16 - len, len);
            return val;
        }

    }

    /**
     * 将ip表达字符串转换为ip段数据
     * 表达方式支持
     * 192.168.1.101 单ip表示
     * 192.168.1.101-192.168.1.120 ip范围表示
     * 192.168.1.101/24 ip段表示
     * ipv6一致
     *
     * @param ipStr
     * @return
     */
    public static IpAuthData ipToVal(String ipStr) {
        try {
            if (ipStr.contains("/")) {
                var s = ipStr.split("/");
                var ip = s[0];
                var ipType = IPUtil.isIPv4(ip) ? IpType.ipv4 : IpType.ipv6;
                var mask = Integer.parseInt(s[1]);
                var omask = (ipType == IpType.ipv4 ? 32 : 128) - mask;
                BigInteger val;
                if (ipType == IpType.ipv4) {
                    val = BigInteger.valueOf(Ipv4Util.ipv4ToLong(ip));
                } else {
                    val = IPv6Address.fromString(ip).toBigInteger();
                }
                var or = BigInteger.valueOf(0);
                for (var i = 0; i < omask; i++) {
                    or = or.shiftLeft(1).or(BigInteger.valueOf(1));
                }
                var min = val.shiftRight(omask).shiftLeft(omask);
                return ipToVal(val, min, min.or(or), omask, ipType);
            } else if (ipStr.contains("-")) {
                var s = ipStr.split("-");
                var start = s[0];
                var end = s[1];
                var ipType = IPUtil.isIPv4(start) ? IpType.ipv4 : IpType.ipv6;
                int omask = 0;
                BigInteger min;
                BigInteger max;
                if (ipType == IpType.ipv4) {
                    min = BigInteger.valueOf(Ipv4Util.ipv4ToLong(start));
                    max = BigInteger.valueOf(Ipv4Util.ipv4ToLong(end));
                } else {
                    min = IPv6Address.fromString(start).toBigInteger();
                    max = IPv6Address.fromString(end).toBigInteger();
                }
                for (var i = 0; i < 128; i++) {
                    if (min.shiftRight(i).equals(max.shiftRight(i))) {
                        omask = i;
                        break;
                    }
                }
                return ipToVal(min, min, max, omask, ipType);
            } else {
                var ipType = IPUtil.isIPv4(ipStr) ? IpType.ipv4 : IpType.ipv6;
                BigInteger max;
                if (ipType == IpType.ipv4) {
                    max = BigInteger.valueOf(Ipv4Util.ipv4ToLong(ipStr));
                } else {
                    max = IPv6Address.fromString(ipStr).toBigInteger();
                }
                return ipToVal(max, max, max, 0, ipType);
            }
        } catch (Exception e) {
            log.error("ip解析失败：", e);
            throw new BizRuntimeException("ip格式错误:");
        }
    }

    public static IpAuthData ipToVal(BigInteger val, BigInteger sip, BigInteger eip, int otherMask, IpType ipType) {
        var result = new IpAuthData();
        result.setMinVal(sip);
        result.setMaxVal(eip);
        result.setValBigInteger(val.shiftRight(otherMask).shiftLeft(otherMask));
        result.setOmask(otherMask);
        result.setType(ipType);
        return result;
    }

    /**
     * 判断ip是否在给定的ip范围中
     *
     * @param ip
     * @param ips
     * @return
     */
    public static boolean ipInIps(String ip, List<String> ips) {
        var ipv4 = IPUtil.isIPv4(ip);
        var val = ipv4 ? BigInteger.valueOf(Ipv4Util.ipv4ToLong(ip)) : IPv6Address.fromString(ip).toBigInteger();
        var res = false;
        for (var s : ips) {
            var data = IpAuthUtil.ipToVal(s);
            if (ipv4 != (data.getType() == IpType.ipv4)) continue;
            var value = new BigInteger(data.getVal());
            var min = new BigInteger(data.getMin());
            var max = new BigInteger(data.getMax());
            var omask = data.getOmask();
            // 比较是否在同一网段
            res = value.shiftRight(omask).equals(val.shiftRight(omask));
            // 比较ip是否大于最小ip
            res = res && min.compareTo(val) <= 0;
            // 比较ip是否小于最小ip
            res = res && max.compareTo(val) >= 0;
            if (res) return true;
        }
        return false;
    }
}
