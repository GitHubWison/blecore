package cn.zfs.blelib.core;

import android.bluetooth.BluetoothGattDescriptor;
import android.support.annotation.NonNull;

import java.util.Queue;
import java.util.UUID;

import cn.zfs.blelib.util.BleUtils;

/**
 * 描述: 用作请求队列
 * 时间: 2018/4/11 15:15
 * 作者: zengfansheng
 */
public class Request {
    
    public enum RequestType {
        TOGGLE_NOTIFICATION, TOGGLE_INDICATION, READ_CHARACTERISTIC, READ_DESCRIPTOR, READ_RSSI, WRITE_CHARACTERISTIC, WRITE_DESCRIPTOR, CHANGE_MTU
    }

    public RequestType type;
    public UUID service;
    public UUID characteristic;
    public UUID descriptor;
    public String requestId;
    public byte[] value;
    boolean waitWriteResult;
    int writeDelay;
    long startTime;//用来记超时，避免卡住队列
    //-----分包发送时用到-----
    Queue<byte[]> remainQueue;
    //----------------------

    private Request(@NonNull RequestType type, @NonNull String requestId, UUID service, UUID characteristic, UUID descriptor, byte[] value) {
        this.type = type;
        this.requestId = requestId;
        this.service = service;
        this.characteristic = characteristic;
        this.descriptor = descriptor;
        this.value = value;
    }
    
    static Request newChangeMtuRequest(@NonNull String requestId, int mtu) {
        if (mtu < 23) {
            mtu = 23;
        } else if (mtu > 517) {
            mtu = 517;
        }
        return new Request(RequestType.CHANGE_MTU, requestId, null, null, null, BleUtils.numberToBytes(false, mtu, 4));
    }
    
    static Request newReadCharacteristicRequest(@NonNull String requestId, UUID service, UUID characteristic) {
        return new Request(RequestType.READ_CHARACTERISTIC, requestId, service, characteristic, null, null);
    }
    
    static Request newToggleNotificationRequest(@NonNull String requestId, UUID service, UUID characteristic, boolean enable) {
        return new Request(RequestType.TOGGLE_NOTIFICATION, requestId, service, characteristic, null, 
                enable ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
    }

    static Request newToggleIndicationRequest(@NonNull String requestId, UUID service, UUID characteristic, boolean enable) {
        return new Request(RequestType.TOGGLE_INDICATION, requestId, service, characteristic, null,
                enable ? BluetoothGattDescriptor.ENABLE_INDICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
    }
    
    static Request newReadDescriptorRequest(@NonNull String requestId, UUID service, UUID characteristic, UUID descriptor) {
        return new Request(RequestType.READ_DESCRIPTOR, requestId, service, characteristic, descriptor, null);
    }
    
    static Request newWriteCharacteristicRequest(@NonNull String requestId, UUID service, UUID characteristic, byte[] value) {
        return new Request(RequestType.WRITE_CHARACTERISTIC, requestId, service, characteristic, null, value);
    }
    
    static Request newReadRssiRequest(@NonNull String requestId) {
        return new Request(RequestType.READ_RSSI, requestId, null, null, null, null);
    }
}
