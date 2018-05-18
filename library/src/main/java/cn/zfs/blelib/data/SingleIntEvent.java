package cn.zfs.blelib.data;

import android.support.annotation.Nullable;

/**
 * 描述: 单参数，整型数据
 * 时间: 2018/5/18 14:08
 * 作者: zengfansheng
 */
public class SingleIntEvent extends SingleValueEvent<Integer> {
    public SingleIntEvent() {
    }

    public SingleIntEvent(int type, @Nullable Device device, int value) {
        super(type, device, value);
    }
}
