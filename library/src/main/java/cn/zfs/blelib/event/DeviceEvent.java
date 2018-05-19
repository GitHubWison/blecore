package cn.zfs.blelib.event;

import android.support.annotation.NonNull;

import cn.zfs.blelib.core.Device;

/**
 * 描述: 带设备的事件
 * 时间: 2018/5/19 18:57
 * 作者: zengfansheng
 */
public class DeviceEvent<D extends Device> {
    /** 设备 */
    public @NonNull D device;

    public DeviceEvent(@NonNull D device) {
        this.device = device;
    }
}
