package cn.zfs.blelib.core;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import cn.zfs.blelib.callback.ConnectionCallback;
import cn.zfs.blelib.data.Device;

/**
 * 描述: 蓝牙连接
 * 时间: 2018/4/11 15:29
 * 作者: zengfansheng
 */
public class BleConnection extends Connection {
    private static final int MSG_ARG1_NONE = 0;
    private static final int MSG_ARG1_RELEASE = 1;
    private static final int MSG_ARG1_RECONNECT = 2;
    
    private static final int MSG_CONNECT = 1;
    private static final int MSG_DISCONNECT = 2;
    private static final int MSG_REFRESH= 3;
    private static final int MSG_AUTO_REFRESH = 4;
    private static final int MSG_TIMER = 5;
    private static final int MSG_RELEASE = 6;
    private static final int MSG_DISCOVER_SERVICES = 7;
    private static final int MSG_ON_CONNECTION_STATE_CHANGE = 8;
    private static final int MSG_ON_SERVICES_DISCOVERED = 9;
    
	private Device device;
	private Handler handler;
	private Context context;
	private ConnectionCallback connectionCallback;
	private long connStartTime;
    private boolean autoReconnEnable = true;//重连控制
	private int refreshTimes;//记录刷新次数，如果成功发现服务器，则清零
    private int tryReconnectTimes;
	    
    private BleConnection() {
        handler = new ConnHandler(this);
    }

    /**
     * 连接
     * @param device 蓝牙设备
     */
	synchronized static BleConnection newInstance(BluetoothAdapter bluetoothAdapter, Context context, Device device, 
                                                  long connectDelay, ConnectionCallback connectionCallback) {
		if (bluetoothAdapter == null || device == null || device.addr == null || !device.addr.matches("^[0-9A-F]{2}(:[0-9A-F]{2}){5}$")) {
			Ble.println(BleConnection.class, Log.ERROR, "BluetoothAdapter not initialized or unspecified address.");
			Ble.getInstance().getObservable().nofityUnableConnect(device, "BluetoothAdapter not initialized or unspecified address.");
			return null;
		}
		//初始化并建立连接
		BleConnection conn = new BleConnection();
		conn.bluetoothAdapter = bluetoothAdapter;
		conn.device = device;
		conn.bluetoothDevice = conn.bluetoothAdapter.getRemoteDevice(device.addr);
		conn.context = context.getApplicationContext();
		conn.connectionCallback = connectionCallback;
		//连接蓝牙设备        
        conn.device.connectionState = STATE_CONNECTING;
        conn.connStartTime = System.currentTimeMillis();
        Ble.getInstance().getObservable().notifyConnectionStateChange(device, STATE_CONNECTING);
        conn.handler.sendEmptyMessageDelayed(MSG_CONNECT, connectDelay);//连接
        conn.handler.sendEmptyMessageDelayed(MSG_TIMER, connectDelay + 1000);//启动定时器，用于断线重连
		return conn;
	}

    @Override
    protected int getWriteDelayMillis() {
        return Ble.getInstance().getConfig().getWriteDelayMillis();
    }

    @Override
    protected int getPackageSize() {
        return Ble.getInstance().getConfig().getPackageSize();
    }

    /**
     * 获取当前连接的设备
     */
    public Device getDevice() {
        return device;
    }

    /**
     * 获取蓝牙服务列表
     */
    public List<BluetoothGattService> getGattServices() {
	    if (bluetoothGatt != null) {
	        return bluetoothGatt.getServices();
	    }
	    return new ArrayList<>();
    }
    
    public synchronized void onScanResult(String addr) {
	    if (device.addr.equals(addr) && device.connectionState == STATE_RECONNECTING) {
            device.connectionState = STATE_CONNECTING;
            Ble.getInstance().getObservable().notifyConnectionStateChange(device, STATE_CONNECTING);
            handler.sendEmptyMessage(MSG_CONNECT);
	    }
    }
    
    public synchronized void onScanStop() {
	    if (device.connectionState == STATE_RECONNECTING) {
	        if (Ble.getInstance().getConfig().getTryReconnectTimes() == BleConfig.TRY_RECONNECT_TIMES_INFINITE ||
                    tryReconnectTimes < Ble.getInstance().getConfig().getTryReconnectTimes()) {
	            tryReconnectTimes++;
                tryReconnect();
	        } else {
                notifyDisconnected();
	        }	        
	    }
    }
        
	private static class ConnHandler extends Handler {
        private WeakReference<BleConnection> ref;

        ConnHandler(BleConnection conn) {
            super(Looper.getMainLooper());
            ref = new WeakReference<>(conn);
        }

        @Override
        public void handleMessage(Message msg) {
            final BleConnection conn = ref.get();
            if (conn == null) {
                return;
            }
            if (!conn.bluetoothAdapter.isEnabled()) {
                conn.notifyDisconnected();
            } else {
                switch(msg.what) {
                    case MSG_CONNECT://连接
                        conn.doConnect();
                		break;
                    case MSG_DISCONNECT://处理断开
                        conn.doDisconnect(msg.arg2 == MSG_ARG1_RECONNECT, msg.arg1 == MSG_ARG1_RELEASE);
                		break;
                    case MSG_REFRESH://手动刷新
                        conn.doRefresh(false);
                        break;
                    case MSG_AUTO_REFRESH://自动刷新
                        conn.doRefresh(true);
                        break;
                    case MSG_RELEASE://销毁连接
                        conn.autoReconnEnable = false;//停止重连
                        conn.doDisconnect(false, true);
                        break;
                    case MSG_TIMER://定时器
                        conn.doTimer();
                        break;
                    case MSG_DISCOVER_SERVICES://开始发现服务
                        conn.doDiscoverServices();
                        break;
                    case MSG_ON_CONNECTION_STATE_CHANGE://连接状态变化
                    case MSG_ON_SERVICES_DISCOVERED://发现服务
                        BluetoothGatt gatt = (BluetoothGatt) msg.obj;
                        int status = msg.arg1;
                        int newState = msg.arg2;
                        if (msg.what == MSG_ON_SERVICES_DISCOVERED) {
                            conn.doOnServicesDiscovered(gatt, status);
                        } else {
                            conn.doOnConnectionStateChange(gatt, status, newState);
                        }
                        break;
                }
            }
        }
    }
    
    private void notifyDisconnected() {
        device.connectionState = STATE_DISCONNECTED;
        sendConnectionCallback();
        Ble.getInstance().getObservable().notifyConnectionStateChange(device, STATE_DISCONNECTED);
    }
    
    private void doOnConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
            Ble.println(BleConnection.class, Log.DEBUG, "连接状态：STATE_CONNECTED, " +
                    gatt.getDevice().getName() + ", " + gatt.getDevice().getAddress());
            device.connectionState = STATE_CONNECTED;
            sendConnectionCallback();
            Ble.getInstance().getObservable().notifyConnectionStateChange(device, STATE_CONNECTED);
            // 进行服务发现，延时
            handler.sendEmptyMessageDelayed(MSG_DISCOVER_SERVICES, Ble.getInstance().getConfig().getDiscoverServicesDelayMillis());
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            Ble.println(BleConnection.class, Log.DEBUG, "连接状态：STATE_DISCONNECTED, " +
                    gatt.getDevice().getName() + ", " + gatt.getDevice().getAddress() + ", autoReconnEnable: " + autoReconnEnable);
            notifyDisconnected();
        } else if (status == 133) {
            doClearTaskAndRefresh(true);
            Ble.println(BleConnection.class, Log.ERROR, "onConnectionStateChange error, status: " + status + ", " +
                    gatt.getDevice().getName() + ", " + gatt.getDevice().getAddress());
        }
    }
    
    private void doOnServicesDiscovered(BluetoothGatt gatt, int status) {
        List<BluetoothGattService> services = gatt.getServices();
        if (status == BluetoothGatt.GATT_SUCCESS) {
            Ble.println(BleConnection.class, Log.DEBUG, "onServicesDiscovered. " + gatt.getDevice().getName() + ", " +
                    gatt.getDevice().getAddress() + ", 服务列表长度: " + gatt.getServices().size());
            if (services.isEmpty()) {
                doClearTaskAndRefresh(true);
            } else {
                refreshTimes = 0;
                tryReconnectTimes = 0;
                device.connectionState = STATE_SERVICE_DISCORVERED;
                sendConnectionCallback();
                Ble.getInstance().getObservable().notifyConnectionStateChange(device, STATE_SERVICE_DISCORVERED);
            }
        } else {
            doClearTaskAndRefresh(true);
            Ble.println(BleConnection.class, Log.ERROR, "onServicesDiscovered error, status: " + status + ", " +
                    gatt.getDevice().getName() + ", " + gatt.getDevice().getAddress());
        }
    }
    
    private void doDiscoverServices() {
        if (bluetoothGatt != null) {
            bluetoothGatt.discoverServices();
            device.connectionState = STATE_SERVICE_DISCORVERING;
            sendConnectionCallback();
            Ble.getInstance().getObservable().notifyConnectionStateChange(device, STATE_SERVICE_DISCORVERING);
        } else {
            notifyDisconnected();
        }
    }
    
    private void doTimer() {
        //连接超时。
        if (device.connectionState != STATE_SERVICE_DISCORVERED && System.currentTimeMillis() - connStartTime > 
                Ble.getInstance().getConfig().getConnectTimeoutMillis()) {
            if (device.connectionState != STATE_DISCONNECTED) {
                connStartTime = System.currentTimeMillis();
                Ble.println(BleConnection.class, Log.ERROR, "连接超时, " + device.name + ", " + device.addr);
                int type;
                if (device.connectionState == STATE_RECONNECTING) {
                    type = TIMEOUT_TYPE_CANNOT_DISCOVER_DEVICE;
                } else if (device.connectionState == STATE_CONNECTING) {
                    type = TIMEOUT_TYPE_CANNOT_CONNECT;
                } else {
                    type = TIMEOUT_TYPE_CANNOT_DISCOVER_SERVICES;
                }
                Ble.getInstance().getObservable().notifyConnectTimeout(device, type);
            }
            if (autoReconnEnable) {
                doDisconnect(true, false);
            } else {
                doDisconnect(false, false);
            }
        }
        handler.sendEmptyMessageDelayed(MSG_TIMER, 1000);
    }
    
    //处理刷新
    private void doRefresh(boolean isAuto) {
	    connStartTime = System.currentTimeMillis();//防止刷新过程自动重连
        if (bluetoothGatt != null) {
            bluetoothGatt.disconnect();
            if (isAuto) {
                if (refreshTimes <= 5) {
                    refresh(bluetoothGatt);
                }
                refreshTimes++;
            } else {
                refresh(bluetoothGatt);
            }
            bluetoothGatt.close();
        }
        notifyDisconnected();
    }
    
    private void doConnect() {
        //连接时需要停止蓝牙扫描
        Ble.getInstance().stopScan();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                bluetoothGatt = bluetoothDevice.connectGatt(context, false, BleConnection.this);
            }
        }, 500);
    }
    
    private void doDisconnect(boolean reconnect, boolean release) {
	    doClearTaskAndRefresh(false);
        if (bluetoothGatt != null) {
            bluetoothGatt.close();
        }
        device.connectionState = STATE_DISCONNECTED;
        if (release) {//销毁
            bluetoothGatt = null;
            Ble.println(BleConnection.class, Log.DEBUG, "连接销毁释放");
        } else if (reconnect) {
            device.connectionState = STATE_RECONNECTING;
            tryReconnect();
        }        
        sendConnectionCallback();
        Ble.getInstance().getObservable().notifyConnectionStateChange(device, device.connectionState);
    }

    private void tryReconnect() {        
        connStartTime = System.currentTimeMillis();
        //开启扫描，扫描到才连接
        Ble.getInstance().startScan(context);
    }

    private void doClearTaskAndRefresh(boolean refresh) {
        requestQueue.clear();
        currentRequest = null;
        if (refresh) {
            doRefresh(true);
        }       
    }
    
    private void sendConnectionCallback() {
	    if (connectionCallback != null) {
	        connectionCallback.onConnectionStateChange(device.connectionState);
	    }
    }
    
    void setAutoReconnectEnable(boolean enable) {
        autoReconnEnable = enable;
    }

    boolean isAutoReconnectEnabled() {
        return autoReconnEnable;
    }
    
    public synchronized void reconnect() {
	    tryReconnectTimes = 0;
        handler.removeMessages(MSG_TIMER);//停止定时器
        handler.sendMessage(Message.obtain(handler, MSG_DISCONNECT, MSG_ARG1_RECONNECT));
        handler.sendEmptyMessageDelayed(MSG_TIMER, 1000);//重启定时器
	}

    public synchronized void disconnect() {
        handler.removeMessages(MSG_TIMER);//主动断开，停止定时器
        handler.sendMessage(Message.obtain(handler, MSG_DISCONNECT, MSG_ARG1_NONE));
	}

    /**
     * 清理缓存
     */
    public void refresh() {
        handler.sendEmptyMessage(MSG_REFRESH);
    }
    
	/**
	 * 销毁连接，停止定时器
	 */
	@Override
	public void release() {
	    super.release();
	    handler.removeCallbacksAndMessages(null);//主动断开，停止定时器
		handler.sendEmptyMessage(MSG_RELEASE);
	}

    public int getConnState() {
		return device.connectionState;
	}
	
	@Override
	public void onConnectionStateChange(final BluetoothGatt gatt, int status, int newState) {
	    handler.sendMessage(Message.obtain(handler, MSG_ON_CONNECTION_STATE_CHANGE, status, newState, gatt));
	}

    
    
	@Override
	public void onServicesDiscovered(final BluetoothGatt gatt, int status) {
        handler.sendMessage(Message.obtain(handler, MSG_ON_SERVICES_DISCOVERED, status, 0, gatt));
	}
}
