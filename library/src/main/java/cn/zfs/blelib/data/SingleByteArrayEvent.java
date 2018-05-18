package cn.zfs.blelib.data;

import android.support.annotation.Nullable;

/**
 * 描述: 单参数，字节数组数据
 * 时间: 2018/5/18 14:08
 * 作者: zengfansheng
 */
public class SingleByteArrayEvent extends SingleValueEvent<byte[]> {
    public SingleByteArrayEvent() {
    }

    public SingleByteArrayEvent(int type, @Nullable Device device, byte[] value) {
        super(type, device, value);
    }
}
