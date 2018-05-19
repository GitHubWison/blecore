package cn.zfs.blelib.event;

import android.support.annotation.NonNull;

import cn.zfs.blelib.core.Device;

/**
 * 描述: onMtuChanged，MTU修改成功
 * 时间: 2018/5/19 20:10
 * 作者: zengfansheng
 */
public class MtuChangedEvent<D extends Device> extends BothDeviceAndRequestIdEvent<D> {
    /** 新的MTU值 */
    public int mtu;

    public MtuChangedEvent(@NonNull D device, @NonNull String requestId, int mtu) {
        super(device, requestId);
        this.mtu = mtu;
    }
}
