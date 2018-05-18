package cn.zfs.blelib.data;

import android.support.annotation.Nullable;

/**
 * 描述: 单参数，boolean数据
 * 时间: 2018/5/18 14:08
 * 作者: zengfansheng
 */
public class SingleBooleanEvent extends SingleValueEvent<Boolean> {
    public SingleBooleanEvent() {
    }

    public SingleBooleanEvent(int type, @Nullable Device device, boolean value) {
        super(type, device, value);
    }
}
