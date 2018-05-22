package cn.zfs.blelib.event;

import android.support.annotation.NonNull;

import cn.zfs.blelib.core.Connection;
import cn.zfs.blelib.core.Device;

/**
 * 描述: 连接超时
 * 时间: 2018/5/19 19:51
 * 作者: zengfansheng
 */
public class ConnectTimeoutEvent extends DeviceEvent<Device> {
    /**
     * 设备连接超时。可能的值：
     * <br>{@link Connection#TIMEOUT_TYPE_CANNOT_DISCOVER_DEVICE}
     * <br>{@link Connection#TIMEOUT_TYPE_CANNOT_CONNECT}
     * <br>{@link Connection#TIMEOUT_TYPE_CANNOT_DISCOVER_SERVICES}
     */
    public int type;

    public ConnectTimeoutEvent(@NonNull Device device, int type) {
        super(device);
        this.type = type;
    }
}
