package cn.zfs.blelib.core;

import java.lang.reflect.Method;

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
}
