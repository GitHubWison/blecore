package cn.zfs.blelib.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 描述: 工具类
 * 时间: 2018/4/24 15:27
 * 作者: zengfansheng
 */
public class BleUtils {
    /**
     * byte数组转换成16进制字符串
     * @param src 源
     */
    public static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder();
        if (src == null || src.length <= 0) {
            return "";
        }
        for (byte aSrc : src) {
            int v = aSrc & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
            stringBuilder.append(" ");
        }
        return stringBuilder.toString().toUpperCase();
    }

    /**
     * 反射调用方法，只适用一个参数的，无返回值
     * @param method 方法名
     * @param cls 参数字节码
     * @param obj 方法所属实例
     * @param args 参数实例
     */
    public static <T> void invoke(String method, Class<T> cls, Object obj, Object... args) {
        try {
            Method m = obj.getClass().getDeclaredMethod(method, cls);
            m.setAccessible(true);
            m.invoke(obj, args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 反射调用方法，只适用无参，返回值
     * @param method 方法名
     * @param obj 方法所属实例
     */
    public static <T> T invoke(String method, Object obj) {
        try {
            Method m = obj.getClass().getDeclaredMethod(method);
            m.setAccessible(true);
            return (T) m.invoke(obj);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 将整数转字节数组
     * @param value 整数，short、int、long
     * @param bigAhead ture表示高位在前，false表示低位在前
     * @return 返回一个8字节的数组，需要几位自取
     */
    public static byte[] numberToBytes(long value, boolean bigAhead) {
        byte[] bytes = new byte[8];
        for (int i = 0; i < 8; i++) {
            int j = bigAhead ? 7 - i : i;
            bytes[i] = (byte) ((value >> (8 * j)) & 0xFF);
        }
        return bytes;
    }

    /**
     * 将字节数组转long数值
     * @param src 待转字节数组
     * @param bigAhead ture表示高位在前，false表示低位在前
     */
    public static long bytesToLong(byte[] src, boolean bigAhead) {
        long value = 0;
        for (int i = 0; i < 8 && i < src.length; i++) {
            int j = bigAhead ? 7 - i : i;
            value |= ((src[i] & 0xFF) << (8 * j));
        }
        return value;
    }

    /**
     * 分包
     * @param src 源
     * @param size 包大小，字节
     * @return 分好的包的集合
     */
    public static List<byte[]> splitPackage(byte[] src, int size) {
        List<byte[]> list = new ArrayList<>();
        int loopCount = src.length / size + (src.length % size == 0 ? 0 : 1);
        for (int j = 0; j < loopCount; j++) {
            int from = j * size;
            int to = Math.min(src.length, from + size);
            list.add(Arrays.copyOfRange(src, j * size, to));
        }
        return list;
    }

    /**
     * 合包
     * @param src 源
     * @return 合好的字节数组
     */
    public static byte[] joinPackage(byte[]... src) {
        byte[] bytes = new byte[0];
        if (src != null) {
            for (byte[] bs : src) {
                bytes = Arrays.copyOf(bytes, bytes.length + bs.length);
                System.arraycopy(bs, 0, bytes, bytes.length - bs.length, bs.length);
            }
        }
        return bytes;
    }
}
