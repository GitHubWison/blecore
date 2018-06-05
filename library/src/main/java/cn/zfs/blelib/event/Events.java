package cn.zfs.blelib.event;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.support.annotation.NonNull;

import cn.zfs.blelib.core.BaseConnection;
import cn.zfs.blelib.core.Connection;
import cn.zfs.blelib.core.Device;
import cn.zfs.blelib.core.Request;

/**
 * 描述: 事件统一管理
 * 时间: 2018/5/29 09:25
 * 作者: zengfansheng
 */
public class Events {
    /**
     * 蓝牙状态变化
     */
    public static class BluetoothStateChanged {
        /**
         * 当前状态。可能的值：
         * <br>{@link BluetoothAdapter#STATE_OFF}
         * <br>{@link BluetoothAdapter#STATE_TURNING_ON}
         * <br>{@link BluetoothAdapter#STATE_ON}
         * <br>{@link BluetoothAdapter#STATE_TURNING_OFF}
         */
        public int state;

        public BluetoothStateChanged(int state) {
            this.state = state;
        }
    }

    /**
     * onCharacteristicChanged，收到设备notify值 （设备上报值）
     */
    public static class CharacteristicChanged extends DeviceEvent<Device> {
        public BluetoothGattCharacteristic characteristic;

        public CharacteristicChanged(@NonNull Device device, BluetoothGattCharacteristic characteristic) {
            super(device);
            this.characteristic = characteristic;
        }
    }

    /**
     * onCharacteristicRead，读取到特征字的值
     */
    public static class CharacteristicRead extends BothDeviceAndRequestIdEvent<Device> {
        public BluetoothGattCharacteristic characteristic;

        public CharacteristicRead(@NonNull Device device, @NonNull String requestId, BluetoothGattCharacteristic characteristic) {
            super(device, requestId);
            this.characteristic = characteristic;
        }
    }

    /**
     * onCharacteristicWrite，写入成功
     */
    public static class CharacteristicWrite extends BothDeviceAndRequestIdEvent<Device> {
        public byte[] value;

        public CharacteristicWrite(@NonNull Device device, @NonNull String requestId, byte[] value) {
            super(device, requestId);
            this.value = value;
        }
    }

    /**
     * 连接创建失败
     */
    public static class ConnectionCreateFailed {
        /** 设备 */
        public Device device;
        /** 失败详情 */
        public String error;

        public ConnectionCreateFailed(Device device, String error) {
            this.device = device;
            this.error = error;
        }
    }

    /**
     * 连接状态变化
     */
    public static class ConnectionStateChanged extends DeviceEvent<Device> {
        /**
         * 当前连接状态。可能的值：
         * <br>{@link Connection#STATE_DISCONNECTED}
         * <br>{@link Connection#STATE_CONNECTING}
         * <br>{@link Connection#STATE_RECONNECTING}
         * <br>{@link Connection#STATE_CONNECTED}
         * <br>{@link Connection#STATE_SERVICE_DISCORVERING}
         * <br>{@link Connection#STATE_SERVICE_DISCORVERED}
         * <br>{@link Connection#STATE_RELEASED}
         */
        public int state;

        public ConnectionStateChanged(@NonNull Device device, int state) {
            super(device);
            this.state = state;
        }
    }

    /**
     * 连接超时
     */
    public static class ConnectTimeout extends DeviceEvent<Device> {
        /**
         * 设备连接超时。可能的值：
         * <br>{@link Connection#TIMEOUT_TYPE_CANNOT_DISCOVER_DEVICE}
         * <br>{@link Connection#TIMEOUT_TYPE_CANNOT_CONNECT}
         * <br>{@link Connection#TIMEOUT_TYPE_CANNOT_DISCOVER_SERVICES}
         */
        public int type;

        public ConnectTimeout(@NonNull Device device, int type) {
            super(device);
            this.type = type;
        }
    }

    public static class DescriptorRead extends BothDeviceAndRequestIdEvent<Device> {
        public BluetoothGattDescriptor descriptor;

        public DescriptorRead(@NonNull Device device, @NonNull String requestId, BluetoothGattDescriptor descriptor) {
            super(device, requestId);
            this.descriptor = descriptor;
        }
    }

    /**
     * indication注册成功
     */
    public static class IndicationRegistered extends BothDeviceAndRequestIdEvent<Device> {
        public BluetoothGattDescriptor descriptor;

        public IndicationRegistered(@NonNull Device device, @NonNull String requestId, BluetoothGattDescriptor descriptor) {
            super(device, requestId);
            this.descriptor = descriptor;
        }
    }

    /**
     * indication取消注册成功
     */
    public static class IndicationUnregistered extends BothDeviceAndRequestIdEvent<Device> {
        public BluetoothGattDescriptor descriptor;

        public IndicationUnregistered(@NonNull Device device, @NonNull String requestId, BluetoothGattDescriptor descriptor) {
            super(device, requestId);
            this.descriptor = descriptor;
        }
    }

    /**
     * onMtuChanged，MTU修改成功
     */
    public static class MtuChanged extends BothDeviceAndRequestIdEvent<Device> {
        /** 新的MTU值 */
        public int mtu;

        public MtuChanged(@NonNull Device device, @NonNull String requestId, int mtu) {
            super(device, requestId);
            this.mtu = mtu;
        }
    }

    /**
     * notification注册成功
     */
    public static class NotificationRegistered extends BothDeviceAndRequestIdEvent<Device> {
        public BluetoothGattDescriptor descriptor;

        public NotificationRegistered(@NonNull Device device, @NonNull String requestId, BluetoothGattDescriptor descriptor) {
            super(device, requestId);
            this.descriptor = descriptor;
        }
    }

    /**
     * notification取消注册成功
     */
    public static class NotificationUnregistered extends BothDeviceAndRequestIdEvent<Device> {
        public BluetoothGattDescriptor descriptor;

        public NotificationUnregistered(@NonNull Device device, @NonNull String requestId, BluetoothGattDescriptor descriptor) {
            super(device, requestId);
            this.descriptor = descriptor;
        }
    }

    /**
     * onReadRemoteRssi，读取到信息强度
     */
    public static class ReadRemoteRssi extends BothDeviceAndRequestIdEvent<Device> {
        public int rssi;

        public ReadRemoteRssi(@NonNull Device device, @NonNull String requestId, int rssi) {
            super(device, requestId);
            this.rssi = rssi;
        }
    }

    /**
     * 请求失败事件，如读特征值、写特征值、开启notification等等
     */
    public static class RequestFailed extends RequestIdEvent {

        @NonNull
        public Request.RequestType requestType;
        /** 请求时带的数据 */
        public byte[] src;

        /**
         * {@link BaseConnection#FAIL_TYPE_REQUEST_FAILED}<br>{@link BaseConnection#FAIL_TYPE_NULL_CHARACTERISTIC}<br>{@link BaseConnection#FAIL_TYPE_NULL_DESCRIPTOR},
         * <br>{@link BaseConnection#FAIL_TYPE_NULL_SERVICE}<br>{@link BaseConnection#FAIL_TYPE_GATT_STATUS_FAILED}<br>{@link BaseConnection#FAIL_TYPE_GATT_IS_NULL}
         * <br>{@link BaseConnection#FAIL_TYPE_API_LEVEL_TOO_LOW}<br>{@link BaseConnection#FAIL_TYPE_BLUETOOTH_ADAPTER_DISABLED}
         */
        public int failType;

        public RequestFailed(@NonNull String requestId, @NonNull Request.RequestType requestType, int failType, byte[] src) {
            super(requestId);
            this.requestId = requestId;
            this.requestType = requestType;
            this.failType = failType;
            this.src = src;
        }
    }
}
