package cn.zfs.blelib.core;

import android.Manifest;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.zfs.blelib.callback.ConnectionCallback;
import cn.zfs.blelib.callback.InitCallback;
import cn.zfs.blelib.callback.ScanListener;
import cn.zfs.blelib.event.Events;
import cn.zfs.blelib.util.LogController;

/**
 * 描述: 蓝牙操作
 * 时间: 2018/4/11 15:29
 * 作者: zengfansheng
 */
public class Ble {        
    private BluetoothAdapter bluetoothAdapter;
    private Map<String, Connection> connectionMap;
    private boolean isInited;
    private boolean scanning;
    private BluetoothLeScanner bleScanner;
    private ScanCallback scanCallback;
    private BluetoothAdapter.LeScanCallback leScanCallback;    
    private Configuration configuration;
    private List<ScanListener> scanListeners;
    private Handler mainThreadHandler;
    private ExecutorService executorService;
    private EventBus publisher;

    private Ble() {
        configuration = new Configuration();
        connectionMap = new ConcurrentHashMap<>();
        mainThreadHandler = new Handler(Looper.getMainLooper());
        scanListeners = new ArrayList<>();
        executorService = Executors.newCachedThreadPool();
        publisher = EventBus.builder().build();
        LogController.printLevelControl = LogController.NONE;
    }

    private static class Holder {
        private static final Ble BLE = new Ble();
    }

    public static Ble getInstance() {
        return Holder.BLE;
    }

    /**
     * 设置日志输出级别控制
     * @param logPrintLevel <br>{@link LogController#NONE}, {@link LogController#VERBOSE}, 
     * {@link LogController#DEBUG}, {@link LogController#INFO}, {@link LogController#WARN}, {@link LogController#ERROR}
     */
    public void setLogPrintLevelControl(int logPrintLevel) {
        LogController.printLevelControl = logPrintLevel;
    }
    
    public Configuration getConfiguration() {
        return configuration;
    }

    /**
     * 替换默认配置
     */
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }
    
    /**
     * 必须先初始化
     * @param context 上下文
     * @param callback 初始化结果回调
     */
    public void initialize(Context context, InitCallback callback) {
        context = context.getApplicationContext();
        //检查手机是否支持BLE
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            if (callback != null) {
                callback.onFail(InitCallback.ERROR_NOT_SUPPORT_BLE);
            }
            return;
        }
        //获取蓝牙管理器
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            if (callback != null) {
                callback.onFail(InitCallback.ERROR_INIT_FAIL);
            }
            return;
        }
        //编译版本>=24时需要定位权限，否则扫描不到蓝牙
        //检查是否拥有定位权限
        if (noLocationPermission(context)) {
            if (callback != null) {
                callback.onFail(InitCallback.ERROR_LACK_PERMISSION);
            }
            return;
        }
        bluetoothAdapter = bluetoothManager.getAdapter();
        //未初始化过才注册，保证广播注册和取消注册成对
        if (!isInited) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
            context.registerReceiver(receiver, filter);
        }
        isInited = true;        
        if (callback != null) {
            callback.onSuccess();
        }
    }

    /**
     * 关闭所有连接，释放资源
     */
    public void release(Context context) {
        if (isInited) {
            stopScan();
            scanListeners.clear();
            releaseAllConnections();//释放所有连接
            context.getApplicationContext().unregisterReceiver(receiver);//取消注册蓝牙状态广播接收者
            isInited = false;
        }
    }
    
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {//蓝牙开关状态变化                 
                if (bluetoothAdapter != null) {
                    publisher.post(new Events.BluetoothStateChanged(bluetoothAdapter.getState()));
                    if (bluetoothAdapter.getState() == BluetoothAdapter.STATE_OFF) {//蓝牙关闭了
                        handleScanCallback(false, null, null);
                        //主动断开，停止定时器和重连尝试
                        for (Connection connection : connectionMap.values()) {
                            connection.disconnect();
                        }
                    } else if (bluetoothAdapter.getState() == BluetoothAdapter.STATE_ON) {
                        for (Connection connection : connectionMap.values()) {
                            if (connection.isAutoReconnectEnabled()) {
                                connection.reconnect();//如果开启了自动重连，则重连
                            }
                        }
                    }
                }
            }
        }
    };
    
    //是否缺少定位权限
    private boolean noLocationPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED;
    }
    
    /**
     * 是否已初始化过
     */
    public boolean isInitialized() {
        return isInited;
    }
    
    public ExecutorService getExecutorService() {
        return executorService;
    }
    
    public EventBus getPublisher() {
        return publisher;
    }
    
    /**
     * 注册订阅者，开始监听蓝牙状态及数据
     * @param subscriber 订阅者
     */
    public void registerSubscriber(Object subscriber) {
        if (!publisher.isRegistered(subscriber)) {
            publisher.register(subscriber);
        }
    }

    /**
     * 取消注册订阅者，停止监听蓝牙状态及数据
     * @param subscriber 订阅者
     */
    public void unregisterSubscriber(Object subscriber) {
        publisher.unregister(subscriber);
    }

    /**
     * 添加扫描监听器
     */
    public void addScanListener(ScanListener listener) {
        if (listener != null && !scanListeners.contains(listener)) {
            scanListeners.add(listener);
        }
    }

    /**
     * 移除扫描监听器
     */
    public void removeScanListener(ScanListener listener) {
        scanListeners.remove(listener);
    }

    /**
     * 是否正在扫描
     */
    public boolean isScanning() {
        return scanning;
    }
    
    public BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
    }
    
    public boolean isBluetoothAdapterEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }
    
    /**
     * 搜索蓝牙设备
     * @param context 用来检查app是否拥有相应权限
     */
    public void startScan(Context context) {        
        if (noLocationPermission(context)) {
            println(Ble.class, Log.ERROR, "缺少定位权限，无法扫描到蓝牙设备");
        }
        if (!isInited || bluetoothAdapter == null || !bluetoothAdapter.isEnabled() || scanning) {
            return;
        }
        getSystemConnectedDevices();
        
        //如果是高版本使用新的搜索方法
        if (configuration.isUseBluetoothLeScanner() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (bleScanner == null) {
                bleScanner = bluetoothAdapter.getBluetoothLeScanner();
            }
            if (scanCallback == null) {
                scanCallback = new MyScanCallback();
            }
            bleScanner.startScan(scanCallback);
        } else {
            if (leScanCallback == null) {
                leScanCallback = new MyLeScanCallback();
            }
            bluetoothAdapter.startLeScan(leScanCallback);
        }
        scanning = true;
        handleScanCallback(true, null, null);       
        mainThreadHandler.postDelayed(stopScanRunnable, configuration.getScanPeriodMillis());
    }

    private void handleScanCallback(final boolean start, final Device device, final byte[] scanRecord) {
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                for (ScanListener listener : scanListeners) {
                    if (device != null) {
                        listener.onScanResult(device, scanRecord);
                    } else if (start) {
                        listener.onScanStart();
                    } else {
                        listener.onScanStop();
                    }
                }
            }
        });
    }
    
    private Runnable stopScanRunnable = new Runnable() {
        @Override
        public void run() {
            stopScan();
        }
    };
    
    //获取系统已连接的设备
    private void getSystemConnectedDevices() {
        try {
            //得到连接状态的方法
            Method method = bluetoothAdapter.getClass().getDeclaredMethod("getConnectionState");
            //打开权限  
            method.setAccessible(true);
            int state = (int) method.invoke(bluetoothAdapter);
            if (state == BluetoothAdapter.STATE_CONNECTED) {
                Set<BluetoothDevice> devices = bluetoothAdapter.getBondedDevices();
                for (BluetoothDevice device : devices) {
                    Method isConnectedMethod = device.getClass().getDeclaredMethod("isConnected");
                    method.setAccessible(true);
                    boolean isConnected = (boolean) isConnectedMethod.invoke(device);
                    if (isConnected) {
                        parseScanResult(device, 0, null);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 停止搜索蓝牙设备
     */
    public void stopScan() {
        if (!isInited || !scanning) {
            return;
        }
        scanning = false;
        mainThreadHandler.removeCallbacks(stopScanRunnable);
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled())
            return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (bleScanner != null && scanCallback != null) {
                bleScanner.stopScan(scanCallback);
            }
        }
        if (leScanCallback != null) {
            bluetoothAdapter.stopLeScan(leScanCallback);
        }
        handleScanCallback(false, null, null);
        for (Connection connection : connectionMap.values()) {
            connection.onScanStop();
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private class MyScanCallback extends ScanCallback {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            ScanRecord scanRecord = result.getScanRecord();
            parseScanResult(result.getDevice(), result.getRssi(), scanRecord == null ? null : scanRecord.getBytes());
        }
    }

    private class MyLeScanCallback implements BluetoothAdapter.LeScanCallback {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            parseScanResult(device, rssi, scanRecord);
        }
    }

    /**
     * 解析广播字段
     * @param device 蓝牙设备
     * @param rssi 信号强度
     * @param scanRecord 广播内容
     */
    public void parseScanResult(BluetoothDevice device, int rssi, byte[] scanRecord) {
        for (Connection connection : connectionMap.values()) {
            connection.onScanResult(device.getAddress());
        }
        //生成
        final Device dev = createDevice();
        dev.name = TextUtils.isEmpty(device.getName()) ? "unknown device" : device.getName();
        dev.addr = device.getAddress();
        dev.rssi = rssi;
        if (configuration.getScanHandler() != null) {
            //只在指定的过滤器通知
            if (configuration.getScanHandler().handle(dev, scanRecord)) {
                handleScanCallback(false, dev, scanRecord);
            }
        } else {
            handleScanCallback(false, dev, scanRecord);
        }
        println(Ble.class, Log.DEBUG, "扫描到设备：" + dev.name + "  " + dev.addr);
    }
    
    private Device createDevice() {
        if (configuration.getDeviceClass() != null) {
            try {
                //通过反射创建Device
                return configuration.getDeviceClass().getConstructor().newInstance();
            } catch (Exception e) {
                //如果反射创建不成功，使用默认创建
                return new Device();
            }
        } else {
            return new Device();
        }
    }
        
    /**
     * 建立连接
     */
    public synchronized void connect(Context context, Device device, boolean autoReconnect) {
        if (!isInited) {
            return;
        }
        if (device != null) {
            Connection connection = connectionMap.get(device.addr);
            //此前这个设备建立过连接，销毁之前的连接重新创建
            if (connection != null) {
                connection.releaseNoEvnet();
            }
            ConnectionCallback callback = null;
            if (configuration.getConnectionCallbackClass() != null) {
                try {
                    //通过反射创建回调
                    callback = configuration.getConnectionCallbackClass().getConstructor(Device.class).newInstance(device);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            IBondController bondController = configuration.getBondController();
            if (bondController != null && bondController.bond(device)) {
                BluetoothDevice bd = bluetoothAdapter.getRemoteDevice(device.addr);
                if (bd.getBondState() == BluetoothDevice.BOND_BONDED) {
                    connection = Connection.newInstance(bluetoothAdapter, context, device, 0, callback);
                } else {
                    createBond(device.addr);//配对
                    connection = Connection.newInstance(bluetoothAdapter, context, device, 1500, callback);
                }                
            } else {
                connection = Connection.newInstance(bluetoothAdapter, context, device, 0, callback);
            }            
            if (connection != null) {
                connection.setAutoReconnectEnable(autoReconnect);
                connectionMap.put(device.addr, connection);
            }
        }
    }

    /**
     * 获取连接
     */
    public Connection getConnection(Device device) {
        if (device != null) {
            return connectionMap.get(device.addr);
        }
        return null;
    }

    /**
     * 获取连接
     */
    public Connection getConnection(String addr) {
        if (addr != null) {
            return connectionMap.get(addr);
        }
        return null;
    }

    /**
     * 获取连接状态
     * @return {@link Connection#STATE_DISCONNECTED}<br> {@link Connection#STATE_CONNECTING}<br>
     *              {@link Connection#STATE_RECONNECTING}<br> {@link Connection#STATE_CONNECTED}<br>
     *              {@link Connection#STATE_SERVICE_DISCORVERING}<br> {@link Connection#STATE_SERVICE_DISCORVERED}
     */
    public int getConnectionState(Device device) {
        Connection connection = getConnection(device);
        if (connection != null) {
            return connection.getConnState();
        }
        return Connection.STATE_DISCONNECTED;
    }

    /**
     * 根据设备断开其连接
     */
    public void disconnectConnection(Device device) {
        if (!isInited) {
            return;
        }
        if (device != null) {
            Connection connection = connectionMap.get(device.addr);
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 断开所有连接
     */
    public void disconnectAllConnection() {
        if (!isInited) {
            return;
        }
        for (Connection connection : connectionMap.values()) {
            connection.disconnect();
        }
    }

    /**
     * 释放所有创建的连接
     */
    public synchronized void releaseAllConnections() {
        if (!isInited) {
            return;
        }
        for (Connection connection : connectionMap.values()) {
            connection.release();
        }
        connectionMap.clear();
    }

    /**
     * 根据设备释放连接
     */
    public synchronized void releaseConnection(Device device) {
        if (!isInited) {
            return;
        }
        if (device != null) {
            Connection connection = connectionMap.remove(device.addr);
            if (connection != null) {
                connection.release();
            }
        }
    }

    /**
     * 重连所有创建的连接
     */
    public void reconnectAll() {
        if (!isInited) {
            return;
        }
        for (Connection connection : connectionMap.values()) {
            if (connection.getConnState() != Connection.STATE_SERVICE_DISCORVERED) {
                connection.reconnect();
            }
        }
    }

    /**
     * 根据设备重连
     */
    public void reconnect(Device device) {
        if (!isInited) {
            return;
        }
        if (device != null) {
            Connection connection = connectionMap.get(device.addr);
            if (connection != null && connection.getConnState() != Connection.STATE_SERVICE_DISCORVERED) {
                connection.reconnect();
            }
        }
    }

    /**
     * 设置是否可自动重连，所有已创建的连接
     */
    public void setAutoReconnectEnable(boolean enable) {
        for (Connection connection : connectionMap.values()) {
            connection.setAutoReconnectEnable(enable);
        }
    }

    /**
     * 设置是否可自动重连
     */
    public void setAutoReconnectEnable(Device device, boolean enable) {
        if (device != null) {
            Connection connection = connectionMap.get(device.addr);
            if (connection != null) {
                connection.setAutoReconnectEnable(enable);
            }
        }
    }

    /**
     * 刷新设备，清除缓存
     */
    public void refresh(Device device) {
        if (!isInited) {
            return;
        }
        if (device != null) {
            Connection connection = connectionMap.get(device.addr);
            if (connection != null) {
                connection.refresh();
            }
        }
    }

    /**
     * 获取设备配对状态
     * @return {@link BluetoothDevice#BOND_NONE},
     * {@link BluetoothDevice#BOND_BONDING},
     * {@link BluetoothDevice#BOND_BONDED}.
     */
    public int getBondState(String addr) {
        try {
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(addr);
            return device.getBondState();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return BluetoothDevice.BOND_NONE;
    }

    /**
     * 绑定设备
     */
    public boolean createBond(String addr) {
        try {
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(addr);
            return device.getBondState() != BluetoothDevice.BOND_NONE || device.createBond();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 根据设置的过滤器清除已配对的设备
     */
    public void clearBondDevices(RemoveBondFilter filter) {
        if (!isInited) {
            return;
        }
        Set<BluetoothDevice> devices = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : devices) {
            if (filter == null || filter.accept(device)) {
                try {
                    device.getClass().getMethod("removeBond").invoke(device);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 取消配对
     */
    public void removeBond(String addr) {
        if (!isInited) {
            return;
        }
        try {
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(addr);
            if (device.getBondState() != BluetoothDevice.BOND_NONE) {
                device.getClass().getMethod("removeBond").invoke(device);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void println(Class cls, int priority, String msg) {
        if (LogController.accept(priority)) {
            Log.println(priority, "blelib:" + cls.getSimpleName(), "blelib--" + msg);
        }
    }
}
