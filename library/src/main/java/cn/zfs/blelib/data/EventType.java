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
    /** 读到设备信号强度RSSI */
    int ON_RSSI_READ = 4;
    /** 数据发送结果 */
    int ON_WRITE_CHARACTERISTIC = 5;
    /** 收到notify数据 */
    int ON_CHARACTERISTIC_CHANGED = 6;
    /** 读取到characteristic值 */
    int ON_CHARACTERISTIC_READ = 7;
    /** 读取到到信号强度 */
    int ON_READ_REMOTE_RSSI = 8;
    /** 读取Descriptor值 */
    int ON_Descriptor_Read = 9;
    /** mtu修改结果 */
    int ON_MTU_CHANGED = 10;    
    /** 蓝牙请求失败，writeCharacteristic，readCharacteristic等 */
    int ON_BLE_REQUEST_FIALED = 11;    
    /** notification注册状态变化 */
    int ON_NOTIFICATION_REGISTER_STATE_CHANGED = 12;    
    /** indication注册状态变化 */
    int ON_INDICATION_REGISTER_STATE_CHANGED = 13;
}
