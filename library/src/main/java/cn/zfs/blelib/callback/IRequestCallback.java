package cn.zfs.blelib.callback;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import cn.zfs.blelib.core.Request;

/**
 * 描述:
 * 时间: 2018/5/17 11:26
 * 作者: zengfansheng
 */
public interface IRequestCallback {
    int NONE = 0;
    int NULL_CHARACTERISTIC = 1;
    int NULL_DESCRIPTOR = 2;
    int NULL_SERVICE = 3;
    int GATT_STATUS_REQUEST_NOT_SUPPORTED = 4;
    int GATT_IS_NULL = 5;
    int API_LEVEL_TOO_LOW = 6;

    @IntDef({NONE, NULL_CHARACTERISTIC, NULL_DESCRIPTOR, NULL_SERVICE, GATT_STATUS_REQUEST_NOT_SUPPORTED, GATT_IS_NULL})
    @Retention(RetentionPolicy.SOURCE)
    @interface FAIL_TYPE {}

    /**
     * 读取characteristic的结果
     * @param requestId 请求ID
     */
    void onCharacteristicRead(@NonNull String requestId, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic);

    /**
     * 收到设备notify值 （设备上报值）
     */
    void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic);

    /**
     * 收到设备信号强度RSSI
     * @param rssi 信号强度
     */
    void onReadRemoteRssi(@NonNull String requestId, BluetoothGatt gatt, int rssi);

    /**
     * 收到设备Mtu改变成功后，新值
     * @param mtu 新的mtu值
     */
    void onMtuChanged(@NonNull String requestId, BluetoothGatt gatt, int mtu);

    /**
     * 请求失败
     * @param requestId 请求ID
     * @param requestType 请求类型
     * @param failType 失败类型
     */
    void onRequestFialed(@NonNull String requestId, @NonNull Request.RequestType requestType, @FAIL_TYPE int failType, byte[] value);

    /**
     * 取descriptor的结果
     * @param requestId 请求ID
     */
    void onDescriptorRead(@NonNull String requestId, BluetoothGatt gatt, BluetoothGattDescriptor descriptor);

    /**
     * Notification打开成功
     * @param requestId 请求ID
     */
    void onNotificationRegistered(@NonNull String requestId, BluetoothGatt gatt, BluetoothGattDescriptor descriptor);

    /**
     * Notification关闭成功
     * @param requestId 请求ID
     */
    void onNotificationUnregistered(@NonNull String requestId, BluetoothGatt gatt, BluetoothGattDescriptor descriptor);

    /**
     * Indication打开成功
     * @param requestId 请求ID
     */
    void onIndicationRegistered(@NonNull String requestId, BluetoothGatt gatt, BluetoothGattDescriptor descriptor);

    /**
     * Indication关闭成功
     * @param requestId 请求ID
     */
    void onIndicationUnregistered(@NonNull String requestId, BluetoothGatt gatt, BluetoothGattDescriptor descriptor);

    /**
     * 写入成功
     * @param requestId 请求ID
     */
    void onCharacteristicWrite(@NonNull String requestId, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic);
}
