package cn.zfs.blelib.data;

import android.support.annotation.Nullable;

/**
 * 描述: 单参数，字节串数据
 * 时间: 2018/5/18 14:08
 * 作者: zengfansheng
 */
public class SingleStringEvent extends SingleValueEvent<String> {
    public SingleStringEvent() {
    }

    public SingleStringEvent(int type, @Nullable Device device, String value) {
        super(type, device, value);
    }
}
