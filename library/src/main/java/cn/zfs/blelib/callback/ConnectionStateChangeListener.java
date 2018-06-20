package cn.zfs.blelib.callback;

import android.support.annotation.NonNull;

import cn.zfs.blelib.core.Connection;
import cn.zfs.blelib.core.Device;

/**
 * 描述: 蓝牙连接状态回调
 * 时间: 2018/6/15 01:00
 * 作者: zengfansheng
 */
public interface ConnectionStateChangeListener {
    
    /**
     * 连接状态变化
     * @param device 设备。device.connectionState: 连接状态<br> {@link Connection#STATE_DISCONNECTED}<br> {@link Connection#STATE_CONNECTING}<br>
     *              {@link Connection#STATE_RECONNECTING}<br> {@link Connection#STATE_CONNECTED}<br>
     *              {@link Connection#STATE_SERVICE_DISCOVERING}<br> {@link Connection#STATE_SERVICE_DISCOVERED}
     */
    void onConnectionStateChanged(@NonNull Device device);
}
