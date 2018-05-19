package cn.zfs.blelib.event;

import android.support.annotation.NonNull;

import cn.zfs.blelib.core.Device;

/**
 * 描述: onReadRemoteRssi，读取到信息强度
 * 时间: 2018/5/19 20:08
 * 作者: zengfansheng
 */
public class ReadRemoteRssiEvent<D extends Device> extends BothDeviceAndRequestIdEvent<D> {
    public int rssi;

    public ReadRemoteRssiEvent(@NonNull D device, @NonNull String requestId, int rssi) {
        super(device, requestId);
        this.rssi = rssi;
    }
}
