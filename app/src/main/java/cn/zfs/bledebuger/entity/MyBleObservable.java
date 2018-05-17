package cn.zfs.bledebuger.entity;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.os.Looper;

import cn.zfs.blelib.core.Request;
import cn.zfs.blelib.data.BleObservable;

/**
 * 描述:
 * 时间: 2018/4/27 14:03
 * 作者: zengfansheng
 */
public class MyBleObservable extends BleObservable {
    public MyBleObservable(Looper looper) {
        super(looper);
    }

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
    public void notifyNotificationUnregistered(String requestId, BluetoothGattDescriptor descriptor) {
        for (Object o : getObservers()) {
            ((MyBleObserver) o).onNotificationUnregistered(requestId, descriptor);
        }
    }

    /**
     * 读取到值
     * @param requestId 请求ID
     */
    public void notifyCharacteristicRead(String requestId, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        for (Object o : getObservers()) {
            ((MyBleObserver) o).onCharacteristicRead(requestId, gatt, characteristic);
        }
    }
}
