package cn.zfs.blelib.core;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.NonNull;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import cn.zfs.blelib.util.BleUtils;

/**
 * 描述: 蓝牙连接基类
 * 时间: 2018/4/11 16:37
 * 作者: zengfansheng
 */
public abstract class BaseConnection extends BluetoothGattCallback {
    public static final UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    public static final int REQUEST_FAIL_TYPE_REQUEST_FAILED = 0;
    public static final int REQUEST_FAIL_TYPE_NULL_CHARACTERISTIC = 1;
    public static final int REQUEST_FAIL_TYPE_NULL_DESCRIPTOR = 2;
    public static final int REQUEST_FAIL_TYPE_NULL_SERVICE = 3;
    /** 请求的回调状态不是{@link BluetoothGatt#GATT_SUCCESS} */
    public static final int REQUEST_FAIL_TYPE_GATT_STATUS_FAILED = 4;
    public static final int REQUEST_FAIL_TYPE_GATT_IS_NULL = 5;
    public static final int REQUEST_FAIL_TYPE_API_LEVEL_TOO_LOW = 6;
    public static final int REQUEST_FAIL_TYPE_BLUETOOTH_ADAPTER_DISABLED = 7;
    public static final int REQUEST_FAIL_TYPE_REQUEST_TIMEOUT = 8;
    public static final int REQUEST_FAIL_TYPE_CONNECTION_DISCONNECTED = 9;
    public static final int REQUEST_FAIL_TYPE_CONNECTION_RELEASED = 10;
    public static final int REQUEST_FAIL_TYPE_VALUE_IS_NULL_OR_EMPTY = 11;
    
    private static final int MSG_ENQUEUE_REQUEST = 1;
    private static final int MSG_NEXT_REQUEST = 2;
    private static final int MSG_TIMER = 3;
    
    protected BluetoothDevice bluetoothDevice;
    protected BluetoothGatt bluetoothGatt;
    protected Queue<Request> requestQueue = new ConcurrentLinkedQueue<>();
    protected Request currentRequest;
    private BluetoothGattCharacteristic pendingCharacteristic;
    protected BluetoothAdapter bluetoothAdapter;
    protected boolean isReleased;
    private BluetoothGattCallback bluetoothGattCallback;
    private HandlerThread requestThread;
    private Handler requestHandler;

    BaseConnection(BluetoothDevice bluetoothDevice) {
        this.bluetoothDevice = bluetoothDevice;
        requestThread = new HandlerThread("Connection: " + bluetoothDevice.getAddress());
        requestThread.start();
        requestHandler = new Handler(requestThread.getLooper(), new HandlerCallback());
        requestHandler.sendEmptyMessageDelayed(MSG_TIMER, 250);
    }

    /**
     * 不经过队列的蓝牙回调，自行区分请求
     */
    public void setBluetoothGattCallback(BluetoothGattCallback callback) {
        bluetoothGattCallback = callback;
    }
    
    public void clearRequestQueue() {
        for (Request request : requestQueue) {
            handleFaildCallback(request.requestId, request.type, REQUEST_FAIL_TYPE_CONNECTION_DISCONNECTED, request.value, false);
        }
        requestQueue.clear();
    }

    public void release() {
        isReleased = true;
        clearRequestQueue();
        requestThread.quit();
    }

    public abstract void onCharacteristicRead(@NonNull String requestId, BluetoothGattCharacteristic characteristic);

    public abstract void onCharacteristicChanged(BluetoothGattCharacteristic characteristic);

    public abstract void onReadRemoteRssi(@NonNull String requestId, int rssi);

    public abstract void onMtuChanged(@NonNull String requestId, int mtu);

    public abstract void onRequestFialed(@NonNull String requestId, @NonNull Request.RequestType requestType, int failType, byte[] value);

    public abstract void onDescriptorRead(@NonNull String requestId, BluetoothGattDescriptor descriptor);

    public abstract void onNotificationChanged(@NonNull String requestId, BluetoothGattDescriptor descriptor, boolean isEnabled);

    public abstract void onIndicationChanged(@NonNull String requestId, BluetoothGattDescriptor descriptor, boolean isEnabled);

    public abstract void onCharacteristicWrite(@NonNull String requestId, byte[] value);

    /*
     * Clears the internal cache and forces a refresh of the services from the
     * remote device.
     */
    public static boolean refresh(BluetoothGatt bluetoothGatt) {
        try {
            Method localMethod = bluetoothGatt.getClass().getMethod("refresh");
            return (Boolean) localMethod.invoke(bluetoothGatt);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void onCharacteristicRead(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status) {
        if (bluetoothGattCallback != null) {
            Ble.getInstance().getExecutorService().execute(new Runnable() {
                @Override
                public void run() {
                    bluetoothGattCallback.onCharacteristicRead(gatt, characteristic, status);
                }
            });
        }
        // 读取到值
        if (currentRequest != null) {
            if (currentRequest.type == Request.RequestType.READ_CHARACTERISTIC) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    onCharacteristicRead(currentRequest.requestId, characteristic);
                } else {
                    handleFaildCallback(currentRequest.requestId, currentRequest.type, REQUEST_FAIL_TYPE_GATT_STATUS_FAILED, currentRequest.value, false);
                }
                executeNextRequest();
            }
        }
    }

    @Override
    public void onCharacteristicWrite(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status) {
        if (bluetoothGattCallback != null) {
            Ble.getInstance().getExecutorService().execute(new Runnable() {
                @Override
                public void run() {
                    bluetoothGattCallback.onCharacteristicWrite(gatt, characteristic, status);
                }
            });
        }
        if (currentRequest != null && currentRequest.waitWriteResult && currentRequest.type == Request.RequestType.WRITE_CHARACTERISTIC) {
            currentRequest.startTime = System.currentTimeMillis();//写数据时有可能大数据请求，更新开始时间，以免被认为超时
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (currentRequest.remainQueue == null || currentRequest.remainQueue.isEmpty()) {
                    onCharacteristicWrite(currentRequest.requestId, currentRequest.value);
                    executeNextRequest();
                } else {
                    try {
                        Thread.sleep(currentRequest.writeDelay);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (writeFail(characteristic, currentRequest.remainQueue.remove())) {
                        performWriteFailed(currentRequest);
                    }
                }
            } else {
                handleFaildCallback(currentRequest.requestId, currentRequest.type, REQUEST_FAIL_TYPE_GATT_STATUS_FAILED, currentRequest.value, true);
            }
        }
    }

    @Override
    public void onCharacteristicChanged(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
        if (bluetoothGattCallback != null) {
            Ble.getInstance().getExecutorService().execute(new Runnable() {
                @Override
                public void run() {
                    bluetoothGattCallback.onCharacteristicChanged(gatt, characteristic);
                }
            });
        }
        // 收到设备notify值 （设备上报值）
        onCharacteristicChanged(characteristic);
    }

    @Override
    public void onReadRemoteRssi(final BluetoothGatt gatt, final int rssi, final int status) {
        if (bluetoothGattCallback != null) {
            Ble.getInstance().getExecutorService().execute(new Runnable() {
                @Override
                public void run() {
                    bluetoothGattCallback.onReadRemoteRssi(gatt, rssi, status);
                }
            });
        }
        if (currentRequest != null) {
            if (currentRequest.type == Request.RequestType.READ_RSSI) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    onReadRemoteRssi(currentRequest.requestId, rssi);
                } else {
                    handleFaildCallback(currentRequest.requestId, currentRequest.type, REQUEST_FAIL_TYPE_GATT_STATUS_FAILED, currentRequest.value, false);
                }
                executeNextRequest();
            }
        }
    }

    @Override
    public void onDescriptorRead(final BluetoothGatt gatt, final BluetoothGattDescriptor descriptor, final int status) {
        if (bluetoothGattCallback != null) {
            Ble.getInstance().getExecutorService().execute(new Runnable() {
                @Override
                public void run() {
                    bluetoothGattCallback.onDescriptorRead(gatt, descriptor, status);
                }
            });
        }
        if (currentRequest != null) {
            BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();
            if (currentRequest.type == Request.RequestType.TOGGLE_NOTIFICATION) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    handleFaildCallback(currentRequest.requestId, currentRequest.type, REQUEST_FAIL_TYPE_GATT_STATUS_FAILED, currentRequest.value, false);
                }
                if (characteristic.getService().getUuid().equals(pendingCharacteristic.getService().getUuid()) && characteristic.getUuid().equals(pendingCharacteristic.getUuid())) {
                    if (!enableNotificationOrIndication(Arrays.equals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, currentRequest.value), true, characteristic)) {
                        handleFaildCallback(currentRequest.requestId, currentRequest.type, REQUEST_FAIL_TYPE_GATT_STATUS_FAILED, currentRequest.value, false);
                    }
                }
            } else if (currentRequest.type == Request.RequestType.TOGGLE_INDICATION) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    handleFaildCallback(currentRequest.requestId, currentRequest.type, REQUEST_FAIL_TYPE_GATT_STATUS_FAILED, currentRequest.value, false);
                }
                if (characteristic.getService().getUuid().equals(pendingCharacteristic.getService().getUuid()) && characteristic.getUuid().equals(pendingCharacteristic.getUuid())) {
                    if (!enableNotificationOrIndication(Arrays.equals(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE, currentRequest.value), false, characteristic)) {
                        handleFaildCallback(currentRequest.requestId, currentRequest.type, REQUEST_FAIL_TYPE_GATT_STATUS_FAILED, currentRequest.value, false);
                    }
                }
            } else if (currentRequest.type == Request.RequestType.READ_DESCRIPTOR) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    onDescriptorRead(currentRequest.requestId, descriptor);
                } else {
                    handleFaildCallback(currentRequest.requestId, currentRequest.type, REQUEST_FAIL_TYPE_GATT_STATUS_FAILED, currentRequest.value, false);
                }
                executeNextRequest();
            }
        }
    }

    @Override
    public void onDescriptorWrite(final BluetoothGatt gatt, final BluetoothGattDescriptor descriptor, final int status) {
        if (bluetoothGattCallback != null) {
            Ble.getInstance().getExecutorService().execute(new Runnable() {
                @Override
                public void run() {
                    bluetoothGattCallback.onDescriptorWrite(gatt, descriptor, status);
                }
            });
        }
        if (currentRequest != null) {
            if (currentRequest.type == Request.RequestType.TOGGLE_NOTIFICATION) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    handleFaildCallback(currentRequest.requestId, currentRequest.type, REQUEST_FAIL_TYPE_GATT_STATUS_FAILED, currentRequest.value, false);
                } else {
                    if (Arrays.equals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, currentRequest.value)) {
                        onNotificationChanged(currentRequest.requestId, descriptor, true);
                    } else {
                        onNotificationChanged(currentRequest.requestId, descriptor, false);
                    }
                }
                executeNextRequest();
            } else if (currentRequest.type == Request.RequestType.TOGGLE_INDICATION) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    handleFaildCallback(currentRequest.requestId, currentRequest.type, REQUEST_FAIL_TYPE_GATT_STATUS_FAILED, currentRequest.value, false);
                } else {
                    if (Arrays.equals(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE, currentRequest.value)) {
                        onIndicationChanged(currentRequest.requestId, descriptor, true);
                    } else {
                        onIndicationChanged(currentRequest.requestId, descriptor, false);
                    }
                }
                executeNextRequest();
            }
        }
    }

    @Override
    public void onMtuChanged(final BluetoothGatt gatt, final int mtu, final int status) {
        if (bluetoothGattCallback != null) {
            Ble.getInstance().getExecutorService().execute(new Runnable() {
                @SuppressLint("NewApi")
                @Override
                public void run() {
                    bluetoothGattCallback.onMtuChanged(gatt, mtu, status);
                }
            });
        }
        if (currentRequest != null) {
            if (currentRequest.type == Request.RequestType.CHANGE_MTU) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    onMtuChanged(currentRequest.requestId, mtu);
                } else {
                    handleFaildCallback(currentRequest.requestId, currentRequest.type, REQUEST_FAIL_TYPE_GATT_STATUS_FAILED, currentRequest.value, false);
                }
                executeNextRequest();
            }
        }
    }

    private void handleFaildCallback(String requestId, Request.RequestType requestType, int failType, byte[] value, boolean executeNext) {
        onRequestFialed(requestId, requestType, failType, value);
        if (executeNext) {
            executeNextRequest();
        }
    }

    public void changeMtu(@NonNull String requestId, int mtu) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            enqueue(Request.newChangeMtuRequest(requestId, mtu));
        } else {
            handleFaildCallback(requestId, Request.RequestType.CHANGE_MTU, REQUEST_FAIL_TYPE_API_LEVEL_TOO_LOW, BleUtils.numberToBytes(false, mtu, 4), false);
        }
    }

    /*
     * 请求读取characteristic的值
     * @param requestId 请求码
     */
    public void readCharacteristic(@NonNull String requestId, UUID service, UUID characteristic) {
        enqueue(Request.newReadCharacteristicRequest(requestId, service, characteristic));
    }

    /**
     * 打开Notifications
     * @param requestId 请求码
     * @param enable 开启还是关闭
     */
    public void toggleNotification(@NonNull String requestId, UUID service, UUID characteristic, boolean enable) {
        enqueue(Request.newToggleNotificationRequest(requestId, service, characteristic, enable));
    }

    /**
     * @param enable 开启还是关闭
     */
    public void toggleIndication(@NonNull String requestId, UUID service, UUID characteristic, boolean enable) {
        enqueue(Request.newToggleIndicationRequest(requestId, service, characteristic, enable));
    }

    public void readDescriptor(@NonNull String requestId, UUID service, UUID characteristic, UUID descriptor) {
        enqueue(Request.newReadDescriptorRequest(requestId, service, characteristic, descriptor));
    }

    public void writeCharacteristic(@NonNull String requestId, UUID service, UUID characteristic, byte[] value) {
        if (value == null || value.length == 0) {
            handleFaildCallback(requestId, Request.RequestType.WRITE_CHARACTERISTIC, REQUEST_FAIL_TYPE_VALUE_IS_NULL_OR_EMPTY, value, false);
            return;
        }
        enqueue(Request.newWriteCharacteristicRequest(requestId, service, characteristic, value));
    }

    public void readRssi(@NonNull String requestId) {
        enqueue(Request.newReadRssiRequest(requestId));
    }
    
    private void enqueue(Request request) {        
        if (isReleased) {
            handleFaildCallback(request.requestId, request.type, REQUEST_FAIL_TYPE_CONNECTION_RELEASED, request.value, false);
        } else {
            Message.obtain(requestHandler, MSG_ENQUEUE_REQUEST, request).sendToTarget();            
        }
    }

    private void executeNextRequest() {
        requestHandler.sendEmptyMessage(MSG_NEXT_REQUEST);
    }
    
    private class HandlerCallback implements Handler.Callback {
        @Override
        public boolean handleMessage(Message msg) {
            switch(msg.what) {
                case MSG_ENQUEUE_REQUEST:
                    if (currentRequest == null) {
                        executeRequest((Request) msg.obj);
                    } else {
                        requestQueue.add(currentRequest);
                    }
            		break;
                case MSG_NEXT_REQUEST:
                    if (requestQueue.isEmpty()) {
                        currentRequest = null;
                    } else {
                        executeRequest(requestQueue.remove());
                    }                    
            		break;
                case MSG_TIMER:
                    if (currentRequest != null && System.currentTimeMillis() - currentRequest.startTime > 1000) {//请求超时
                        handleFaildCallback(currentRequest.requestId, currentRequest.type, REQUEST_FAIL_TYPE_REQUEST_TIMEOUT, currentRequest.value, true);
                    }
                    requestHandler.sendEmptyMessageDelayed(MSG_TIMER, 250);
                    break;
            }
            return true;
        }
    }
    
    private void executeRequest(Request request) {
        currentRequest = request;
        currentRequest.startTime = System.currentTimeMillis();
        if (bluetoothAdapter.isEnabled()) {
            if (bluetoothGatt != null) {
                switch(request.type) {                    
                    case READ_RSSI:
                        executeReadRssi(request);
                        break;
                    case CHANGE_MTU:
                        executeChangeMtu(request);
                        break;
                    default:
                        BluetoothGattService gattService = bluetoothGatt.getService(request.service);
                        if (gattService != null) {
                            BluetoothGattCharacteristic gattCharacteristic = gattService.getCharacteristic(request.characteristic);
                            if (gattCharacteristic != null) {
                                switch(request.type) {
                                    case TOGGLE_NOTIFICATION:
                                    case TOGGLE_INDICATION:
                                        executeIndicationOrNotification(gattCharacteristic, request.requestId, request.type, request.value);
                                        break;
                                    case READ_CHARACTERISTIC:
                                        executeReadCharacteristic(gattCharacteristic, request.requestId);
                                        break;
                                    case READ_DESCRIPTOR:
                                        executeReadDescriptor(gattCharacteristic, request.descriptor, request.requestId);
                                        break;
                                    case WRITE_CHARACTERISTIC:                                        
                                        executeWriteCharacteristic(gattCharacteristic, request);
                                        break;
                                }
                            } else {
                                handleFaildCallback(request.requestId, request.type, REQUEST_FAIL_TYPE_NULL_CHARACTERISTIC, request.value, true);
                            }
                        } else {
                            handleFaildCallback(request.requestId, request.type, REQUEST_FAIL_TYPE_NULL_SERVICE, request.value, true);
                        }                        
                        break;
                }
            } else {
                handleFaildCallback(request.requestId, request.type, REQUEST_FAIL_TYPE_GATT_IS_NULL, request.value, true);
            }
        } else {
            handleFaildCallback(request.requestId, request.type, REQUEST_FAIL_TYPE_BLUETOOTH_ADAPTER_DISABLED, request.value, true);
        }
    }
    
    private void executeChangeMtu(Request request) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (!bluetoothGatt.requestMtu((int) BleUtils.bytesToLong(false, request.value))) {
                handleFaildCallback(request.requestId, request.type, REQUEST_FAIL_TYPE_REQUEST_FAILED, request.value, true);
            }
        } else {
            handleFaildCallback(request.requestId, request.type, REQUEST_FAIL_TYPE_API_LEVEL_TOO_LOW, request.value, true);
        }
    }
    
    private void executeReadRssi(Request request) {
        if (!bluetoothGatt.readRemoteRssi()) {
            handleFaildCallback(request.requestId, request.type, REQUEST_FAIL_TYPE_REQUEST_FAILED, request.value, true);
        }
    }
    
    private void executeReadCharacteristic(BluetoothGattCharacteristic gattCharacteristic, String requestId) {
        if (!bluetoothGatt.readCharacteristic(gattCharacteristic)) {
            handleFaildCallback(requestId, Request.RequestType.READ_CHARACTERISTIC, REQUEST_FAIL_TYPE_REQUEST_FAILED, null, true);
        }
    }

    private void executeWriteCharacteristic(BluetoothGattCharacteristic gattCharacteristic, Request request) {
        try {
            request.waitWriteResult = Ble.getInstance().getConfiguration().isWaitWriteResult();
            request.writeDelay = Ble.getInstance().getConfiguration().getPackageWriteDelayMillis();
            int packSize = Ble.getInstance().getConfiguration().getPackageSize();
            int requestWriteDelayMillis = Ble.getInstance().getConfiguration().getRequestWriteDelayMillis();
            Thread.sleep(requestWriteDelayMillis > 0 ? requestWriteDelayMillis : request.writeDelay);
            if (request.value.length > packSize) {
                List<byte[]> list = BleUtils.splitPackage(request.value, packSize);  
                if (!request.waitWriteResult) {//不等待则遍历发送
                    for (int i = 0; i < list.size(); i++) {
                        byte[] bytes = list.get(i);
                        if (i > 0) {
                            Thread.sleep(request.writeDelay);
                        }
                        if (writeFail(gattCharacteristic, bytes)) {//写失败
                            performWriteFailed(request);
                            return;
                        }
                    }
                } else {//等待则只直接发送第一包，剩下的添加到队列等待回调
                    request.remainQueue = new ConcurrentLinkedQueue<>();
                    request.remainQueue.addAll(list);
                    if (writeFail(gattCharacteristic, request.remainQueue.remove())) {//写失败
                        performWriteFailed(request);
                        return;
                    }
                }
            } else if (writeFail(gattCharacteristic, request.value)) {
                performWriteFailed(request);
                return;
            }                       
            if (!request.waitWriteResult) {
                onCharacteristicWrite(request.requestId, request.value);
                executeNextRequest();
            }
        } catch (InterruptedException e) {
            performWriteFailed(request);
        }
    }

    private void performWriteFailed(Request request) {
        request.remainQueue = null;
        handleFaildCallback(request.requestId, Request.RequestType.WRITE_CHARACTERISTIC, REQUEST_FAIL_TYPE_REQUEST_FAILED, request.value, true);        
    }

    private boolean writeFail(BluetoothGattCharacteristic gattCharacteristic, byte[] value) {
        gattCharacteristic.setValue(value);
        int writeType = Ble.getInstance().getConfiguration().getWriteType();
        if (writeType == BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT || writeType == BluetoothGattCharacteristic.WRITE_TYPE_SIGNED ||
                writeType == BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE) {
            gattCharacteristic.setWriteType(writeType);
        }
        return !bluetoothGatt.writeCharacteristic(gattCharacteristic);
    }
    
    private void executeReadDescriptor(BluetoothGattCharacteristic characteristic, UUID descriptor, String requestId) {
        BluetoothGattDescriptor gattDescriptor = characteristic.getDescriptor(descriptor);
        if (gattDescriptor != null) {
            if (!bluetoothGatt.readDescriptor(gattDescriptor)) {
                handleFaildCallback(requestId, Request.RequestType.READ_DESCRIPTOR, REQUEST_FAIL_TYPE_REQUEST_FAILED, null, true);
            }
        } else {
            handleFaildCallback(requestId, Request.RequestType.READ_DESCRIPTOR, REQUEST_FAIL_TYPE_NULL_DESCRIPTOR, null, true);
        }
    }
    
    private void executeIndicationOrNotification(BluetoothGattCharacteristic characteristic, String requestId, Request.RequestType requestType, byte[] value) {
        pendingCharacteristic = characteristic;
        BluetoothGattDescriptor gattDescriptor = pendingCharacteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
        if (gattDescriptor == null || !bluetoothGatt.readDescriptor(gattDescriptor)) {
            handleFaildCallback(requestId, requestType, REQUEST_FAIL_TYPE_REQUEST_FAILED, value, true);
        }
    }

    private boolean enableNotificationOrIndication(boolean enable, boolean notification, BluetoothGattCharacteristic characteristic) {
        //setCharacteristicNotification是设置本机
        if (!bluetoothAdapter.isEnabled() || bluetoothGatt == null || !bluetoothGatt.setCharacteristicNotification(characteristic, enable)) {
            return false;
        }
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
        if (descriptor == null) {
            return false;
        }
        if (enable) {
            descriptor.setValue(notification ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
        } else {
            descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        }
        //部分蓝牙在Android6.0及以下需要设置写入类型为有响应的，否则会enable回调是成功，但是仍然无法收到notification数据
        int writeType = characteristic.getWriteType();
        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        boolean result =  bluetoothGatt.writeDescriptor(descriptor);//把设置写入蓝牙设备
        characteristic.setWriteType(writeType);
        return result;
    }
}