package cn.zfs.blelib.data;

/**
 * 描述:
 * 时间: 2018/5/18 14:54
 * 作者: zengfansheng
 */
interface ObserverInfo {
    Class<?> getObserverClass();

    ObserverMethod[] getObserverMethods();

    ObserverInfo getSuperObserverInfo();

    boolean shouldCheckSuperclass();
}
