package cn.zfs.blelib.event;

import android.bluetooth.BluetoothGattCharacteristic;
import android.support.annotation.NonNull;

import cn.zfs.blelib.core.Device;

/**
 * 描述: onCharacteristicWrite，写入成功
 * 时间: 2018/5/19 20:25
 * 作者: zengfansheng
 */
public class CharacteristicWriteEvent extends BothDeviceAndRequestIdEvent<Device> {
    public BluetoothGattCharacteristic characteristic;

    public CharacteristicWriteEvent(@NonNull Device device, @NonNull String requestId, BluetoothGattCharacteristic characteristic) {
        super(device, requestId);
        this.characteristic = characteristic;
    }
}
