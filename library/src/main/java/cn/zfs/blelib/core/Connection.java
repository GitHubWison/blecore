package cn.zfs.blelib.core;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import cn.zfs.blelib.callback.ConnectionStateChangeListener;
import cn.zfs.blelib.event.Events;
import cn.zfs.blelib.util.BleUtils;

/**
 * 描述: 蓝牙连接
 * 时间: 2018/4/11 15:29
 * 作者: zengfansheng
 */
public class Connection extends BaseConnection {
    private static final int MSG_ARG_NONE = 0;
    private static final int MSG_ARG_RECONNECT = 1;
    private static final int MSG_ARG_NOTIFY = 2;
    
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
	private ConnectionStateChangeListener stateChangeListener;
	private long connStartTime;
    private boolean autoReconnEnable = true;//重连控制
	private int refreshTimes;//记录刷新次数，如果成功发现服务器，则清零
    private int tryReconnectTimes;
    private int lastConnectState = -1;
	    
    private Connection(BluetoothDevice bluetoothDevice) {
        super(bluetoothDevice);
        handler = new ConnHandler(this);
    }

    /**
     * 连接
     * @param device 蓝牙设备
     */
	synchronized static Connection newInstance(@NonNull BluetoothAdapter bluetoothAdapter, @NonNull Context context, @NonNull Device device,
                                               long connectDelay, ConnectionStateChangeListener stateChangeListener) {
		if (device.addr == null || !device.addr.matches("^[0-9A-F]{2}(:[0-9A-F]{2}){5}$")) {
            Ble.println(Connection.class, Log.ERROR, String.format(Locale.US, "CONNECT FAILED [type: unspecified mac address, name: %s, mac: %s]",
                    device.name, device.addr));
			notifyConnectFailed(device, CONNECT_FAIL_TYPE_UNSPECIFIED_MAC_ADDRESS, stateChangeListener);
			return null;
		}
		//初始化并建立连接
		Connection conn = new Connection(bluetoothAdapter.getRemoteDevice(device.addr));
		conn.bluetoothAdapter = bluetoothAdapter;
		conn.device = device;
		conn.context = context.getApplicationContext();
		conn.stateChangeListener = stateChangeListener;
		//连接蓝牙设备        
        conn.device.connectionState = STATE_CONNECTING;
        conn.connStartTime = System.currentTimeMillis();
        conn.sendConnectionCallback();
        conn.handler.sendEmptyMessageDelayed(MSG_CONNECT, connectDelay);//连接
        conn.handler.sendEmptyMessageDelayed(MSG_TIMER, connectDelay);//启动定时器，用于断线重连
		return conn;
	}

    /**
     * 获取当前连接的设备
     */
    public Device getDevice() {
        return device;
    }
    
    public BluetoothGatt getBluetoothGatt() {
        return bluetoothGatt;
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
	    if (!isReleased && device.addr.equals(addr) && device.connectionState == STATE_RECONNECTING) {
            device.connectionState = STATE_CONNECTING;
            sendConnectionCallback();
            handler.sendEmptyMessage(MSG_CONNECT);
	    }
    }
    
    public synchronized void onScanStop() {
	    if (!isReleased && device.connectionState == STATE_RECONNECTING) {
	        if (Ble.getInstance().getConfiguration().getTryReconnectTimes() == Configuration.TRY_RECONNECT_TIMES_INFINITE ||
                    tryReconnectTimes < Ble.getInstance().getConfiguration().getTryReconnectTimes()) {
	            tryReconnectTimes++;
                tryReconnect();
	        }        
	    }
    }

    private static class ConnHandler extends Handler {
        private WeakReference<Connection> ref;

        ConnHandler(Connection conn) {
            super(Looper.getMainLooper());
            ref = new WeakReference<>(conn);
        }

        @Override
        public void handleMessage(Message msg) {
            final Connection conn = ref.get();
            if (conn == null) {
                return;
            }
            if (conn.bluetoothAdapter.isEnabled()) {
                switch(msg.what) {
                    case MSG_CONNECT://连接
                        conn.doConnect();
                		break;
                    case MSG_DISCONNECT://处理断开
                        conn.doDisconnect(msg.arg2 == MSG_ARG_RECONNECT, true);
                		break;
                    case MSG_REFRESH://手动刷新
                        conn.doRefresh(false);
                        break;
                    case MSG_AUTO_REFRESH://自动刷新
                        conn.doRefresh(true);
                        break;
                    case MSG_RELEASE://销毁连接
                        conn.autoReconnEnable = false;//停止重连
                        conn.doDisconnect(false, msg.arg1 == MSG_ARG_NOTIFY);
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
    }
    
    private static void notifyConnectFailed(Device device, int type, ConnectionStateChangeListener listener) {
        if (listener != null) {
            listener.onConnectFailed(device, type);
        }
        Ble.getInstance().postEvent(Events.newConnectFailed(device, type));
    }
    
    private void doOnConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
            Ble.println(Connection.class, Log.DEBUG, String.format(Locale.US, "CONNECTED [name: %s, mac: %s]",
                    gatt.getDevice().getName(), gatt.getDevice().getAddress()));           
            device.connectionState = STATE_CONNECTED;
            sendConnectionCallback();            
            // 进行服务发现，延时
            handler.sendEmptyMessageDelayed(MSG_DISCOVER_SERVICES, Ble.getInstance().getConfiguration().getDiscoverServicesDelayMillis());
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            Ble.println(Connection.class, Log.DEBUG, String.format(Locale.US, "DISCONNECTED [name: %s, mac: %s, autoReconnEnable: %s]",
                    gatt.getDevice().getName(), gatt.getDevice().getAddress(), String.valueOf(autoReconnEnable)));
            clearRequestQueue();
            notifyDisconnected();
        } else if (status == 133) {
            doClearTaskAndRefresh(true);
            Ble.println(Connection.class, Log.ERROR, String.format(Locale.US, "GATT ERROR [name: %s, mac: %s, status: %d]",
                    gatt.getDevice().getName(), gatt.getDevice().getAddress(), status));
        }
    }
    
    private void doOnServicesDiscovered(BluetoothGatt gatt, int status) {        
        List<BluetoothGattService> services = gatt.getServices();
        if (status == BluetoothGatt.GATT_SUCCESS) {
            Ble.println(Connection.class, Log.DEBUG, String.format(Locale.US, "SERVICES DISCOVERED [name: %s, mac: %s, size: %d]",
                    gatt.getDevice().getName(), gatt.getDevice().getAddress(), gatt.getServices().size()));
            if (services.isEmpty()) {
                doClearTaskAndRefresh(true);
            } else {
                refreshTimes = 0;
                tryReconnectTimes = 0;
                device.connectionState = STATE_SERVICE_DISCOVERED;
                sendConnectionCallback();
            }
        } else {
            doClearTaskAndRefresh(true);
            Ble.println(Connection.class, Log.ERROR, String.format(Locale.US, "GATT ERROR [status: %d, name: %s, mac: %s]",
                    status, gatt.getDevice().getName(), gatt.getDevice().getAddress()));
        }
    }
    
    private void doDiscoverServices() {
        if (bluetoothGatt != null) {
            bluetoothGatt.discoverServices();
            device.connectionState = STATE_SERVICE_DISCOVERING;
            sendConnectionCallback();
        } else {
            notifyDisconnected();
        }
    }
    
    private void doTimer() {
        //只处理不在连接状态的
        if (!isReleased && device.connectionState != STATE_SERVICE_DISCOVERED) {
            if (device.connectionState != STATE_DISCONNECTED) {
                //超时
                if (System.currentTimeMillis() - connStartTime > Ble.getInstance().getConfiguration().getConnectTimeoutMillis()) {
                    connStartTime = System.currentTimeMillis();
                    Ble.println(Connection.class, Log.ERROR, String.format(Locale.US, "CONNECT TIMEOUT [name: %s, mac: %s]", device.name, device.addr));
                    int type;
                    if (device.connectionState == STATE_RECONNECTING) {
                        type = TIMEOUT_TYPE_CANNOT_DISCOVER_DEVICE;
                    } else if (device.connectionState == STATE_CONNECTING) {
                        type = TIMEOUT_TYPE_CANNOT_CONNECT;
                    } else {
                        type = TIMEOUT_TYPE_CANNOT_DISCOVER_SERVICES;
                    }
                    Ble.getInstance().postEvent(Events.newConnectTimeout(device, type));
                    if (autoReconnEnable && Ble.getInstance().getConfiguration().getTryReconnectTimes() == Configuration.TRY_RECONNECT_TIMES_INFINITE ||
                            tryReconnectTimes < Ble.getInstance().getConfiguration().getTryReconnectTimes()) {
                        doDisconnect(true, true);
                    } else {
                        doDisconnect(false, true);
                        notifyConnectFailed(device, CONNECT_FAIL_TYPE_MAXIMUM_RECONNECTION, stateChangeListener);
                        Ble.println(Connection.class, Log.ERROR, String.format(Locale.US, "CONNECT FAILED [type: maximun reconnection, name: %s, mac: %s]", 
                                device.name, device.addr));
                    }
                }                
            } else if (autoReconnEnable) {
                doDisconnect(true, true);
            }
        }
        handler.sendEmptyMessageDelayed(MSG_TIMER, 500);
    }
        
    //处理刷新
    private void doRefresh(boolean isAuto) {
        Ble.println(Connection.class, Log.DEBUG, String.format(Locale.US, "REFRESH GATT [name: %s, mac: %s]", device.name, device.addr));
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
        Ble.println(Connection.class, Log.DEBUG, String.format(Locale.US, "CONNECTING [name: %s, mac: %s]", device.name, device.addr));
        //连接时需要停止蓝牙扫描
        Ble.getInstance().stopScan();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isReleased) {
                    bluetoothGatt = bluetoothDevice.connectGatt(context, false, Connection.this);
                }
            }
        }, 500);
    }
    
    private void doDisconnect(boolean reconnect, boolean notify) {
	    doClearTaskAndRefresh(false);
        if (bluetoothGatt != null) {
            bluetoothGatt.disconnect();
            bluetoothGatt.close();
        }
        device.connectionState = STATE_DISCONNECTED;
        if (isReleased) {//销毁
            device.connectionState = STATE_RELEASED;
            bluetoothGatt = null;
            Ble.println(Connection.class, Log.DEBUG, String.format(Locale.US, "CONNECTION RELEASED [name: %s, mac: %s]", device.name, device.addr));
        } else if (reconnect) {
            device.connectionState = STATE_RECONNECTING;
            tryReconnect();
        }        
        if (notify) {
            sendConnectionCallback();
        }
    }

    private void tryReconnect() {        
        if (!isReleased) {
            Ble.println(Connection.class, Log.DEBUG, String.format(Locale.US, "RECONNECTING [name: %s, mac: %s]", device.name, device.addr));
            connStartTime = System.currentTimeMillis();
            //开启扫描，扫描到才连接
            Ble.getInstance().startScan(context);
        }
    }

    private void doClearTaskAndRefresh(boolean refresh) {
        clearRequestQueue();
        if (refresh) {
            doRefresh(true);
        }       
    }
    
    private void sendConnectionCallback() {
	    if (lastConnectState != device.connectionState) {
            if (stateChangeListener != null) {
                stateChangeListener.onConnectionStateChanged(device);
            }
            Ble.getInstance().postEvent(Events.newConnectionStateChanged(device, device.connectionState));
	    }
	    lastConnectState = device.connectionState;
    }
    
    void setAutoReconnectEnable(boolean enable) {
        autoReconnEnable = enable;
    }

    boolean isAutoReconnectEnabled() {
        return autoReconnEnable;
    }
    
    public boolean reconnect() {
	    if (!isReleased) {
            tryReconnectTimes = 0;
            handler.removeMessages(MSG_TIMER);//停止定时器
            Message.obtain(handler, MSG_DISCONNECT, MSG_ARG_RECONNECT).sendToTarget();
            handler.sendEmptyMessageDelayed(MSG_TIMER, 500);//重启定时器
            return true;
	    }
	    return false;
	}

    public void disconnect() {
        if (!isReleased) {
            handler.removeMessages(MSG_TIMER);//主动断开，停止定时器
            Message.obtain(handler, MSG_DISCONNECT, MSG_ARG_NONE).sendToTarget();
        }
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
        Message.obtain(handler, MSG_RELEASE, MSG_ARG_NOTIFY, 0).sendToTarget();
	}

    /**
     * 销毁连接，不发布消息
     */
    public void releaseNoEvnet() {
        super.release();
        handler.removeCallbacksAndMessages(null);//主动断开，停止定时器
        Message.obtain(handler, MSG_RELEASE, MSG_ARG_NONE, 0).sendToTarget();
    }
	
    public int getConnState() {
		return device.connectionState;
	}
	
	@Override
	public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        if (isReleased) {
            gatt.disconnect();
            gatt.close();
        } else {
            handler.sendMessage(Message.obtain(handler, MSG_ON_CONNECTION_STATE_CHANGE, status, newState, gatt));
        }	    
	}  
    
	@Override
	public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        if (isReleased) {
            gatt.disconnect();
            gatt.close();
        } else {
            handler.sendMessage(Message.obtain(handler, MSG_ON_SERVICES_DISCOVERED, status, 0, gatt));
        }
	}

	private String getHex(byte[] value) {
        return BleUtils.bytesToHexString(value).trim();
    }
	
    @Override
    public void onCharacteristicRead(@NonNull String requestId, BluetoothGattCharacteristic characteristic) {
        Ble.getInstance().postEvent(Events.newCharacteristicRead(device, requestId, characteristic));
        Ble.println(Connection.class, Log.DEBUG, String.format(Locale.US, "CHARACTERISTIC READ [mac: %s, value: %s]", device.addr, getHex(characteristic.getValue())));
    }

    @Override
    public void onCharacteristicChanged(BluetoothGattCharacteristic characteristic) {
        Ble.getInstance().postEvent(Events.newCharacteristicChanged(device, characteristic));
        Ble.println(Connection.class, Log.INFO, String.format(Locale.US, "CHARACTERISTIC CHANGE [mac: %s, value: %s]", device.addr, getHex(characteristic.getValue())));
    }

    @Override
    public void onReadRemoteRssi(@NonNull String requestId, int rssi) {
        Ble.getInstance().postEvent(Events.newRemoteRssiRead(device, requestId, rssi));
        Ble.println(Connection.class, Log.DEBUG, String.format(Locale.US, "RSSI READ [mac: %s, rssi: %d]", device.addr, rssi));
    }

    @Override
    public void onMtuChanged(@NonNull String requestId, int mtu) {
        Ble.getInstance().postEvent(Events.newMtuChanged(device, requestId, mtu));
        Ble.println(Connection.class, Log.DEBUG, String.format(Locale.US, "MTU CHANGE [mac: %s, mtu: %d]", device.addr, mtu));
    }

    @Override
    public void onRequestFialed(@NonNull String requestId, @NonNull Request.RequestType requestType, int failType, byte[] value) {
        Ble.getInstance().postEvent(Events.newRequestFailed(requestId, requestType, failType, value));
        Ble.println(Connection.class, Log.DEBUG, String.format(Locale.US, "REQUEST FAILED [mac: %s, requestId: %s, failType: %d]", device.addr, requestId, failType));
    }

    @Override
    public void onDescriptorRead(@NonNull String requestId, BluetoothGattDescriptor descriptor) {
        Ble.getInstance().postEvent(Events.newDescriptorRead(device, requestId, descriptor));
        Ble.println(Connection.class, Log.DEBUG, String.format(Locale.US, "DESCRIPTOR READ [mac: %s, value: %s]", device.addr, getHex(descriptor.getValue())));
    }

    @Override
    public void onNotificationChanged(@NonNull String requestId, BluetoothGattDescriptor descriptor, boolean isEnabled) {
        Ble.getInstance().postEvent(Events.newNotificationChanged(device, requestId, descriptor, isEnabled));
        Ble.println(Connection.class, Log.DEBUG, String.format(Locale.US, (isEnabled ? "NOTIFICATION ENABLED" : "NOTIFICATION DISABLED") + " [mac: %s]", device.addr));
    }

    @Override
    public void onIndicationChanged(@NonNull String requestId, BluetoothGattDescriptor descriptor, boolean isEnabled) {
        Ble.getInstance().postEvent(Events.newIndicationChanged(device, requestId, descriptor, isEnabled));
        Ble.println(Connection.class, Log.DEBUG, String.format(Locale.US, (isEnabled ? "INDICATION ENABLED" : "INDICATION DISABLED") + " [mac: %s]", device.addr));
    }

    @Override
    public void onCharacteristicWrite(@NonNull String requestId, byte[] value) {
        Ble.getInstance().postEvent(Events.newCharacteristicWrite(device, requestId, value));
        Ble.println(Connection.class, Log.DEBUG, String.format(Locale.US, "WRITE SUCCESS [mac: %s, value: %s]", device.addr, getHex(value)));
    }
}
