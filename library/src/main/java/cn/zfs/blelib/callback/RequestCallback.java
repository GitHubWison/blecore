package cn.zfs.blelib.callback;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.support.annotation.NonNull;
import android.util.Log;

import cn.zfs.blelib.core.Ble;
import cn.zfs.blelib.core.Request;
import cn.zfs.blelib.data.Device;
import cn.zfs.blelib.util.BleUtils;

/**
 * 描述: 接收蓝牙数据消息
 * 时间: 2018/4/23 11:17
 * 作者: zengfansheng
 */
public class RequestCallback implements IRequestCallback {    
    protected Device device;

    public RequestCallback(@NonNull Device device) {
        this.device = device;
    }

    public Device getDevice() {
        return device;
    }

    @Override
    public void onDestroy() {
        
    }

    @Override
    public void onCharacteristicRead(@NonNull String requestId, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        Ble.println(RequestCallback.class, Log.DEBUG, "onCharacteristicRead！请求ID：" + requestId +
                ", value: " + BleUtils.bytesToHexString(characteristic.getValue()) + ", mac: " + device.addr);
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

    }

    @Override
    public void onRssiRead(@NonNull String requestId, BluetoothGatt gatt, int rssi) {
        Ble.println(RequestCallback.class, Log.DEBUG, "读到信号强度！rssi: "+ rssi + ", mac: " + device.addr);
    }

    @Override
    public void onMtuChanged(@NonNull String requestId, BluetoothGatt gatt, int mtu) {
        Ble.println(RequestCallback.class, Log.DEBUG, "Mtu修改成功！mtu: "+ mtu + ", mac: " + device.addr);
    }

    @Override
    public void onRequestFialed(@NonNull String requestId, @NonNull Request.RequestType requestType, int failType, byte[] value) {
        if (requestType == Request.RequestType.WRITE_CHARACTERISTIC) {
            Ble.getInstance().getObservable().notifyWriteCharacteristicResult(device, requestId, false, value);
        }
        Ble.println(RequestCallback.class, Log.ERROR, "请求失败！请求ID：" + requestId +
                ", failType: " + failType + ", mac: " + device.addr);
    }

    @Override
    public void onDescriptorRead(@NonNull String requestId, BluetoothGatt gatt, BluetoothGattDescriptor descriptor) {
        Ble.println(RequestCallback.class, Log.DEBUG, "onDescriptorRead！请求ID：" + requestId +
                ", value: " + BleUtils.bytesToHexString(descriptor.getValue()) + ", mac: " + device.addr);
    }

    @Override
    public void onNotificationRegistered(@NonNull String requestId, BluetoothGatt gatt, BluetoothGattDescriptor descriptor) {
        Ble.println(RequestCallback.class, Log.DEBUG, "NOTIFICATION_REGISTERED！请求ID：" + requestId + ", mac: " + device.addr);
    }

    @Override
    public void onNotificationUnregistered(@NonNull String requestId, BluetoothGatt gatt, BluetoothGattDescriptor descriptor) {
        Ble.println(RequestCallback.class, Log.DEBUG, "NOTIFICATION_UNREGISTERED！请求ID：" + requestId + ", mac: " + device.addr);
    }

    @Override
    public void onIndicationRegistered(@NonNull String requestId, BluetoothGatt gatt, BluetoothGattDescriptor descriptor) {
        Ble.println(RequestCallback.class, Log.DEBUG, "INDICATION_REGISTERED！请求ID：" + requestId + ", mac: " + device.addr);
    }

    @Override
    public void onIndicationUnregistered(@NonNull String requestId, BluetoothGatt gatt, BluetoothGattDescriptor descriptor) {
        Ble.println(RequestCallback.class, Log.DEBUG, "INDICATION_UNREGISTERED！请求ID：" + requestId + ", mac: " + device.addr);
    }

    @Override
    public void onCharacteristicWrite(@NonNull String requestId, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        Ble.println(RequestCallback.class, Log.DEBUG, "写入成功！value: "+ BleUtils.bytesToHexString(characteristic.getValue()) +
                ", 请求ID：" + requestId + ", mac: " + device.addr);
        Ble.getInstance().getObservable().notifyWriteCharacteristicResult(device, requestId, true, characteristic.getValue());
    }
}
