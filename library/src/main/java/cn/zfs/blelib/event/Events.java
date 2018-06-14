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

        private BluetoothStateChanged(int state) {
            this.state = state;
        }
    }

    /**
     * onCharacteristicChanged，收到设备notify值 （设备上报值）
     */
    public static class CharacteristicChanged extends DeviceEvent<Device> {
        public BluetoothGattCharacteristic characteristic;

        private CharacteristicChanged(@NonNull Device device, BluetoothGattCharacteristic characteristic) {
            super(device);
            this.characteristic = characteristic;
        }
    }

    /**
     * onCharacteristicRead，读取到特征字的值
     */
    public static class CharacteristicRead extends BothDeviceAndRequestIdEvent<Device> {
        public BluetoothGattCharacteristic characteristic;

        private CharacteristicRead(@NonNull Device device, @NonNull String requestId, BluetoothGattCharacteristic characteristic) {
            super(device, requestId);
            this.characteristic = characteristic;
        }
    }

    /**
     * onCharacteristicWrite，写入成功
     */
    public static class CharacteristicWrite extends BothDeviceAndRequestIdEvent<Device> {
        public byte[] value;

        private CharacteristicWrite(@NonNull Device device, @NonNull String requestId, byte[] value) {
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

        private ConnectionCreateFailed(Device device, String error) {
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

        private ConnectionStateChanged(@NonNull Device device, int state) {
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

        private ConnectTimeout(@NonNull Device device, int type) {
            super(device);
            this.type = type;
        }
    }

    public static class DescriptorRead extends BothDeviceAndRequestIdEvent<Device> {
        public BluetoothGattDescriptor descriptor;

        private DescriptorRead(@NonNull Device device, @NonNull String requestId, BluetoothGattDescriptor descriptor) {
            super(device, requestId);
            this.descriptor = descriptor;
        }
    }

    /**
     * indication开关状态变化
     */
    public static class IndicationChanged extends BothDeviceAndRequestIdEvent<Device> {
        public BluetoothGattDescriptor descriptor;
        public boolean isEnabled;

        private IndicationChanged(@NonNull Device device, @NonNull String requestId, BluetoothGattDescriptor descriptor, boolean isEnabled) {
            super(device, requestId);
            this.descriptor = descriptor;
            this.isEnabled = isEnabled;
        }
    }

    /**
     * onMtuChanged，MTU修改成功
     */
    public static class MtuChanged extends BothDeviceAndRequestIdEvent<Device> {
        /** 新的MTU值 */
        public int mtu;

        private MtuChanged(@NonNull Device device, @NonNull String requestId, int mtu) {
            super(device, requestId);
            this.mtu = mtu;
        }
    }

    /**
     * notification开关状态变化
     */
    public static class NotificationChanged extends BothDeviceAndRequestIdEvent<Device> {
        public BluetoothGattDescriptor descriptor;
        public boolean isEnabled;

        private NotificationChanged(@NonNull Device device, @NonNull String requestId, BluetoothGattDescriptor descriptor, boolean isEnabled) {
            super(device, requestId);
            this.descriptor = descriptor;
            this.isEnabled = isEnabled;
        }
    }

    /**
     * onReadRemoteRssi，读取到信息强度
     */
    public static class RemoteRssiRead extends BothDeviceAndRequestIdEvent<Device> {
        public int rssi;

        private RemoteRssiRead(@NonNull Device device, @NonNull String requestId, int rssi) {
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

        private RequestFailed(@NonNull String requestId, @NonNull Request.RequestType requestType, int failType, byte[] src) {
            super(requestId);
            this.requestId = requestId;
            this.requestType = requestType;
            this.failType = failType;
            this.src = src;
        }
    }
    
    public static BluetoothStateChanged newBluetoothStateChanged(int state) {
        return new BluetoothStateChanged(state);
    }

    public static CharacteristicChanged newCharacteristicChanged(@NonNull Device device, BluetoothGattCharacteristic characteristic) {
        return new CharacteristicChanged(device, characteristic);
    }

    public static CharacteristicRead newCharacteristicRead(@NonNull Device device, @NonNull String requestId, BluetoothGattCharacteristic characteristic) {
        return new CharacteristicRead(device, requestId, characteristic);
    }

    public static CharacteristicWrite newCharacteristicWrite(@NonNull Device device, @NonNull String requestId, byte[] value) {
        return new CharacteristicWrite(device, requestId, value);
    }

    public static ConnectionCreateFailed newConnectionCreateFailed(Device device, String error) {
        return new ConnectionCreateFailed(device, error);
    }

    public static ConnectionStateChanged newConnectionStateChanged(@NonNull Device device, int state) {
        return new ConnectionStateChanged(device, state);
    }

    public static ConnectTimeout newConnectTimeout(@NonNull Device device, int type) {
        return new ConnectTimeout(device, type);
    }

    public static DescriptorRead newDescriptorRead(@NonNull Device device, @NonNull String requestId, BluetoothGattDescriptor descriptor) {
        return new DescriptorRead(device, requestId, descriptor);
    }

    public static IndicationChanged newIndicationChanged(@NonNull Device device, @NonNull String requestId, BluetoothGattDescriptor descriptor, boolean isEnabled) {
        return new IndicationChanged(device, requestId, descriptor, isEnabled);
    }

    public static MtuChanged newMtuChanged(@NonNull Device device, @NonNull String requestId, int mtu) {
        return new MtuChanged(device, requestId, mtu);
    }

    public static NotificationChanged newNotificationChanged(@NonNull Device device, @NonNull String requestId, BluetoothGattDescriptor descriptor, boolean isEnabled) {
        return new NotificationChanged(device, requestId, descriptor, isEnabled);
    }

    public static RemoteRssiRead newRemoteRssiRead(@NonNull Device device, @NonNull String requestId, int rssi) {
        return new RemoteRssiRead(device, requestId, rssi);
    }

    public static RequestFailed newRequestFailed(@NonNull String requestId, @NonNull Request.RequestType requestType, int failType, byte[] src) {
        return new RequestFailed(requestId, requestType, failType, src);
    }
}
