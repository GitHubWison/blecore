package cn.zfs.blelib.event;

import android.bluetooth.BluetoothGattCharacteristic;
import android.support.annotation.NonNull;

import cn.zfs.blelib.core.Device;

/**
 * 描述: onCharacteristicChanged，收到设备notify值 （设备上报值）
 * 时间: 2018/5/19 20:05
 * 作者: zengfansheng
 */
public class CharacteristicChangedEvent<D extends Device> extends DeviceEvent<D> {
    public BluetoothGattCharacteristic characteristic;

    public CharacteristicChangedEvent(@NonNull D device, BluetoothGattCharacteristic characteristic) {
        super(device);
        this.characteristic = characteristic;
    }
}
