package com.keray.common.utils;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.annotation.TableField;
import com.keray.common.entity.IBEntity;
import com.keray.common.service.model.base.BaseTreeModel;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.*;
import java.util.function.Function;

/**
 * @author by keray
 * date:2019/12/4 2:14 PM
 */
@Slf4j
public final class CommonUtil {
    /**
     * <p>
     * <h3>作者 keray</h3>
     * <h3>时间： 2019/12/4 2:15 PM</h3>
     * 寻找某个class是否有某个注解 包括向上的接口，父类
     * </p>
     *
     * @param clazz
     * @param annotation
     * @return <p> {@link boolean} </p>
     * @throws
     */
    public static boolean classAllSuperHaveAnnotation(Class<?> clazz, Class<? extends Annotation> annotation) {
        if (clazz.getAnnotation(annotation) != null) {
            return true;
        }
        if (clazz.getSuperclass() == null) {
            return false;
        }
        return classAllSuperHaveAnnotation(clazz.getSuperclass(), annotation);
    }

    /**
     * 递归向上找class存在的注解
     *
     * @param clazz      类
     * @param annotation 查找的注解
     * @param <A>
     * @return
     */
    public static <A extends Annotation> A getClassAllAnnotation(Class<?> clazz, Class<A> annotation) {
        if (clazz.getAnnotation(annotation) != null) {
            return clazz.getAnnotation(annotation);
        }
        if (clazz.getSuperclass() == null) {
            return null;
        }
        return getClassAllAnnotation(clazz.getSuperclass(), annotation);
    }

    /**
     * 递归向上找出类存在某个字段，包含private字段
     *
     * @param clazz
     * @param fieldName
     * @return
     * @throws NoSuchFieldException
     */
    public static Field getClassField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException ignore) {
        }
        if (clazz.getSuperclass() == null) {
            throw new NoSuchFieldException(fieldName);
        }
        return getClassField(clazz.getSuperclass(), fieldName);
    }

    /**
     * 递归判断类是否存继承某个父类
     *
     * @param owner
     * @param target
     * @return
     */
    public static Class<?> superHaveClass(Class<?> owner, Class<?> target) {
        if (owner.equals(Object.class)) {
            return null;
        }
        if (owner.getSuperclass().equals(target)) return owner;
        return superHaveClass(owner.getSuperclass(), target);
    }

    /**
     * 将double类型的数字保留两位
     *
     * @param num
     * @return
     */
    public static BigDecimal moneyTrans(double num) {
        return BigDecimal.valueOf((long) (num * 100), 2);
    }

    /**
     * 将mybatis-plus的model类存在不为null的字段设置为field = value的sql语句
     *
     * @param model
     * @return
     */
    public static String mybatisPluginsModel2Sql(IBEntity<?> model) {
        StringJoiner stringJoiner = new StringJoiner("and");
        List<Field> fields = model.scanFields(model.getClass(), null);
        int index = 0;
        for (Field field : fields) {
            TableField tableField = field.getAnnotation(TableField.class);
            if (tableField != null && !tableField.exist()) {
                continue;
            }
            try {
                Method get = model.scanMethod(model.getClass(), "get" + StrUtil.upperFirst(field.getName()));
                if (get == null) {
                    continue;
                }
                Object result = get.invoke(model);
                if (ObjectUtil.isNotEmpty(result)) {
                    if (tableField == null) {
                        stringJoiner.add(String.format(" `%s` = {%s}", field.getName(), index++));
                    }
                }
            } catch (Exception e) {
                log.error("not here", e);
            }
        }
        return stringJoiner.toString();
    }

    /**
     * 将mybatis-plus的model类中不为null的字段返回值
     *
     * @param model
     * @return
     */
    public static Object[] mybatisPluginsModel2SqlValue(IBEntity<?> model) {
        List<Object> values = new LinkedList<>();
        List<Field> fields = model.scanFields(model.getClass(), null);
        for (Field field : fields) {
            TableField tableField = field.getAnnotation(TableField.class);
            if (tableField != null && !tableField.exist()) {
                continue;
            }
            try {
                Method get = model.scanMethod(model.getClass(), "get" + StrUtil.upperFirst(field.getName()));
                if (get == null) {
                    continue;
                }
                Object result = get.invoke(model);
                if (ObjectUtil.isNotEmpty(result)) {
                    values.add(result);
                }
            } catch (Exception e) {
                log.error("not here", e);
            }
        }
        return values.toArray(new Object[0]);
    }

    /**
     * 获取主机的ip
     *
     * @return
     */
    public static String hostIp(boolean v4) {
        try {
            Enumeration<NetworkInterface> allNetInterfaces = NetworkInterface.getNetworkInterfaces();
            InetAddress ip;
            while (allNetInterfaces.hasMoreElements()) {
                NetworkInterface netInterface = allNetInterfaces.nextElement();
                Enumeration<InetAddress> addresses = netInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    ip = addresses.nextElement();
                    if (ip instanceof Inet4Address && !v4) continue;
                    if (ip instanceof Inet6Address && v4) continue;
                    String retIp = ip.getHostAddress();
                    if (retIp.startsWith("192.168")) {
                        return retIp;
                    }
                    if (retIp.startsWith("172.")) {
                        return retIp;
                    }
                    if (retIp.startsWith("10.")) {
                        return retIp;
                    }
                    if ("127.0.0.1".equals(retIp) || "::1".equals(retIp)) continue;
                    return retIp;
                }
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 影藏手机号的中间4位
     *
     * @param mobile
     * @return
     */
    public static String mobileSafe(String mobile) {
        if (StrUtil.isBlank(mobile) || mobile.length() != 11) return null;
        return mobile.substring(0, 3) + "****" + mobile.substring(7, 11);
    }

    /**
     * 将树形结构的集合转换为树形结构
     *
     * @param list
     * @param <ID>
     * @param <T>
     * @return
     */
    public static <ID extends Serializable, T extends BaseTreeModel<T, ID>> List<T> listData2Tree(List<T> list) {
        var parents = list.stream().filter(v -> list.stream().noneMatch(v1 -> v1.getId().equals(v.getParentId()))).toList();
        for (var parent : parents) {
            listData2TreeParentProcess(parent, list);
        }
        return parents;
    }

    /**
     * 将树形结构的集合转换为树形结构
     *
     * @param parent
     * @param list
     * @param <ID>
     * @param <T>
     */
    public static <ID extends Serializable, T extends BaseTreeModel<T, ID>> void listData2TreeParentProcess(T parent, List<T> list) {
        parent.setChildren(list.stream().filter(v -> parent.getId().equals(v.getParentId())).toList());
        for (var child : parent.getChildren()) {
            listData2TreeParentProcess(child, list);
        }
    }

    /**
     * 洗牌算法去随机集合
     *
     * @param source
     * @param count
     * @param <T>
     * @return
     */
    public static <T> List<T> randomEleList(List<T> source, int count) {
        if (count >= source.size()) {
            return ListUtil.toList(source);
        }
        final int[] randomList = ArrayUtil.sub(randomInts(source.size(), count), 0, count);
        List<T> result = new ArrayList<>();
        for (int e : randomList) {
            result.add(source.get(e));
        }
        return result;
    }

    /**
     * 给定最大值随机生成指定长度的index
     *
     * @param length
     * @param limit
     * @return
     */
    public static int[] randomInts(int length, int limit) {
        final int[] range = ArrayUtil.range(length);
        for (int i = 0; i < limit; i++) {
            int random = RandomUtil.randomInt(i, length);
            ArrayUtil.swap(range, i, random);
        }
        return range;
    }

    /**
     * 二分查询数组中是否存在指定的值
     *
     * @param arr
     * @param target
     * @return
     */
    public static <T extends Comparable<T>> boolean binarySearch(T[] arr, T target) {
        int start = 0;
        int end = arr.length - 1;
        while (start <= end) {
            int middle = (start + end) / 2;
            if (target.compareTo(arr[middle]) < 0) {
                end = middle - 1;
            } else if (target.compareTo(arr[middle]) > 0) {
                start = middle + 1;
            } else {
                return true;
            }
        }
        return false;
    }

    /**
     * 公平随机算法，保证每个位置都在总数/位置的均值左右
     *
     * @param sum
     * @param size
     * @param min   低保数字
     * @param cover
     * @param <T>
     * @return
     */
    public static <T extends Number & Comparable<T>> ArrayList<T> randomDistribution(double sum, int size, double min, Function<Double, T> cover) {
        if (sum < size) throw new RuntimeException();
        ArrayList<T> result = new ArrayList<>(size * 2);
        var balance = sum;
        for (var i = 0; i < size - 1; i++) {
            // 2 * 余额/剩余坑位  2 * 2 / 2 = 2 3 * 2/ 2 = 3
            var k = 2 * balance / (size - i);
            var random = RandomUtil.randomDouble(min, k);
            result.set(i, cover.apply(balance));
            balance -= random;
        }
        result.set(size - 1, cover.apply(balance));
        return result;
    }

}
