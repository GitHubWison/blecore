package cn.zfs.blelib.callback;

import cn.zfs.blelib.event.BluetoothStateChangedEvent;
import cn.zfs.blelib.event.CharacteristicChangedEvent;
import cn.zfs.blelib.event.CharacteristicReadEvent;
import cn.zfs.blelib.event.CharacteristicWriteEvent;
import cn.zfs.blelib.event.ConnectTimeoutEvent;
import cn.zfs.blelib.event.ConnectionCreateFailedEvent;
import cn.zfs.blelib.event.ConnectionStateChangedEvent;
import cn.zfs.blelib.event.DescriptorReadEvent;
import cn.zfs.blelib.event.IndicationRegisteredEvent;
import cn.zfs.blelib.event.IndicationUnregisteredEvent;
import cn.zfs.blelib.event.MtuChangedEvent;
import cn.zfs.blelib.event.NotificationRegisteredEvent;
import cn.zfs.blelib.event.NotificationUnregisteredEvent;
import cn.zfs.blelib.event.ReadRemoteRssiEvent;
import cn.zfs.blelib.event.RequestFailedEvent;

/**
 * 描述: 事件监听器
 * 时间: 2018/5/23 14:09
 * 作者: zengfansheng
 */
public class SimpleEventListener {

    /**
     * 蓝牙状态变化
     */
    public void onBluetoothStateChanged(BluetoothStateChangedEvent event) {}

    /**
     * 收到设备notify值 （设备上报值）
     */
    public void onCharacteristicChanged(CharacteristicChangedEvent event) {}

    /**
     * 读取到特征字的值
     */
    public void onCharacteristicRead(CharacteristicReadEvent event) {}

    /**
     * 写入成功
     */
    public void onCharacteristicWrite(CharacteristicWriteEvent event) {}

    /**
     * 连接创建失败
     */
    public void onConnectionCreateFailed(ConnectionCreateFailedEvent event) {}

    /**
     * 连接状态变化
     */
    public void onConnectionStateChanged(ConnectionStateChangedEvent event) {}

    /**
     * 连接超时
     */
    public void onConnectTimeout(ConnectTimeoutEvent event) {}

    public void onDescriptorRead(DescriptorReadEvent event) {}

    /**
     * indication注册成功
     */
    public void onIndicationRegistered(IndicationRegisteredEvent event) {}

    /**
     * indication取消注册成功
     */
    public void onIndicationUnregistered(IndicationUnregisteredEvent event) {}

    /**
     * MTU修改成功
     */
    public void onMtuChanged(MtuChangedEvent event) {}

    /**
     * notification注册成功
     */
    public void onNotificationRegistered(NotificationRegisteredEvent event) {}

    /**
     * notification取消注册成功
     */
    public void onNotificationUnregistered(NotificationUnregisteredEvent event) {}

    /**
     * 读取到信息强度
     */
    public void onReadRemoteRssi(ReadRemoteRssiEvent event) {}

    /**
     * 请求失败事件，如读特征值、写特征值、开启notification等等
     */
    public void onRequestFailed(RequestFailedEvent event) {}
}
