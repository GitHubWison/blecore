package cn.zfs.blelib.core;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.os.Build;
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

    public static final int FAIL_TYPE_REQUEST_FAILED = 0;
    public static final int FAIL_TYPE_NULL_CHARACTERISTIC = 1;
    public static final int FAIL_TYPE_NULL_DESCRIPTOR = 2;
    public static final int FAIL_TYPE_NULL_SERVICE = 3;
    /** 请求的回调状态不是{@link BluetoothGatt#GATT_SUCCESS} */
    public static final int FAIL_TYPE_GATT_STATUS_FAILED = 4;
    public static final int FAIL_TYPE_GATT_IS_NULL = 5;
    public static final int FAIL_TYPE_API_LEVEL_TOO_LOW = 6;
    public static final int FAIL_TYPE_BLUETOOTH_ADAPTER_DISABLED = 7;
    public static final int FAIL_TYPE_REQUEST_TIMEOUT = 8;
    
    protected BluetoothDevice bluetoothDevice;
    protected BluetoothGatt bluetoothGatt;
    protected Queue<Request> requestQueue = new ConcurrentLinkedQueue<>();
    protected Request currentRequest;
    private Request lastRequest;
    private BluetoothGattCharacteristic pendingCharacteristic;
    protected BluetoothAdapter bluetoothAdapter;
    protected boolean isReleased;
    private volatile boolean executorRunning;
    private RequestRunnable requestRunnalbe;

    BaseConnection(BluetoothDevice bluetoothDevice) {
        this.bluetoothDevice = bluetoothDevice;
        requestRunnalbe = new RequestRunnable();
    }

    public void clearRequestQueue() {
        requestQueue.clear();
        currentRequest = null;
    }

    public void release() {
        isReleased = true;
        clearRequestQueue();
    }

    private void checkIfRelease() {
        if (isReleased) {
            throw new RuntimeException("连接已被释放，需要重新实例化");
        }
    }

    public abstract void onCharacteristicRead(@NonNull String requestId, BluetoothGattCharacteristic characteristic);

    public abstract void onCharacteristicChanged(BluetoothGattCharacteristic characteristic);

    public abstract void onReadRemoteRssi(@NonNull String requestId, int rssi);

    public abstract void onMtuChanged(@NonNull String requestId, int mtu);

    public abstract void onRequestFialed(@NonNull String requestId, @NonNull Request.RequestType requestType, int failType, byte[] value);

    public abstract void onDescriptorRead(@NonNull String requestId, BluetoothGattDescriptor descriptor);

    public abstract void onNotificationRegistered(@NonNull String requestId, BluetoothGattDescriptor descriptor);

    public abstract void onNotificationUnregistered(@NonNull String requestId, BluetoothGattDescriptor descriptor);

    public abstract void onIndicationRegistered(@NonNull String requestId, BluetoothGattDescriptor descriptor);

    public abstract void onIndicationUnregistered(@NonNull String requestId, BluetoothGattDescriptor descriptor);

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
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        // 读取到值
        Request request = currentRequest;
        if (request != null) {
            if (request.type == Request.RequestType.READ_CHARACTERISTIC) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    onCharacteristicRead(request.requestId, characteristic);
                } else {
                    handleFaildCallback(request.requestId, request.type, FAIL_TYPE_GATT_STATUS_FAILED, request.value, false);
                }
                processNextRequest();
            }
        }
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        Request request = currentRequest;
        if (request != null && request.waitWriteResult && request.type == Request.RequestType.WRITE_CHARACTERISTIC && request.writeOverValue != null) {
            request.startTime = System.currentTimeMillis();//写数据时有可能大数据请求，更新开始时间，以免被认为超时
            if (status == BluetoothGatt.GATT_SUCCESS) {
                byte[] value = characteristic.getValue();
                byte[] wrote = Arrays.copyOf(request.writeOverValue, request.writeOverValue.length + value.length);
                System.arraycopy(value, 0, wrote, request.writeOverValue.length, value.length);
                request.writeOverValue = wrote;
                if (Arrays.equals(wrote, request.value)) {
                    onCharacteristicWrite(request.requestId, request.value);
                    request.writeOverValue = null;
                    processNextRequest();
                } else if (request.remainQueue != null && !request.remainQueue.isEmpty()) {
                    try {
                        Thread.sleep(request.writeDelay);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    doWrite(characteristic, request.remainQueue.remove());
                }
            } else {
                request.writeOverValue = null;//不让再次进入
                handleFaildCallback(request.requestId, request.type, FAIL_TYPE_GATT_STATUS_FAILED, request.value, true);
            }
        }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        // 收到设备notify值 （设备上报值）
        onCharacteristicChanged(characteristic);
    }

    @Override
    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
        Request request = currentRequest;
        if (request != null) {
            if (request.type == Request.RequestType.READ_RSSI) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    onReadRemoteRssi(request.requestId, rssi);
                } else {
                    handleFaildCallback(request.requestId, request.type, FAIL_TYPE_GATT_STATUS_FAILED, request.value, false);
                }
                processNextRequest();
            }
        }
    }

    @Override
    public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        Request request = currentRequest;
        if (request != null) {
            BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();
            if (request.type == Request.RequestType.CHARACTERISTIC_NOTIFICATION) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    handleFaildCallback(request.requestId, request.type, FAIL_TYPE_GATT_STATUS_FAILED, request.value, false);
                }
                if (characteristic.getService().getUuid().equals(pendingCharacteristic.getService().getUuid()) && characteristic.getUuid().equals(pendingCharacteristic.getUuid())) {
                    if (!enableNotificationOrIndication(Arrays.equals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, request.value), true, characteristic)) {
                        handleFaildCallback(request.requestId, request.type, FAIL_TYPE_GATT_STATUS_FAILED, request.value, false);
                    }
                }
            } else if (request.type == Request.RequestType.CHARACTERISTIC_INDICATION) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    handleFaildCallback(request.requestId, request.type, FAIL_TYPE_GATT_STATUS_FAILED, request.value, false);
                }
                if (characteristic.getService().getUuid().equals(pendingCharacteristic.getService().getUuid()) && characteristic.getUuid().equals(pendingCharacteristic.getUuid())) {
                    if (!enableNotificationOrIndication(Arrays.equals(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE, request.value), false, characteristic)) {
                        handleFaildCallback(request.requestId, request.type, FAIL_TYPE_GATT_STATUS_FAILED, request.value, false);
                    }
                }
            } else if (request.type == Request.RequestType.READ_DESCRIPTOR) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    onDescriptorRead(request.requestId, descriptor);
                } else {
                    handleFaildCallback(request.requestId, request.type, FAIL_TYPE_GATT_STATUS_FAILED, request.value, false);
                }
                processNextRequest();
            }
        }
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        Request request = currentRequest;
        if (request != null) {
            if (request.type == Request.RequestType.CHARACTERISTIC_NOTIFICATION) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    handleFaildCallback(request.requestId, request.type, FAIL_TYPE_GATT_STATUS_FAILED, request.value, false);
                } else {
                    if (Arrays.equals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, request.value)) {
                        onNotificationRegistered(request.requestId, descriptor);
                    } else {
                        onNotificationUnregistered(request.requestId, descriptor);
                    }
                }
                processNextRequest();
            } else if (request.type == Request.RequestType.CHARACTERISTIC_INDICATION) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    handleFaildCallback(request.requestId, request.type, FAIL_TYPE_GATT_STATUS_FAILED, request.value, false);
                } else {
                    if (Arrays.equals(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE, request.value)) {
                        onIndicationRegistered(request.requestId, descriptor);
                    } else {
                        onIndicationUnregistered(request.requestId, descriptor);
                    }
                }
                processNextRequest();
            }
        }
    }

    @Override
    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
        Request request = currentRequest;
        if (request != null) {
            if (request.type == Request.RequestType.CHANGE_MTU) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    onMtuChanged(request.requestId, mtu);
                } else {
                    handleFaildCallback(request.requestId, request.type, FAIL_TYPE_GATT_STATUS_FAILED, request.value, false);
                }
                processNextRequest();
            }
        }
    }

    private void handleFaildCallback(String requestId, Request.RequestType requestType, int failType, byte[] value, boolean processNext) {
        onRequestFialed(requestId, requestType, failType, value);
        if (processNext) {
            processNextRequest();
        }
    }

    public void changeMtu(@NonNull String requestId, int mtu) {
        checkIfRelease();
        if (mtu < 23) {//小于23时，不会回调onMtuChanged，所以直接返回
            onMtuChanged(requestId, 23);
        } else {
            enqueue(new Request(Request.RequestType.CHANGE_MTU, requestId, null, null, null, BleUtils.numberToBytes(false, mtu, 4)));
        }        
    }

    /*
     * 请求读取characteristic的值
     * @param requestId 请求码
     */
    public void readCharacteristic(@NonNull String requestId, UUID service, UUID characteristic) {
        checkIfRelease();
        enqueue(new Request(Request.RequestType.READ_CHARACTERISTIC, requestId, service, characteristic, null));
    }

    /**
     * 打开Notifications
     * @param requestId 请求码
     * @param enable 开启还是关闭
     */
    public void requestCharacteristicNotification(@NonNull String requestId, UUID service, UUID characteristic, boolean enable) {
        checkIfRelease();
        enqueue(new Request(Request.RequestType.CHARACTERISTIC_NOTIFICATION, requestId, service,
                characteristic, null, enable ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE));
    }

    /**
     * @param enable 开启还是关闭
     */
    public void requestCharacteristicIndication(@NonNull String requestId, UUID service, UUID characteristic, boolean enable) {
        checkIfRelease();
        enqueue(new Request(Request.RequestType.CHARACTERISTIC_INDICATION, requestId, service,
                characteristic, null, enable ? BluetoothGattDescriptor.ENABLE_INDICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE));
    }

    public void readDescriptor(@NonNull final String requestId, final UUID service, final UUID characteristic, final UUID descriptor) {
        checkIfRelease();
        enqueue(new Request(Request.RequestType.READ_DESCRIPTOR, requestId, service, characteristic, descriptor));
    }

    public void writeCharacteristic(@NonNull String requestId, UUID service, UUID characteristic, byte[] value) {
        checkIfRelease();
        if (value == null || value.length == 0) {
            return;
        }
        enqueue(new Request(Request.RequestType.WRITE_CHARACTERISTIC, requestId, service, characteristic, null, value));
    }

    public void readRssi(@NonNull String requestId) {
        checkIfRelease();
        enqueue(new Request(Request.RequestType.READ_RSSI, requestId, null, null, null));
    }
    
    private void enqueue(Request request) {
        synchronized (this) {
            requestQueue.add(request);
            if (!executorRunning) {
                executorRunning = true;
                processNextRequest();
                Ble.getInstance().getExecutorService().execute(requestRunnalbe);
            }
        }
    }
    
    private class RequestRunnable implements Runnable {
        @Override
        public void run() {
            try {
                while (true) {
                    Request request = currentRequest;
                    if (request == null) {
                        synchronized (this) {
                            request = currentRequest;
                            if (request == null) {
                                executorRunning = false;
                                return;
                            }
                        }
                    }
                    if (lastRequest != request) {
                        lastRequest = request;
                        request.startTime = System.currentTimeMillis();
                        executeRequest(request);
                    } else if (System.currentTimeMillis() - request.startTime > 1000) {//请求超时
                        handleFaildCallback(request.requestId, request.type, FAIL_TYPE_REQUEST_TIMEOUT, request.value, true);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                executorRunning = false;
            }
        }
    }

    private void executeRequest(Request request) {
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
                                    case CHARACTERISTIC_NOTIFICATION:
                                    case CHARACTERISTIC_INDICATION:
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
                                handleFaildCallback(request.requestId, request.type, FAIL_TYPE_NULL_CHARACTERISTIC, request.value, true);
                            }
                        } else {
                            handleFaildCallback(request.requestId, request.type, FAIL_TYPE_NULL_SERVICE, request.value, true);
                        }                        
                        break;
                }
            } else {
                handleFaildCallback(request.requestId, request.type, FAIL_TYPE_GATT_IS_NULL, request.value, true);
            }
        } else {
            handleFaildCallback(request.requestId, request.type, FAIL_TYPE_BLUETOOTH_ADAPTER_DISABLED, request.value, true);
        }
    }
    
    private synchronized void processNextRequest() {
        if (requestQueue.isEmpty()) {
            currentRequest = null;
            return;
        }
        currentRequest = requestQueue.remove();
    }
    
    private void executeChangeMtu(Request request) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (!bluetoothGatt.requestMtu((int) BleUtils.bytesToLong(false, request.value))) {
                handleFaildCallback(request.requestId, request.type, FAIL_TYPE_REQUEST_FAILED, request.value, true);
            }
        } else {
            handleFaildCallback(request.requestId, request.type, FAIL_TYPE_API_LEVEL_TOO_LOW, request.value, true);
        }
    }
    
    private void executeReadRssi(Request request) {
        if (!bluetoothGatt.readRemoteRssi()) {
            handleFaildCallback(request.requestId, request.type, FAIL_TYPE_REQUEST_FAILED, request.value, true);
        }
    }
    
    private void executeReadCharacteristic(BluetoothGattCharacteristic gattCharacteristic, String requestId) {
        if (!bluetoothGatt.readCharacteristic(gattCharacteristic)) {
            handleFaildCallback(requestId, Request.RequestType.READ_CHARACTERISTIC, FAIL_TYPE_REQUEST_FAILED, null, true);
        }
    }

    private void executeWriteCharacteristic(BluetoothGattCharacteristic gattCharacteristic, Request request) {
        try {
            request.waitWriteResult = Ble.getInstance().getConfiguration().isWaitWriteResult();
            request.writeDelay = Ble.getInstance().getConfiguration().getWriteDelayMillis();
            request.writeOverValue = new byte[0];
            int packSize = Ble.getInstance().getConfiguration().getPackageSize();                        
            if (request.value.length > packSize) {
                List<byte[]> list = BleUtils.splitPackage(request.value, packSize);  
                if (!request.waitWriteResult) {//不等待则遍历发送
                    for (byte[] bytes : list) {
                        Thread.sleep(request.writeDelay);
                        if (!doWrite(gattCharacteristic, bytes)) {//写失败
                            performWriteFailed(request);
                            return;
                        }
                    }
                } else {//等待则只直接发送第一包，剩下的添加到队列等待回调
                    request.remainQueue = new ConcurrentLinkedQueue<>();
                    request.remainQueue.addAll(list);
                    Thread.sleep(request.writeDelay);
                    if (!doWrite(gattCharacteristic, request.remainQueue.remove())) {//写失败
                        performWriteFailed(request);
                        return;
                    }
                }
            } else {
                Thread.sleep(request.writeDelay);
                if (!doWrite(gattCharacteristic, request.value)) {
                    performWriteFailed(request);
                    return;
                }
            }                       
            if (!request.waitWriteResult) {
                onCharacteristicWrite(request.requestId, request.value);
                processNextRequest();
            }
        } catch (InterruptedException e) {
            performWriteFailed(request);
        }
    }

    private void performWriteFailed(Request request) {
        request.writeOverValue = null;
        request.remainQueue = null;
        handleFaildCallback(request.requestId, Request.RequestType.WRITE_CHARACTERISTIC, FAIL_TYPE_REQUEST_FAILED, request.value, true);        
    }

    private boolean doWrite(BluetoothGattCharacteristic gattCharacteristic, byte[] value) {
        gattCharacteristic.setValue(value);
        int writeType = Ble.getInstance().getConfiguration().getWriteType();
        if (writeType == BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT || writeType == BluetoothGattCharacteristic.WRITE_TYPE_SIGNED ||
                writeType == BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE) {
            gattCharacteristic.setWriteType(writeType);
        }
        return bluetoothGatt.writeCharacteristic(gattCharacteristic);
    }
    
    private void executeReadDescriptor(BluetoothGattCharacteristic characteristic, UUID descriptor, String requestId) {
        BluetoothGattDescriptor gattDescriptor = characteristic.getDescriptor(descriptor);
        if (gattDescriptor != null) {
            if (!bluetoothGatt.readDescriptor(gattDescriptor)) {
                handleFaildCallback(requestId, Request.RequestType.READ_DESCRIPTOR, FAIL_TYPE_REQUEST_FAILED, null, true);
            }
        } else {
            handleFaildCallback(requestId, Request.RequestType.READ_DESCRIPTOR, FAIL_TYPE_NULL_DESCRIPTOR, null, true);
        }
    }
    
    private void executeIndicationOrNotification(BluetoothGattCharacteristic characteristic, String requestId, Request.RequestType requestType, byte[] value) {
        pendingCharacteristic = characteristic;
        BluetoothGattDescriptor gattDescriptor = pendingCharacteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
        if (gattDescriptor == null || !bluetoothGatt.readDescriptor(gattDescriptor)) {
            handleFaildCallback(requestId, requestType, FAIL_TYPE_REQUEST_FAILED, value, true);
        }
    }

    private boolean enableNotificationOrIndication(boolean enable, boolean notification, BluetoothGattCharacteristic characteristic) {
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
        return bluetoothGatt.writeDescriptor(descriptor);
    }
}