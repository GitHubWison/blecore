package cn.zfs.blelib.data;

import android.support.annotation.Nullable;

/**
 * 描述: 单值事件
 * 时间: 2018/5/18 09:06
 * 作者: zengfansheng
 */
public class SingleValueEvent<T> extends Event {
    /** 该事件的数据 */
    public T value;

    public SingleValueEvent() {}

    public SingleValueEvent(int type, @Nullable Device device, T value) {
        super(type, device);
        this.value = value;
    }
}
