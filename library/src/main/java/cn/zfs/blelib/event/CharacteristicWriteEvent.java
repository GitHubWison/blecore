package cn.zfs.blelib.event;

import android.bluetooth.BluetoothGattCharacteristic;
import android.support.annotation.NonNull;

import cn.zfs.blelib.core.Device;

/**
 * 描述: onCharacteristicWrite，写入成功
 * 时间: 2018/5/19 20:25
 * 作者: zengfansheng
 */
public class CharacteristicWriteEvent<D extends Device> extends BothDeviceAndRequestIdEvent<D> {
    public BluetoothGattCharacteristic characteristic;

    public CharacteristicWriteEvent(@NonNull D device, @NonNull String requestId, BluetoothGattCharacteristic characteristic) {
        super(device, requestId);
        this.characteristic = characteristic;
    }
}
