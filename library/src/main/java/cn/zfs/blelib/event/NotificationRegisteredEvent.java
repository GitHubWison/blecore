package cn.zfs.blelib.event;

import android.bluetooth.BluetoothGattDescriptor;
import android.support.annotation.NonNull;

import cn.zfs.blelib.core.Device;

/**
 * 描述: notification注册成功
 * 时间: 2018/5/19 20:21
 * 作者: zengfansheng
 */
public class NotificationRegisteredEvent extends BothDeviceAndRequestIdEvent<Device> {
    public BluetoothGattDescriptor descriptor;

    public NotificationRegisteredEvent(@NonNull Device device, @NonNull String requestId, BluetoothGattDescriptor descriptor) {
        super(device, requestId);
        this.descriptor = descriptor;
    }
}
