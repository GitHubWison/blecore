package cn.zfs.blelib.data;

import android.support.annotation.Nullable;

/**
 * 描述: 单参数，字节数据
 * 时间: 2018/5/18 14:08
 * 作者: zengfansheng
 */
public class SingleByteEvent extends SingleValueEvent<Byte> {
    public SingleByteEvent() {
    }

    public SingleByteEvent(int type, @Nullable Device device, byte value) {
        super(type, device, value);
    }
}
