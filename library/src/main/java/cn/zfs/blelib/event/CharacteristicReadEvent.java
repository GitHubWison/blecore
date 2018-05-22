package cn.zfs.blelib.event;

import android.bluetooth.BluetoothGattCharacteristic;
import android.support.annotation.NonNull;

import cn.zfs.blelib.core.Device;

/**
 * 描述: onCharacteristicRead，读取到特征字的值
 * 时间: 2018/5/19 20:03
 * 作者: zengfansheng
 */
public class CharacteristicReadEvent extends BothDeviceAndRequestIdEvent<Device> {
    public BluetoothGattCharacteristic characteristic;

    public CharacteristicReadEvent(@NonNull Device device, @NonNull String requestId, BluetoothGattCharacteristic characteristic) {
        super(device, requestId);
        this.characteristic = characteristic;
    }
}
