package cn.zfs.blelib.data;

import android.support.annotation.Nullable;

/**
 * 描述: 单参数，长整型数据
 * 时间: 2018/5/18 14:08
 * 作者: zengfansheng
 */
public class SingleLongEvent extends SingleValueEvent<Long> {
    public SingleLongEvent() {
    }

    public SingleLongEvent(int type, @Nullable Device device, long value) {
        super(type, device, value);
    }
}
