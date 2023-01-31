package com.keray.common.utils;

import org.apache.commons.lang3.StringUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IPUtil {
    public static final boolean PREFER_IPV6_ADDRESSES = Boolean.parseBoolean(System.getProperty("java.net.preferIPv6Addresses"));
    public static final String IPV6_START_MARK = "[";
    public static final String IPV6_END_MARK = "]";
    public static final String ILLEGAL_IP_PREFIX = "illegal ip: ";
    public static final String IP_PORT_SPLITER = ":";
    public static final int SPLIT_IP_PORT_RESULT_LENGTH = 2;
    public static final String PERCENT_SIGN_IN_IPV6 = "%";
    private static final String LOCAL_HOST_IP_V4 = "127.0.0.1";
    private static final String LOCAL_HOST_IP_V6 = "[::1]";
    private static Pattern ipv4Pattern = Pattern.compile("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
    private static final int IPV4_ADDRESS_LENGTH = 4;
    private static final int IPV6_ADDRESS_LENGTH = 16;
    private static final String CHECK_OK = "ok";

    public IPUtil() {
    }

    public static String localHostIP() {
        return PREFER_IPV6_ADDRESSES ? "[::1]" : "127.0.0.1";
    }

    public static boolean isIPv4(String addr) {
        try {
            return InetAddress.getByName(addr).getAddress().length == 4;
        } catch (UnknownHostException var2) {
            return false;
        }
    }

    public static boolean isIPv6(String addr) {
        try {
            return InetAddress.getByName(addr).getAddress().length == 16;
        } catch (UnknownHostException var2) {
            return false;
        }
    }

    public static boolean isIP(String addr) {
        try {
            InetAddress.getByName(addr);
            return true;
        } catch (UnknownHostException var2) {
            return false;
        }
    }

    public static boolean containsPort(String address) {
        return splitIPPortStr(address).length == 2;
    }

    public static String[] splitIPPortStr(String str) {
        if (StringUtils.isBlank(str)) {
            throw new IllegalArgumentException("ip and port string cannot be empty!");
        } else {
            String[] serverAddrArr;
            if (str.startsWith("[") && StringUtils.containsIgnoreCase(str, "]")) {
                if (str.endsWith("]")) {
                    serverAddrArr = new String[]{str};
                } else {
                    serverAddrArr = new String[]{str.substring(0, str.indexOf("]") + 1), str.substring(str.indexOf("]") + 2)};
                }

                if (!isIPv6(serverAddrArr[0])) {
                    throw new IllegalArgumentException("The IPv6 address(\"" + serverAddrArr[0] + "\") is incorrect.");
                }
            } else {
                serverAddrArr = str.split(":");
                if (serverAddrArr.length > 2) {
                    throw new IllegalArgumentException("The IP address(\"" + str + "\") is incorrect. If it is an IPv6 address, please use [] to enclose the IP part!");
                }

                if (!isIPv4(serverAddrArr[0])) {
                    throw new IllegalArgumentException("The IPv4 address(\"" + serverAddrArr[0] + "\") is incorrect.");
                }
            }

            return serverAddrArr;
        }
    }

    public static String getIPFromString(String str) {
        if (StringUtils.isBlank(str)) {
            return "";
        } else {
            String result = "";
            if (StringUtils.containsIgnoreCase(str, "[") && StringUtils.containsIgnoreCase(str, "]")) {
                result = str.substring(str.indexOf("["), str.indexOf("]") + 1);
                if (!isIPv6(result)) {
                    result = "";
                }
            } else {
                Matcher m = ipv4Pattern.matcher(str);
                if (m.find()) {
                    result = m.group();
                    if (!isIPv4(result)) {
                        result = "";
                    }
                }
            }

            return result;
        }
    }

    public static String checkIPs(String... ips) {
        if (ips != null && ips.length != 0) {
            StringBuilder illegalResponse = new StringBuilder();
            String[] var2 = ips;
            int var3 = ips.length;

            for(int var4 = 0; var4 < var3; ++var4) {
                String ip = var2[var4];
                if (!isIP(ip)) {
                    illegalResponse.append(ip + ",");
                }
            }

            if (illegalResponse.length() == 0) {
                return "ok";
            } else {
                return "illegal ip: " + illegalResponse.substring(0, illegalResponse.length() - 1);
            }
        } else {
            return "ok";
        }
    }

    public static boolean checkOK(String checkIPsResult) {
        return "ok".equals(checkIPsResult);
    }
}
