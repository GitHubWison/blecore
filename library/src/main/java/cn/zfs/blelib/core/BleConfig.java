package cn.zfs.blelib.core;

import android.util.Log;

import cn.zfs.blelib.callback.ConnectionCallback;
import cn.zfs.blelib.callback.RequestCallback;
import cn.zfs.blelib.data.BleObservable;
import cn.zfs.blelib.data.Device;

/**
 * 描述: 蓝牙配置
 * 时间: 2018/4/17 17:02
 * 作者: zengfansheng
 */
public class BleConfig {
    private static final int DEFAULT_DISCOVER_SERVICES_DELAY_MILLIS = 500;
    private static final int DEFAULT_CONN_TIMEOUT_MILLIS = 8000;//连接超时时间
    public static final int TRY_RECONNECT_TIMES_INFINITE = -1;//无限重连
    private IScanHandler scanHandler;    
    private long discoverServicesDelayMillis = DEFAULT_DISCOVER_SERVICES_DELAY_MILLIS;
    private int connectTimeoutMillis = DEFAULT_CONN_TIMEOUT_MILLIS;
    private BleObservable observable = new BleObservable();
    private Class<? extends RequestCallback> requestCallbackClass;
    private Class<? extends ConnectionCallback> connectionCallbackClass;
    private Class<? extends Device> deviceClass;
    private int tryReconnectTimes = TRY_RECONNECT_TIMES_INFINITE;
    private int writeDelayMillis;
    private int scanPeriodMillis = 10000;
    private boolean useBluetoothLeScanner = true;
    private int packageSize = 20;//发送数据时的分包大小
    
    public Class<? extends Device> getDeviceClass() {
        return deviceClass;
    }

    public BleConfig setDeviceClass(Class<? extends Device> deviceClass) {
        this.deviceClass = deviceClass;
        Ble.println(this.getClass(), Log.DEBUG, "setDeviceClass");
        return this;
    }

    public BleObservable getObservable() {
        return observable;
    }

    /**
     * 设置扫描过滤器
     * @param handler 扫描结果处理
     */
    public BleConfig setScanHandler(IScanHandler handler) {
        scanHandler = handler;
        return this;
    }

    public IScanHandler getScanHandler() {
        return scanHandler;
    }

    public BleConfig setObservable(BleObservable observable) {
        this.observable = observable;
        return this;
    }

    public Class<? extends ConnectionCallback> getConnectionCallbackClass() {
        return connectionCallbackClass;
    }

    /**
     * 设置连接状态变化回调
     */
    public BleConfig setConnectionCallbackClass(Class<? extends ConnectionCallback> connectionCallbackClass) {
        this.connectionCallbackClass = connectionCallbackClass;
        return this;
    }

    public Class<? extends RequestCallback> getRequestCallbackClass() {
        return requestCallbackClass;
    }

    /**
     * 设置请求回调，数据交互
     */
    public BleConfig setRequestCallbackClass(Class<? extends RequestCallback> requestCallbackClass) {
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
    public BleConfig setConnectTimeoutMillis(int connectTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
        return this;
    }

    /**
     * 设置连接成功后，延时发现服务的时间
     * @param delayMillis 延时，毫秒
     */
    public BleConfig setDiscoverServicesDelayMillis(int delayMillis) {
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
    public BleConfig setTryReconnectTimes(int tryReconnectTimes) {
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
     * @param scanPeriodMillis 毫秒
     */
    public BleConfig setScanPeriodMillis(int scanPeriodMillis) {
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
     * @param packageSize 包大小，字节
     */
    public BleConfig setPackageSize(int packageSize) {
        this.packageSize = packageSize;
        return this;
    }
}
