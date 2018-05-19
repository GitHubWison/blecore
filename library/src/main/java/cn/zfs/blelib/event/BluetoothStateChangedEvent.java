package cn.zfs.blelib.event;

import android.bluetooth.BluetoothAdapter;

/**
 * 描述: 蓝牙状态变化
 * 时间: 2018/5/19 19:24
 * 作者: zengfansheng
 */
public class BluetoothStateChangedEvent {
    /**
     * 当前状态。可能的值：
     * <br>{@link BluetoothAdapter#STATE_OFF}
     * <br>{@link BluetoothAdapter#STATE_TURNING_ON}
     * <br>{@link BluetoothAdapter#STATE_ON}
     * <br>{@link BluetoothAdapter#STATE_TURNING_OFF}
     */
    public int state;

    public BluetoothStateChangedEvent(int state) {
        this.state = state;
    }
}
