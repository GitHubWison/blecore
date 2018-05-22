package cn.zfs.blelib.core;

import android.bluetooth.BluetoothGattCharacteristic;

import cn.zfs.blelib.callback.ConnectionCallback;
import cn.zfs.blelib.callback.IRequestCallback;

/**
 * 描述: 蓝牙配置
 * 时间: 2018/4/17 17:02
 * 作者: zengfansheng
 */
public class Configuration {
    private static final int DEFAULT_DISCOVER_SERVICES_DELAY_MILLIS = 500;
    private static final int DEFAULT_CONN_TIMEOUT_MILLIS = 8000;//连接超时时间
    public static final int TRY_RECONNECT_TIMES_INFINITE = -1;//无限重连
    private IScanHandler scanHandler;
    private long discoverServicesDelayMillis = DEFAULT_DISCOVER_SERVICES_DELAY_MILLIS;
    private int connectTimeoutMillis = DEFAULT_CONN_TIMEOUT_MILLIS;
    private Class<? extends IRequestCallback> requestCallbackClass;
    private Class<? extends ConnectionCallback> connectionCallbackClass;
    private Class<? extends Device> deviceClass;
    private int tryReconnectTimes = TRY_RECONNECT_TIMES_INFINITE;
    private int writeDelayMillis;
    private int scanPeriodMillis = 10000;
    private boolean useBluetoothLeScanner = true;
    private int packageSize = 20;//发送数据时的分包大小
    private int writeType;
    private boolean waitWriteResult;

    public Class<? extends Device> getDeviceClass() {
        return deviceClass;
    }

    /**
     * 设置设备字节码，扫描回调返回的Device实例根据此字节码实例化
     */
    public Configuration setDeviceClass(Class<? extends Device> deviceClass) {
        this.deviceClass = deviceClass;
        return this;
    }

    /**
     * 设置扫描过滤器
     *
     * @param handler 扫描结果处理
     */
    public Configuration setScanHandler(IScanHandler handler) {
        scanHandler = handler;
        return this;
    }

    public IScanHandler getScanHandler() {
        return scanHandler;
    }

    /**
     * 连接状态回调
     */
    public Class<? extends ConnectionCallback> getConnectionCallbackClass() {
        return connectionCallbackClass;
    }

    /**
     * 设置连接状态变化回调
     */
    public Configuration setConnectionCallbackClass(Class<? extends ConnectionCallback> connectionCallbackClass) {
        this.connectionCallbackClass = connectionCallbackClass;
        return this;
    }

    public Class<? extends IRequestCallback> getRequestCallbackClass() {
        return requestCallbackClass;
    }

    /**
     * 设置请求回调，数据交互
     */
    public Configuration setRequestCallbackClass(Class<? extends IRequestCallback> requestCallbackClass) {
        this.requestCallbackClass = requestCallbackClass;
        return this;
    }

    /**
     * 连接时是否执行绑定
     */
    public boolean isBondWhenConnect(Device device) {
        return false;
    }

    /**
     * 获取连接超时时间
     */
    public int getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    /**
     * 设置连接超时时间
     */
    public Configuration setConnectTimeoutMillis(int connectTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
        return this;
    }

    /**
     * 设置连接成功后，延时发现服务的时间
     *
     * @param delayMillis 延时，毫秒
     */
    public Configuration setDiscoverServicesDelayMillis(int delayMillis) {
        this.discoverServicesDelayMillis = delayMillis;
        return this;
    }

    public long getDiscoverServicesDelayMillis() {
        return discoverServicesDelayMillis;
    }

    public int getTryReconnectTimes() {
        return tryReconnectTimes;
    }

    /**
     * 设置断开后尝试自动重连次数。-1为无限重连。默认为-1
     */
    public Configuration setTryReconnectTimes(int tryReconnectTimes) {
        this.tryReconnectTimes = tryReconnectTimes;
        return this;
    }

    public int getWriteDelayMillis() {
        return writeDelayMillis;
    }

    /**
     * 设置发送延时，默认不延时
     */
    public void setWriteDelayMillis(int writeDelayMillis) {
        this.writeDelayMillis = writeDelayMillis;
    }

    public int getScanPeriodMillis() {
        return scanPeriodMillis;
    }

    /**
     * 设置蓝牙扫描周期
     *
     * @param scanPeriodMillis 毫秒
     */
    public Configuration setScanPeriodMillis(int scanPeriodMillis) {
        this.scanPeriodMillis = scanPeriodMillis;
        return this;
    }

    public boolean isUseBluetoothLeScanner() {
        return useBluetoothLeScanner;
    }

    /**
     * 控制是否使用新版的扫描器
     */
    public void setUseBluetoothLeScanner(boolean useBluetoothLeScanner) {
        this.useBluetoothLeScanner = useBluetoothLeScanner;
    }

    public int getPackageSize() {
        return packageSize;
    }

    /**
     * 发送数据时的分包大小
     *
     * @param packageSize 包大小，字节
     */
    public Configuration setPackageSize(int packageSize) {
        this.packageSize = packageSize;
        return this;
    }

    public int getWriteType() {
        return writeType;
    }

    /**
     * 设置写入模式，默认的规则：如果Characteristic的属性有PROPERTY_WRITE_NO_RESPONSE则使用
     * {@link BluetoothGattCharacteristic#WRITE_TYPE_NO_RESPONSE}，否则使用{@link BluetoothGattCharacteristic#WRITE_TYPE_DEFAULT}
     *
     * @param writeType {@link BluetoothGattCharacteristic#WRITE_TYPE_NO_RESPONSE}<br>{@link BluetoothGattCharacteristic#WRITE_TYPE_DEFAULT}<br>
     *                  {@link BluetoothGattCharacteristic#WRITE_TYPE_SIGNED}
     */
    public void setWriteType(int writeType) {
        this.writeType = writeType;
    }

    public boolean isWaitWriteResult() {
        return waitWriteResult;
    }

    /**
     * 是否等待写入结果，不等待则直接处理下一个请求，否则等待onCharacteristicWrite回调后再处理下一请求，默认不等待。
     * 不等待的话也不会处理写入结果回调，也意味着不会发布写入结果的消息
     */
    public void setWaitWriteResult(boolean waitWriteResult) {
        this.waitWriteResult = waitWriteResult;
    }
}
