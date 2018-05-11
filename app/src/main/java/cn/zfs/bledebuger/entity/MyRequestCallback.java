package cn.zfs.bledebuger.entity;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.support.annotation.NonNull;

import cn.zfs.blelib.core.Ble;
import cn.zfs.blelib.data.Device;
import cn.zfs.blelib.core.Request;
import cn.zfs.blelib.callback.RequestCallback;

/**
 * 描述:
 * 时间: 2018/4/27 14:05
 * 作者: zengfansheng
 */
public class MyRequestCallback extends RequestCallback {
    public MyRequestCallback(@NonNull Device device) {
        super(device);
    }

    @Override
    public void onRequestFialed(String requestId, Request.RequestType requestType, int failType, byte[] value) {
        super.onRequestFialed(requestId, requestType, failType, value);
        MyBleObservable observable = (MyBleObservable) Ble.getInstance().getObservable();
        observable.notifyRequestFialed(requestId, requestType, failType);
    }

    @Override
    public void onNotificationRegistered(String requestId, BluetoothGattDescriptor descriptor) {
        super.onNotificationRegistered(requestId, descriptor);
        MyBleObservable observable = (MyBleObservable) Ble.getInstance().getObservable();
        observable.notifyNotificationRegistered(requestId, descriptor);
    }

    @Override
    public void onCharacteristicChanged(BluetoothGattCharacteristic characteristic) {
        MyBleObservable observable = (MyBleObservable) Ble.getInstance().getObservable();
        observable.notifyCharacteristicChanged(characteristic);
    }

    @Override
    public void onNotificationUnregistered(String requestId, BluetoothGattDescriptor descriptor) {
        super.onNotificationUnregistered(requestId, descriptor);
        MyBleObservable observable = (MyBleObservable) Ble.getInstance().getObservable();
        observable.onNotificationUnregistered(requestId, descriptor);
    }
}
