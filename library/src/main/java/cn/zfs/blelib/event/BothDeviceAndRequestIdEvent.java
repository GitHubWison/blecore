package cn.zfs.blelib.event;

import android.support.annotation.NonNull;

import cn.zfs.blelib.core.Device;

/**
 * 描述: 带请求ID和设备的事件
 * 时间: 2018/5/19 19:09
 * 作者: zengfansheng
 */
public class BothDeviceAndRequestIdEvent<D extends Device> {
    /** 设备 */
    public @NonNull D device;
    @NonNull
    public String requestId;

    public BothDeviceAndRequestIdEvent(@NonNull D device, @NonNull String requestId) {
        this.device = device;
        this.requestId = requestId;
    }
}
