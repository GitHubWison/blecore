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
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.util.Log;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import cn.zfs.blelib.callback.ConnectionStateChangeListener;
import cn.zfs.blelib.event.Events;
import cn.zfs.blelib.util.BleUtils;

/**
 * 描述: 蓝牙连接
 * 时间: 2018/4/11 15:29
 * 作者: zengfansheng
 */
public class Connection extends BaseConnection {
    //----------蓝牙连接状态-------------   
    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_RECONNECTING = 2;
    public static final int STATE_CONNECTED = 3;
    public static final int STATE_SERVICE_DISCORVERING = 4;
    public static final int STATE_SERVICE_DISCORVERED = 5;
    public static final int STATE_RELEASED = 6;
    //----------连接超时类型---------
    /**搜索不到设备*/
    public static final int TIMEOUT_TYPE_CANNOT_DISCOVER_DEVICE = 0;
    /**能搜到，连接不上*/
    public static final int TIMEOUT_TYPE_CANNOT_CONNECT = 1;
    /**能连接上，无法发现服务*/
    public static final int TIMEOUT_TYPE_CANNOT_DISCOVER_SERVICES = 2;

    @IntDef({STATE_DISCONNECTED, STATE_CONNECTING, STATE_RECONNECTING, STATE_CONNECTED, STATE_SERVICE_DISCORVERING, STATE_SERVICE_DISCORVERED})
    @Retention(RetentionPolicy.SOURCE)
    public @interface STATE {}
    
    private static final int MSG_ARG_NONE = 0;
    private static final int MSG_ARG_RELEASE = 1;
    private static final int MSG_ARG_RECONNECT = 2;
    private static final int MSG_ARG_NOTIFY = 3;
    
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
    private boolean notifyTimeout;
	    
    private Connection(BluetoothDevice bluetoothDevice) {
        super(bluetoothDevice);
        handler = new ConnHandler(this);
    }

    /**
     * 连接
     * @param device 蓝牙设备
     */
	synchronized static Connection newInstance(BluetoothAdapter bluetoothAdapter, Context context, Device device,
                                               long connectDelay, ConnectionStateChangeListener connectionCallback) {
		if (bluetoothAdapter == null || device == null || device.addr == null || !device.addr.matches("^[0-9A-F]{2}(:[0-9A-F]{2}){5}$")) {
			Ble.println(Connection.class, Log.ERROR, "BluetoothAdapter not initialized or unspecified address.");
			Ble.getInstance().postEvent(Events.newConnectionCreateFailed(device, "BluetoothAdapter not initialized or unspecified address."));
			return null;
		}
		//初始化并建立连接
		Connection conn = new Connection(bluetoothAdapter.getRemoteDevice(device.addr));
		conn.bluetoothAdapter = bluetoothAdapter;
		conn.device = device;
		conn.context = context.getApplicationContext();
		conn.stateChangeListener = connectionCallback;
		//连接蓝牙设备        
        conn.device.connectionState = STATE_CONNECTING;
        conn.connStartTime = System.currentTimeMillis();
        conn.sendConnectionCallback();
        conn.handler.sendEmptyMessageDelayed(MSG_CONNECT, connectDelay);//连接
        conn.handler.sendEmptyMessageDelayed(MSG_TIMER, connectDelay + 500);//启动定时器，用于断线重连
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
	        } else {
                notifyDisconnected();
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
            if (!conn.bluetoothAdapter.isEnabled()) {
                conn.notifyDisconnected();
            } else {
                switch(msg.what) {
                    case MSG_CONNECT://连接
                        conn.notifyTimeout = true;
                        conn.doConnect();
                		break;
                    case MSG_DISCONNECT://处理断开
                        conn.doDisconnect(msg.arg2 == MSG_ARG_RECONNECT, msg.arg1 == MSG_ARG_RELEASE, true);
                		break;
                    case MSG_REFRESH://手动刷新
                        conn.doRefresh(false);
                        break;
                    case MSG_AUTO_REFRESH://自动刷新
                        conn.doRefresh(true);
                        break;
                    case MSG_RELEASE://销毁连接
                        conn.autoReconnEnable = false;//停止重连
                        conn.doDisconnect(false, true, msg.arg1 == MSG_ARG_NOTIFY);
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
    
    private void doOnConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        if (isReleased) {
            gatt.disconnect();
            gatt.close();
            return;
        }
        if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
            Ble.println(Connection.class, Log.DEBUG, "连接状态：STATE_CONNECTED, " +
                    gatt.getDevice().getName() + ", " + gatt.getDevice().getAddress());            
            device.connectionState = STATE_CONNECTED;
            sendConnectionCallback();            
            // 进行服务发现，延时
            handler.sendEmptyMessageDelayed(MSG_DISCOVER_SERVICES, Ble.getInstance().getConfiguration().getDiscoverServicesDelayMillis());
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            Ble.println(Connection.class, Log.DEBUG, "连接状态：STATE_DISCONNECTED, " +
                    gatt.getDevice().getName() + ", " + gatt.getDevice().getAddress() + ", autoReconnEnable: " + autoReconnEnable);
            notifyDisconnected();
            //如果连接上马上又断开，直接把连接开始时间往前推到可以重连的范围
            if (System.currentTimeMillis() - connStartTime < 1000) {
                connStartTime = System.currentTimeMillis() - Ble.getInstance().getConfiguration().getConnectTimeoutMillis();
                notifyTimeout = false;
            }
        } else if (status == 133) {
            doClearTaskAndRefresh(true);
            Ble.println(Connection.class, Log.ERROR, "onConnectionStateChange error, status: " + status + ", " +
                    gatt.getDevice().getName() + ", " + gatt.getDevice().getAddress());
        }
    }
    
    private void doOnServicesDiscovered(BluetoothGatt gatt, int status) {
        if (isReleased) {
            gatt.disconnect();
            gatt.close();
            return;
        }
        List<BluetoothGattService> services = gatt.getServices();
        if (status == BluetoothGatt.GATT_SUCCESS) {
            Ble.println(Connection.class, Log.DEBUG, "onServicesDiscovered. " + gatt.getDevice().getName() + ", " +
                    gatt.getDevice().getAddress() + ", 服务列表长度: " + gatt.getServices().size());
            if (services.isEmpty()) {
                doClearTaskAndRefresh(true);
            } else {
                refreshTimes = 0;
                tryReconnectTimes = 0;
                device.connectionState = STATE_SERVICE_DISCORVERED;
                sendConnectionCallback();
            }
        } else {
            doClearTaskAndRefresh(true);
            Ble.println(Connection.class, Log.ERROR, "onServicesDiscovered error, status: " + status + ", " +
                    gatt.getDevice().getName() + ", " + gatt.getDevice().getAddress());
        }
    }
    
    private void doDiscoverServices() {
        if (bluetoothGatt != null) {
            bluetoothGatt.discoverServices();
            device.connectionState = STATE_SERVICE_DISCORVERING;
            sendConnectionCallback();
        } else {
            notifyDisconnected();
        }
    }
    
    private void doTimer() {
        if (isReleased) {
            return;
        }
        //连接超时。
        if (device.connectionState != STATE_SERVICE_DISCORVERED && System.currentTimeMillis() - connStartTime > 
                Ble.getInstance().getConfiguration().getConnectTimeoutMillis()) {
            if (device.connectionState != STATE_DISCONNECTED) {
                connStartTime = System.currentTimeMillis();
                Ble.println(Connection.class, Log.ERROR, "连接超时, " + device.name + ", " + device.addr);
                int type;
                if (device.connectionState == STATE_RECONNECTING) {
                    type = TIMEOUT_TYPE_CANNOT_DISCOVER_DEVICE;
                } else if (device.connectionState == STATE_CONNECTING) {
                    type = TIMEOUT_TYPE_CANNOT_CONNECT;
                } else {
                    type = TIMEOUT_TYPE_CANNOT_DISCOVER_SERVICES;
                }
                if (notifyTimeout) {
                    Ble.getInstance().postEvent(Events.newConnectTimeout(device, type));
                }
                notifyTimeout = true;
            }
            if (autoReconnEnable) {
                doDisconnect(true, false, true);
            } else {
                doDisconnect(false, false, true);
            }
        }
        handler.sendEmptyMessageDelayed(MSG_TIMER, 500);
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
                if (!isReleased) {
                    bluetoothGatt = bluetoothDevice.connectGatt(context, false, Connection.this);
                }
            }
        }, 500);
    }
    
    private void doDisconnect(boolean reconnect, boolean release, boolean notify) {
	    doClearTaskAndRefresh(false);
        if (bluetoothGatt != null) {
            bluetoothGatt.disconnect();
            bluetoothGatt.close();
        }
        device.connectionState = STATE_DISCONNECTED;
        if (release) {//销毁
            device.connectionState = STATE_RELEASED;
            bluetoothGatt = null;
            Ble.println(Connection.class, Log.DEBUG, "连接销毁释放");
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
	    if (stateChangeListener != null) {
	        stateChangeListener.onConnectionStateChanged(device);
	    }
        Ble.getInstance().postEvent(Events.newConnectionStateChanged(device, device.connectionState));
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
	    handler.sendMessage(Message.obtain(handler, MSG_ON_CONNECTION_STATE_CHANGE, status, newState, gatt));
	}  
    
	@Override
	public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        handler.sendMessage(Message.obtain(handler, MSG_ON_SERVICES_DISCOVERED, status, 0, gatt));
	}

    @Override
    public void onCharacteristicRead(@NonNull String requestId, BluetoothGattCharacteristic characteristic) {
        Ble.getInstance().postEvent(Events.newCharacteristicRead(device, requestId, characteristic));
        Ble.println(Connection.class, Log.DEBUG, "onCharacteristicRead！请求ID：" + requestId +
                ", value: " + BleUtils.bytesToHexString(characteristic.getValue()) + ", mac: " + device.addr);
    }

    @Override
    public void onCharacteristicChanged(BluetoothGattCharacteristic characteristic) {
        Ble.getInstance().postEvent(Events.newCharacteristicChanged(device, characteristic));
    }

    @Override
    public void onReadRemoteRssi(@NonNull String requestId, int rssi) {
        Ble.getInstance().postEvent(Events.newRemoteRssiRead(device, requestId, rssi));
        Ble.println(Connection.class, Log.DEBUG, "读到信号强度！rssi: "+ rssi + ", mac: " + device.addr);
    }

    @Override
    public void onMtuChanged(@NonNull String requestId, int mtu) {
        Ble.getInstance().postEvent(Events.newMtuChanged(device, requestId, mtu));
        Ble.println(Connection.class, Log.DEBUG, "Mtu修改成功！mtu: "+ mtu + ", mac: " + device.addr);
    }

    @Override
    public void onRequestFialed(@NonNull String requestId, @NonNull Request.RequestType requestType, int failType, byte[] value) {
        Ble.getInstance().postEvent(Events.newRequestFailed(requestId, requestType, failType, value));
        Ble.println(Connection.class, Log.ERROR, "请求失败！请求ID：" + requestId +
                ", failType: " + failType + ", mac: " + device.addr);
    }

    @Override
    public void onDescriptorRead(@NonNull String requestId, BluetoothGattDescriptor descriptor) {
        Ble.getInstance().postEvent(Events.newDescriptorRead(device, requestId, descriptor));
        Ble.println(Connection.class, Log.DEBUG, "onDescriptorRead！请求ID：" + requestId +
                ", value: " + BleUtils.bytesToHexString(descriptor.getValue()) + ", mac: " + device.addr);
    }

    @Override
    public void onNotificationChanged(@NonNull String requestId, BluetoothGattDescriptor descriptor, boolean isEnabled) {
        Ble.getInstance().postEvent(Events.newNotificationChanged(device, requestId, descriptor, isEnabled));
        Ble.println(Connection.class, Log.DEBUG, (isEnabled ? "Notification enabled！" : "Notification disabled！") + "请求ID：" + requestId + ", mac: " + device.addr);
    }

    @Override
    public void onIndicationChanged(@NonNull String requestId, BluetoothGattDescriptor descriptor, boolean isEnabled) {
        Ble.getInstance().postEvent(Events.newIndicationChanged(device, requestId, descriptor, isEnabled));
        Ble.println(Connection.class, Log.DEBUG, (isEnabled ? "Indication enabled！" : "Indication disabled！") + "请求ID：" + requestId + ", mac: " + device.addr);
    }

    @Override
    public void onCharacteristicWrite(@NonNull String requestId, byte[] value) {
        Ble.getInstance().postEvent(Events.newCharacteristicWrite(device, requestId, value));       
        Ble.println(Connection.class, Log.DEBUG, "写入成功！value: "+ BleUtils.bytesToHexString(value) +
                ", 请求ID：" + requestId + ", mac: " + device.addr);
    }
}
