package cn.zfs.blelib.core;

import android.support.annotation.NonNull;

/**
 * 描述: 蓝牙设备状态、数据观察者，观察设备状态、数据变化
 * 时间: 2018/4/17 17:02
 * 作者: zengfansheng
 */
public class BleObserver {

	/**
	 * 设备连接状态变化
	 * @param device 蓝牙设备
	 * @param state 连接状态<br> {@link Connection#STATE_DISCONNECTED}<br> {@link Connection#STATE_CONNECTING}<br>
     *              {@link Connection#STATE_RECONNECTING}<br> {@link Connection#STATE_CONNECTED}<br>
     *              {@link Connection#STATE_SERVICE_DISCORVERING}<br> {@link Connection#STATE_SERVICE_DISCORVERED}
	 */
	public void onConnectionStateChange(@NonNull Device device, @Connection.STATE int state) {}

    /**
     * 无法建立连接
     * @param device 蓝牙设备，有可能是null
     * @param error 错误信息
     */
	public void onUnableConnect(Device device, String error) {}
	
	/**
	 * 设备连接超时
	 * @param device 蓝牙设备
     * @param type 超时类型<br> {@link Connection#TIMEOUT_TYPE_CANNOT_DISCOVER_DEVICE}<br>
     *     {@link Connection#TIMEOUT_TYPE_CANNOT_CONNECT}<br> {@link Connection#TIMEOUT_TYPE_CANNOT_DISCOVER_SERVICES}  
	 */
	public void onConnectTimeout(@NonNull Device device, int type) {}
	    
    /**
     * 收到设备信号强度RSSI
     * @param device 设备
     * @param rssi 信号强度
     */
    public void onRssiRead(@NonNull Device device, int rssi) {}

    /**
     * 数据发送结果
     * @param device 设备
     * @param requestId 请求ID
     * @param result 发送结果
     * @param value 写入的数据
     */
    public void onWriteCharacteristicResult(@NonNull Device device, String requestId, boolean result, byte[] value) {}
}
