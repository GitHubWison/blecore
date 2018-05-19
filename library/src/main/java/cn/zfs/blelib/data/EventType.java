package cn.zfs.blelib.data;

/**
 * 描述: 事件类型
 * 时间: 2018/5/18 09:43
 * 作者: zengfansheng
 */
public interface EventType {
    /** 蓝牙状态变化 */
    int ON_BLUETOOTH_STATE_CHANGED = 0;
    /** 连接状态变化 */
    int ON_CONNECTION_STATE_CHANGED = 1;
    /** 连接建立失败 */
    int ON_CONNECTION_CREATE_FAILED = 2;
    /** 连接建立失败 */
    int ON_CONNECT_TIMEOUT = 3;
    /** 数据发送成功 */
    int ON_WRITE_CHARACTERISTIC = 4;
    /** 收到notify数据 */
    int ON_CHARACTERISTIC_CHANGED = 5;
    /** 读取到characteristic值 */
    int ON_CHARACTERISTIC_READ = 6;
    /** 读取到到信号强度 */
    int ON_READ_REMOTE_RSSI = 7;
    /** 读取Descriptor值 */
    int ON_DESCRIPTOR_READ = 8;
    /** mtu修改结果 */
    int ON_MTU_CHANGED = 9;    
    /** 蓝牙请求失败，writeCharacteristic，readCharacteristic等 */
    int ON_BLE_REQUEST_FIALED = 10;    
    /** notification注册成功 */
    int ON_NOTIFICATION_REGISTERED = 11;
    /** notification取消注册成功 */
    int ON_NOTIFICATION_UNREGISTERED = 12;
    /** indication注册成功 */
    int ON_INDICATION_REGISTERED = 13;
    /** indication取消注册成功 */
    int ON_INDICATION_UNREGISTERED = 14;
    /** 蓝牙连接释放了 */
    int ON_CONNECTION_RELEASED = 15;
}
