package cn.zfs.blelib.data;

import android.support.annotation.Nullable;

/**
 * 描述: 单参数，short数据
 * 时间: 2018/5/18 14:08
 * 作者: zengfansheng
 */
public class SingleShortEvent extends SingleValueEvent<Short> {
    public SingleShortEvent() {
    }

    public SingleShortEvent(int type, @Nullable Device device, short value) {
        super(type, device, value);
    }
}
