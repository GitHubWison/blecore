package cn.zfs.blelib.data;

/**
 * 描述:
 * 时间: 2018/5/18 16:11
 * 作者: zengfansheng
 */
interface Poster {
    void enqueue(Observation observation, Object event);
}
