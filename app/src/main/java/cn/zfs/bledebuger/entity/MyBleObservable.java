package cn.zfs.bledebuger.entity;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;

import cn.zfs.blelib.data.BleObservable;
import cn.zfs.blelib.core.Request;

/**
 * 描述:
 * 时间: 2018/4/27 14:03
 * 作者: zengfansheng
 */
public class MyBleObservable extends BleObservable {
    public void notifyNotificationRegistered(String requestId, BluetoothGattDescriptor descriptor) {
        for (Object o : getObservers()) {
            ((MyBleObserver) o).onNotificationRegistered(requestId, descriptor);
        }
    }

    public void notifyRequestFialed(String requestId, Request.RequestType requestType, int failType) {
        for (Object o : getObservers()) {
            ((MyBleObserver) o).onRequestFialed(requestId, requestType, failType);
        }
    }

    /**
     * 收到设备notify值 （设备上报值）
     */
    public void notifyCharacteristicChanged(BluetoothGattCharacteristic characteristic) {
        for (Object o : getObservers()) {
            ((MyBleObserver) o).onCharacteristicChanged(characteristic);
        }
    }

    /**
     * Notification关闭成功
     * @param requestId 请求ID
     */
    public void onNotificationUnregistered(String requestId, BluetoothGattDescriptor descriptor) {
        for (Object o : getObservers()) {
            ((MyBleObserver) o).onNotificationUnregistered(requestId, descriptor);
        }
    }
}
