package cn.zfs.blelib.core;

import android.bluetooth.BluetoothDevice;
import android.support.annotation.NonNull;

/**
 * 描述: 清空绑定时过滤器
 * 时间: 2018/4/17 15:43
 * 作者: zengfansheng
 */
public interface RemoveBondFilter {
    /**
     * 是否清除此设备的配对状态
     * @return true清除，false不清除
     */
    boolean accept(@NonNull BluetoothDevice device);
}
