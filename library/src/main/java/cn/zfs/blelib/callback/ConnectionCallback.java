package cn.zfs.blelib.callback;

import android.support.annotation.NonNull;

import cn.zfs.blelib.core.Connection;
import cn.zfs.blelib.core.Device;

/**
 * 描述: 连接状态变化回调
 * 时间: 2018/4/24 08:31
 * 作者: zengfansheng
 */
public abstract class ConnectionCallback {
    protected Device device;

    public ConnectionCallback(@NonNull Device device) {
        this.device = device;
    }

    /**
     * 连接状态变化
     * @param state 连接状态<br> {@link Connection#STATE_DISCONNECTED}<br> {@link Connection#STATE_CONNECTING}<br>
     *              {@link Connection#STATE_RECONNECTING}<br> {@link Connection#STATE_CONNECTED}<br>
     *              {@link Connection#STATE_SERVICE_DISCORVERING}<br> {@link Connection#STATE_SERVICE_DISCORVERED}
     */
    public abstract void onConnectionStateChange(@Connection.STATE int state);
}
