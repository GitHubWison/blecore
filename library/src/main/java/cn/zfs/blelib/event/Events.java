package cn.zfs.blelib.event;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.support.annotation.NonNull;

import cn.zfs.blelib.callback.IRequestCallback;
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
     * 描述: onCharacteristicRead，读取到特征字的值
     */
    public static class CharacteristicRead extends BothDeviceAndRequestIdEvent<Device> {
        public BluetoothGattCharacteristic characteristic;

        public CharacteristicRead(@NonNull Device device, @NonNull String requestId, BluetoothGattCharacteristic characteristic) {
            super(device, requestId);
            this.characteristic = characteristic;
        }
    }

    /**
     * 描述: onCharacteristicWrite，写入成功
     */
    public static class CharacteristicWrite extends BothDeviceAndRequestIdEvent<Device> {
        public BluetoothGattCharacteristic characteristic;

        public CharacteristicWrite(@NonNull Device device, @NonNull String requestId, BluetoothGattCharacteristic characteristic) {
            super(device, requestId);
            this.characteristic = characteristic;
        }
    }

    /**
     * 描述: 连接创建失败
     * 时间: 2018/5/19 19:35
     * 作者: zengfansheng
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
     * 描述: 连接状态变化
     * 时间: 2018/5/19 19:42
     * 作者: zengfansheng
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
     * 描述: 连接超时
     * 时间: 2018/5/19 19:51
     * 作者: zengfansheng
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

    /**
     * 描述: onDescriptorRead
     * 时间: 2018/5/19 20:19
     * 作者: zengfansheng
     */
    public static class DescriptorRead extends BothDeviceAndRequestIdEvent<Device> {
        public BluetoothGattDescriptor descriptor;

        public DescriptorRead(@NonNull Device device, @NonNull String requestId, BluetoothGattDescriptor descriptor) {
            super(device, requestId);
            this.descriptor = descriptor;
        }
    }

    /**
     * 描述: indication注册成功
     * 时间: 2018/5/19 20:21
     * 作者: zengfansheng
     */
    public static class IndicationRegistered extends BothDeviceAndRequestIdEvent<Device> {
        public BluetoothGattDescriptor descriptor;

        public IndicationRegistered(@NonNull Device device, @NonNull String requestId, BluetoothGattDescriptor descriptor) {
            super(device, requestId);
            this.descriptor = descriptor;
        }
    }

    /**
     * 描述: indication取消注册成功
     * 时间: 2018/5/19 20:21
     * 作者: zengfansheng
     */
    public static class IndicationUnregistered extends BothDeviceAndRequestIdEvent<Device> {
        public BluetoothGattDescriptor descriptor;

        public IndicationUnregistered(@NonNull Device device, @NonNull String requestId, BluetoothGattDescriptor descriptor) {
            super(device, requestId);
            this.descriptor = descriptor;
        }
    }

    /**
     * 描述: onMtuChanged，MTU修改成功
     * 时间: 2018/5/19 20:10
     * 作者: zengfansheng
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
     * 描述: notification注册成功
     * 时间: 2018/5/19 20:21
     * 作者: zengfansheng
     */
    public static class NotificationRegistered extends BothDeviceAndRequestIdEvent<Device> {
        public BluetoothGattDescriptor descriptor;

        public NotificationRegistered(@NonNull Device device, @NonNull String requestId, BluetoothGattDescriptor descriptor) {
            super(device, requestId);
            this.descriptor = descriptor;
        }
    }

    /**
     * 描述: notification取消注册成功
     * 时间: 2018/5/19 20:21
     * 作者: zengfansheng
     */
    public static class NotificationUnregistered extends BothDeviceAndRequestIdEvent<Device> {
        public BluetoothGattDescriptor descriptor;

        public NotificationUnregistered(@NonNull Device device, @NonNull String requestId, BluetoothGattDescriptor descriptor) {
            super(device, requestId);
            this.descriptor = descriptor;
        }
    }

    /**
     * 描述: onReadRemoteRssi，读取到信息强度
     * 时间: 2018/5/19 20:08
     * 作者: zengfansheng
     */
    public static class ReadRemoteRssi extends BothDeviceAndRequestIdEvent<Device> {
        public int rssi;

        public ReadRemoteRssi(@NonNull Device device, @NonNull String requestId, int rssi) {
            super(device, requestId);
            this.rssi = rssi;
        }
    }

    /**
     * 描述: 请求失败事件，如读特征值、写特征值、开启notification等等
     */
    public static class RequestFailed extends RequestIdEvent {

        @NonNull
        public Request.RequestType requestType;
        /** 请求时带的数据 */
        public byte[] src;

        /**
         * {@link IRequestCallback#NONE}<br>{@link IRequestCallback#NULL_CHARACTERISTIC}<br>{@link IRequestCallback#NULL_DESCRIPTOR},
         * <br>{@link IRequestCallback#NULL_SERVICE}<br>{@link IRequestCallback#GATT_STATUS_REQUEST_NOT_SUPPORTED}
         * <br>{@link IRequestCallback#GATT_IS_NULL}<br>{@link IRequestCallback#API_LEVEL_TOO_LOW}
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
