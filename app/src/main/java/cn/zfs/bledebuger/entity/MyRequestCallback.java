package cn.zfs.bledebuger.entity;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.support.annotation.NonNull;

import cn.zfs.blelib.callback.RequestCallback;
import cn.zfs.blelib.core.Ble;
import cn.zfs.blelib.core.Request;
import cn.zfs.blelib.data.Device;

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
    public void onRequestFialed(@NonNull String requestId, @NonNull Request.RequestType requestType, int failType, byte[] value) {
        super.onRequestFialed(requestId, requestType, failType, value);
        MyBleObservable observable = (MyBleObservable) Ble.getInstance().getObservable();
        observable.notifyRequestFialed(requestId, requestType, failType);
    }

    @Override
    public void onNotificationRegistered(@NonNull String requestId, BluetoothGatt gatt, BluetoothGattDescriptor descriptor) {
        super.onNotificationRegistered(requestId, gatt, descriptor);
        MyBleObservable observable = (MyBleObservable) Ble.getInstance().getObservable();
        observable.notifyNotificationRegistered(requestId, descriptor);
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        MyBleObservable observable = (MyBleObservable) Ble.getInstance().getObservable();
        observable.notifyCharacteristicChanged(characteristic);
    }

    @Override
    public void onNotificationUnregistered(@NonNull String requestId, BluetoothGatt gatt, BluetoothGattDescriptor descriptor) {
        super.onNotificationUnregistered(requestId, gatt, descriptor);
        MyBleObservable observable = (MyBleObservable) Ble.getInstance().getObservable();
        observable.notifyNotificationUnregistered(requestId, descriptor);
    }

    @Override
    public void onCharacteristicRead(@NonNull String requestId, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicRead(requestId, gatt, characteristic);
        MyBleObservable observable = (MyBleObservable) Ble.getInstance().getObservable();
        observable.notifyCharacteristicRead(requestId, gatt, characteristic);
    }
}
