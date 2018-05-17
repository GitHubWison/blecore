package cn.zfs.bledebuger.entity;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;

import cn.zfs.blelib.core.Request;
import cn.zfs.blelib.data.BleObserver;

/**
 * 描述:
 * 时间: 2018/4/27 14:04
 * 作者: zengfansheng
 */
public class MyBleObserver extends BleObserver {
    public void onRequestFialed(String requestId, Request.RequestType requestType, int failType) {}

    public void onNotificationRegistered(String requestId, BluetoothGattDescriptor descriptor) {}

    /**
     * 收到设备notify值 （设备上报值）
     */
    public void onCharacteristicChanged(BluetoothGattCharacteristic characteristic) {}

    /**
     * Notification关闭成功
     * @param requestId 请求ID
     */
    public void onNotificationUnregistered(String requestId, BluetoothGattDescriptor descriptor) {}

    /**
     * 读取到值
     */
    public void onCharacteristicRead(String requestId, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {}
}
