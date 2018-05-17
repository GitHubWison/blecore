package cn.zfs.blelib.data;

import android.support.annotation.NonNull;

/**
 * 描述: 蓝牙设备状态、数据观察者，观察设备状态、数据变化
 * 时间: 2018/4/17 17:02
 * 作者: zengfansheng
 */
public class BleObserver implements IBleObserver {

    @Override
    public void onBluetoothStateChange(int state) {
        
    }

    @Override
    public void onConnectionStateChange(@NonNull Device device, int state) {

    }

    @Override
    public void onUnableConnect(Device device, String error) {

    }

    @Override
    public void onConnectTimeout(@NonNull Device device, int type) {

    }

    @Override
    public void onRssiRead(@NonNull Device device, int rssi) {

    }

    @Override
    public void onWriteCharacteristicResult(@NonNull Device device, @NonNull String requestId, boolean result, byte[] value) {

    }
}
