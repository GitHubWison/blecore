package cn.zfs.blelib.core;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.util.Log;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 描述: 接收蓝牙数据消息
 * 时间: 2018/4/23 11:17
 * 作者: zengfansheng
 */
public class RequestCallback {
    public static final int NONE = 0;
    public static final int NULL_CHARACTERISTIC = 1;
    public static final int NULL_DESCRIPTOR = 2;
    public static final int NULL_SERVICE = 3;
    public static final int GATT_STATUS_REQUEST_NOT_SUPPORTED = 4;
    public static final int GATT_IS_NULL = 5;

    @IntDef({NONE, NULL_CHARACTERISTIC, NULL_DESCRIPTOR, NULL_SERVICE, GATT_STATUS_REQUEST_NOT_SUPPORTED, GATT_IS_NULL})
    @Retention(RetentionPolicy.SOURCE)
    public @interface FAIL_TYPE {}
    
    protected Device device;

    public RequestCallback(@NonNull Device device) {
        this.device = device;
    }

    public Device getDevice() {
        return device;
    }

    /**
     * 连接销毁回调
     */
    public void onDestroy() {}
    
    /**
     * 读取characteristic的结果
     * @param requestId 请求ID
     */
    public void onCharacteristicRead(String requestId, BluetoothGattCharacteristic characteristic) {
        Ble.println(RequestCallback.class, Log.DEBUG, "onCharacteristicRead！请求ID：" + requestId +
                "value: " + BleUtils.bytesToHexString(characteristic.getValue()) + ", mac: " + device.addr);
    }

    /**
     * 收到设备notify值 （设备上报值）
     */
    public void onCharacteristicChanged(BluetoothGattCharacteristic characteristic) {}

    /**
     * 收到设备信号强度RSSI
     * @param rssi 信号强度
     */
    public void onRssiRead(int rssi) {
        Ble.println(RequestCallback.class, Log.DEBUG, "读到信号强度！rssi: "+ rssi + ", mac: " + device.addr);
    }

    /**
     * 请求失败
     * @param requestId 请求ID
     * @param requestType 请求类型
     * @param failType 失败类型
     */
    public void onRequestFialed(String requestId, Request.RequestType requestType, @FAIL_TYPE int failType, byte[] value) {
        if (requestType == Request.RequestType.WRITE_CHARACTERISTIC) {
            Ble.getInstance().getObservable().notifyWriteCharacteristicResult(device, requestId, false, value);
        }
        Ble.println(RequestCallback.class, Log.ERROR, "请求失败！请求ID：" + requestId + 
                ", failType: " + failType + ", mac: " + device.addr);
    }

    /**
     * 取descriptor的结果
     * @param requestId 请求ID
     */
    public void onDescriptorRead(String requestId, BluetoothGattDescriptor descriptor) {
        Ble.println(RequestCallback.class, Log.DEBUG, "onDescriptorRead！请求ID：" + requestId + 
                "value: " + BleUtils.bytesToHexString(descriptor.getValue()) + ", mac: " + device.addr);
    }

    /**
     * Notification打开成功
     * @param requestId 请求ID
     */
    public void onNotificationRegistered(String requestId, BluetoothGattDescriptor descriptor) {
        Ble.println(RequestCallback.class, Log.DEBUG, "NOTIFICATION_REGISTERED！请求ID：" + requestId + ", mac: " + device.addr);
    }

    /**
     * Notification关闭成功
     * @param requestId 请求ID
     */
    public void onNotificationUnregistered(String requestId, BluetoothGattDescriptor descriptor) {
        Ble.println(RequestCallback.class, Log.DEBUG, "NOTIFICATION_UNREGISTERED！请求ID：" + requestId + ", mac: " + device.addr);
    }

    /**
     * Indication打开成功
     * @param requestId 请求ID
     */
    public void onIndicationRegistered(String requestId, BluetoothGattDescriptor descriptor) {
        Ble.println(RequestCallback.class, Log.DEBUG, "INDICATION_REGISTERED！请求ID：" + requestId + ", mac: " + device.addr);
    }

    /**
     * Indication关闭成功
     * @param requestId 请求ID
     */
    public void onIndicationUnregistered(String requestId, BluetoothGattDescriptor descriptor) {
        Ble.println(RequestCallback.class, Log.DEBUG, "INDICATION_UNREGISTERED！请求ID：" + requestId + ", mac: " + device.addr);
    }

    /**
     * 写入成功
     * @param requestId 请求ID
     */
    public void onCharacteristicWrite(String requestId, BluetoothGattCharacteristic characteristic) {
        Ble.println(RequestCallback.class, Log.DEBUG, "写入成功！value: "+ BleUtils.bytesToHexString(characteristic.getValue()) +
                "请求ID：" + requestId + ", mac: " + device.addr);
        Ble.getInstance().getObservable().notifyWriteCharacteristicResult(device, requestId, true, characteristic.getValue());
    }
}
