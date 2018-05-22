package cn.zfs.blelib.event;

import android.support.annotation.NonNull;

import cn.zfs.blelib.core.Connection;
import cn.zfs.blelib.core.Device;

/**
 * 描述: 连接状态变化
 * 时间: 2018/5/19 19:42
 * 作者: zengfansheng
 */
public class ConnectionStateChangedEvent extends DeviceEvent<Device> {
    /**
     * 当前连接状态。可能的值：
     * <br>{@link Connection#STATE_DISCONNECTED} 
     * <br>{@link Connection#STATE_CONNECTING}
     * <br>{@link Connection#STATE_RECONNECTING} 
     * <br>{@link Connection#STATE_CONNECTED}
     * <br>{@link Connection#STATE_SERVICE_DISCORVERING}
     * <br>{@link Connection#STATE_SERVICE_DISCORVERED}
     * <br>{@link Connection#STATE_RELEASED}
     */
    public int state;

    public ConnectionStateChangedEvent(@NonNull Device device, int state) {
        super(device);
        this.state = state;
    }
}
