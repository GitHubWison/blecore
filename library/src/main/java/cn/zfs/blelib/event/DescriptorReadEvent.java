package cn.zfs.blelib.event;

import android.bluetooth.BluetoothGattDescriptor;
import android.support.annotation.NonNull;

import cn.zfs.blelib.core.Device;

/**
 * 描述: onDescriptorRead
 * 时间: 2018/5/19 20:19
 * 作者: zengfansheng
 */
public class DescriptorReadEvent extends BothDeviceAndRequestIdEvent<Device> {
    public BluetoothGattDescriptor descriptor;

    public DescriptorReadEvent(@NonNull Device device, @NonNull String requestId, BluetoothGattDescriptor descriptor) {
        super(device, requestId);
        this.descriptor = descriptor;
    }
}
