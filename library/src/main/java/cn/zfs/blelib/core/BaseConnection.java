package cn.zfs.blelib.core;

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
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import cn.zfs.blelib.callback.IRequestCallback;
import cn.zfs.blelib.util.BleUtils;

/**
 * 描述: 蓝牙连接基类
 * 时间: 2018/4/11 16:37
 * 作者: zengfansheng
 */
public abstract class BaseConnection extends BluetoothGattCallback {
    public static final UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    //----------蓝牙连接状态-------------   
    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_RECONNECTING = 2;
    public static final int STATE_CONNECTED = 3;
    public static final int STATE_SERVICE_DISCORVERING = 4;
    public static final int STATE_SERVICE_DISCORVERED = 5;
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

    private static final short  GATT_REQ_NOT_SUPPORTED = 6;
    protected BluetoothDevice bluetoothDevice;
    protected BluetoothGatt bluetoothGatt;
    protected Queue<Request> requestQueue = new ConcurrentLinkedQueue<>();
    Request currentRequest;
    private BluetoothGattCharacteristic pendingCharacteristic;
    private RequestCallbackContainer requestCallbackContainer = new RequestCallbackContainer();
    protected BluetoothAdapter bluetoothAdapter;
    private HandlerThread handlerThread;
    private Handler requestHandler;
    private IRequestCallback requestCallback;

    BaseConnection(BluetoothDevice bluetoothDevice) {
        this.bluetoothDevice = bluetoothDevice;
        handlerThread = new HandlerThread("ConnectionThread_" + bluetoothDevice.getAddress());
        handlerThread.start();
        requestHandler = new Handler(handlerThread.getLooper());
    }

    public void release() {
        handlerThread.quit();//移除所有消息，停止线程
    }

    /**
     * 设置请求回调
     */
    public void setRequestCallback(IRequestCallback callback) {
        requestCallback = callback;
    }
    
    protected abstract int getWriteDelayMillis();
    
    protected abstract int getPackageSize();
    
    /*
     * Clears the internal cache and forces a refresh of the services from the
     * remote device.
     */
    public boolean refresh(BluetoothGatt bluetoothGatt) {
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
        if (currentRequest != null && currentRequest.type == Request.RequestType.READ_CHARACTERISTIC) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                handleCharacteristic(currentRequest.callback, currentRequest.requestId, Request.RequestType.READ_CHARACTERISTIC, gatt, characteristic);
                handleCharacteristic(requestCallback, currentRequest.requestId, Request.RequestType.READ_CHARACTERISTIC, gatt, characteristic);
            } else {
                handleFaildCallback(currentRequest.callback, currentRequest.requestId, currentRequest.type, IRequestCallback.NONE, currentRequest.value);
                handleFaildCallback(requestCallback, currentRequest.requestId, currentRequest.type, IRequestCallback.NONE, currentRequest.value);
            }
            processNextRequest();
        }
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        if (currentRequest != null && currentRequest.type == Request.RequestType.WRITE_CHARACTERISTIC) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                handleCharacteristic(currentRequest.callback, currentRequest.requestId, Request.RequestType.WRITE_CHARACTERISTIC, gatt, characteristic);
                handleCharacteristic(requestCallback, currentRequest.requestId, Request.RequestType.WRITE_CHARACTERISTIC, gatt, characteristic);
            } else if (status == GATT_REQ_NOT_SUPPORTED) {
                handleFaildCallback(currentRequest.callback, currentRequest.requestId, currentRequest.type, IRequestCallback.GATT_STATUS_REQUEST_NOT_SUPPORTED, currentRequest.value);
            } else {
                handleFaildCallback(currentRequest.callback, currentRequest.requestId, currentRequest.type, IRequestCallback.NONE, currentRequest.value);
            }
            processNextRequest();
        }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        // 收到设备notify值 （设备上报值）
        IRequestCallback callback = requestCallbackContainer.getCallback(characteristic.getService().getUuid(), characteristic.getUuid());
        if (callback != null) {
            callback.onCharacteristicChanged(gatt, characteristic);
        }
        if (requestCallback != null) {
            requestCallback.onCharacteristicChanged(gatt, characteristic);
        }
    }

    @Override
    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
        if (currentRequest != null && currentRequest.type == Request.RequestType.READ_RSSI) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                handleRssiOrMtu(currentRequest.callback, currentRequest.requestId, Request.RequestType.READ_RSSI, gatt, rssi);
                handleRssiOrMtu(requestCallback, currentRequest.requestId, Request.RequestType.READ_RSSI, gatt, rssi);
            } else if (status == GATT_REQ_NOT_SUPPORTED) {
                handleFaildCallback(currentRequest.callback, currentRequest.requestId, currentRequest.type, IRequestCallback.GATT_STATUS_REQUEST_NOT_SUPPORTED, currentRequest.value);
            } else {
                handleFaildCallback(currentRequest.callback, currentRequest.requestId, currentRequest.type, IRequestCallback.NONE, currentRequest.value);
            }
            processNextRequest();
        }
    }

    @Override
    public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        if (currentRequest == null) return;
        BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();
        if (currentRequest.type == Request.RequestType.CHARACTERISTIC_NOTIFICATION) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                handleFaildCallback(currentRequest.callback, currentRequest.requestId, currentRequest.type, IRequestCallback.NONE, currentRequest.value);
                requestCallbackContainer.removeCallback(characteristic.getService().getUuid(), characteristic.getUuid());
            }
            if (characteristic.getService().getUuid().equals(pendingCharacteristic.getService().getUuid())
                    && characteristic.getUuid().equals(pendingCharacteristic.getUuid())) {
                requestCallbackContainer.addCallback(characteristic.getService().getUuid(), characteristic.getUuid(), currentRequest.callback);
                if (!enableNotification(currentRequest.value == null || currentRequest.value.length == 0 || currentRequest.value[0] == 1, characteristic)) {
                    handleFaildCallback(currentRequest.callback, currentRequest.requestId, currentRequest.type, IRequestCallback.NONE, currentRequest.value);
                    requestCallbackContainer.removeCallback(characteristic.getService().getUuid(), characteristic.getUuid());
                }
            }
        } else if (currentRequest.type == Request.RequestType.CHARACTERISTIC_INDICATION) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                handleFaildCallback(currentRequest.callback, currentRequest.requestId, currentRequest.type, IRequestCallback.NONE, currentRequest.value);
                requestCallbackContainer.removeCallback(characteristic.getService().getUuid(), characteristic.getUuid());
            }
            if (characteristic.getService().getUuid().equals(pendingCharacteristic.getService().getUuid())
                    && characteristic.getUuid().equals(pendingCharacteristic.getUuid())) {
                requestCallbackContainer.addCallback(characteristic.getService().getUuid(), characteristic.getUuid(), currentRequest.callback);
                if (!enableIndication(currentRequest.value == null || currentRequest.value.length == 0 || currentRequest.value[0] == 1, characteristic)) {
                    handleFaildCallback(currentRequest.callback, currentRequest.requestId, currentRequest.type, IRequestCallback.NONE, currentRequest.value);
                    requestCallbackContainer.removeCallback(characteristic.getService().getUuid(), characteristic.getUuid());
                }
            }
        } else if (currentRequest.type == Request.RequestType.READ_DESCRIPTOR) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                handleDescriptorOrNotification(currentRequest.callback, currentRequest.requestId, Request.RequestType.READ_DESCRIPTOR, gatt, descriptor, false);
                handleDescriptorOrNotification(requestCallback, currentRequest.requestId, Request.RequestType.READ_DESCRIPTOR, gatt, descriptor, false);
                processNextRequest();
            } else {
                handleFaildCallback(currentRequest.callback, currentRequest.requestId, currentRequest.type, IRequestCallback.NONE, currentRequest.value);
            }
        }
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        if (currentRequest == null) return;
        BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();
        if (currentRequest.type == Request.RequestType.CHARACTERISTIC_NOTIFICATION) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                handleFaildCallback(currentRequest.callback, currentRequest.requestId, currentRequest.type, IRequestCallback.NONE, currentRequest.value);
                requestCallbackContainer.removeCallback(characteristic.getService().getUuid(), characteristic.getUuid());
            } else {
                handleDescriptorOrNotification(currentRequest.callback, currentRequest.requestId, Request.RequestType.CHARACTERISTIC_NOTIFICATION, 
                        gatt, descriptor, currentRequest.value == null || currentRequest.value.length == 0 || currentRequest.value[0] == 1);
                handleDescriptorOrNotification(requestCallback, currentRequest.requestId, Request.RequestType.CHARACTERISTIC_NOTIFICATION,
                        gatt, descriptor, currentRequest.value == null || currentRequest.value.length == 0 || currentRequest.value[0] == 1);
            }
            processNextRequest();
        } else if (currentRequest.type == Request.RequestType.CHARACTERISTIC_INDICATION) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                handleFaildCallback(currentRequest.callback, currentRequest.requestId, currentRequest.type, IRequestCallback.NONE, currentRequest.value);
                requestCallbackContainer.removeCallback(characteristic.getService().getUuid(), characteristic.getUuid());
            } else {
                handleDescriptorOrNotification(currentRequest.callback, currentRequest.requestId, Request.RequestType.CHARACTERISTIC_INDICATION,
                        gatt, descriptor, currentRequest.value == null || currentRequest.value.length == 0 || currentRequest.value[0] == 1);
                handleDescriptorOrNotification(requestCallback, currentRequest.requestId, Request.RequestType.CHARACTERISTIC_INDICATION,
                        gatt, descriptor, currentRequest.value == null || currentRequest.value.length == 0 || currentRequest.value[0] == 1);
            }
            processNextRequest();
        }
    }

    @Override
    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
        if (currentRequest != null && currentRequest.type == Request.RequestType.SET_MTU) {
            if (currentRequest.callback != null) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    handleRssiOrMtu(currentRequest.callback, currentRequest.requestId, Request.RequestType.SET_MTU, gatt, mtu);
                    handleRssiOrMtu(requestCallback, currentRequest.requestId, Request.RequestType.SET_MTU, gatt, mtu);
                } else if (status == GATT_REQ_NOT_SUPPORTED) {
                    handleFaildCallback(currentRequest.callback, currentRequest.requestId, currentRequest.type, IRequestCallback.GATT_STATUS_REQUEST_NOT_SUPPORTED, currentRequest.value);
                } else {
                    handleFaildCallback(currentRequest.callback, currentRequest.requestId, currentRequest.type, IRequestCallback.NONE, currentRequest.value);
                }
            }
            processNextRequest();
        }
    }

    private void handleCharacteristic(IRequestCallback callback, @NonNull String requestId, @NonNull Request.RequestType requestType,
                                      BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if (callback != null) {
            switch(requestType) {
                case READ_CHARACTERISTIC:	
                    callback.onCharacteristicRead(requestId, gatt, characteristic);
            		break;
                case WRITE_CHARACTERISTIC:		
                    callback.onCharacteristicWrite(requestId, gatt, characteristic);
            		break;
            }
        }
    }
        
    private void handleRssiOrMtu(IRequestCallback callback, @NonNull String requestId, @NonNull Request.RequestType requestType, BluetoothGatt gatt, int value) {
        if (callback != null) {
            switch(requestType) {
                case READ_RSSI:
                    callback.onRssiRead(requestId, gatt, value);
                    break;
                case SET_MTU:
                    callback.onMtuChanged(requestId, gatt, value);
                    break;
            }
        }
    }
    
    private void handleDescriptorOrNotification(IRequestCallback callback, @NonNull String requestId, @NonNull Request.RequestType requestType, 
                                                BluetoothGatt gatt, BluetoothGattDescriptor descriptor, boolean register) {
        if (callback != null) {
            switch(requestType) {
                case CHARACTERISTIC_NOTIFICATION:
                    if (register) {
                        callback.onNotificationRegistered(requestId, gatt, descriptor);
                    } else {
                        callback.onNotificationUnregistered(requestId, gatt, descriptor);
                    }
                    break;
                case CHARACTERISTIC_INDICATION:
                    if (register) {
                        callback.onIndicationRegistered(requestId, gatt, descriptor);
                    } else {
                        callback.onIndicationUnregistered(requestId, gatt, descriptor);
                    }
                    break;
                case READ_DESCRIPTOR:
                    callback.onDescriptorRead(requestId, gatt, descriptor);
                    break;
            }
        }
    }

    private void handleFaildCallback(IRequestCallback callback, String requestId, Request.RequestType requestType, int failType, byte[] value) {
        if (callback != null) {
            callback.onRequestFialed(requestId, requestType, failType, value);
        }
        if (requestCallback != null) {
            requestCallback.onRequestFialed(requestId, requestType, failType, value);
        }
    }
    
    public void requestMtu(@NonNull final String requestId, final int mtu, final IRequestCallback callback) {
        requestHandler.post(new Runnable() {
            @Override
            public void run() {
                if (currentRequest == null) {
                    performRequestMtu(requestId, mtu, callback);
                } else {
                    requestQueue.add(new Request(Request.RequestType.SET_MTU, requestId, null, null, null, callback, BleUtils.numberToBytes(mtu, false)));
                }
            }
        });
    }
    
    /*
     * 请求读取characteristic的值
     * @param requestId 请求码
     */
    public void requestCharacteristicValue(@NonNull final String requestId, final UUID service, final UUID characteristic, final IRequestCallback callback) {
        requestHandler.post(new Runnable() {
            @Override
            public void run() {
                if (currentRequest == null) {
                    performCharacteristicValueRequest(requestId, service, characteristic, callback);
                } else {
                    requestQueue.add(new Request(Request.RequestType.READ_CHARACTERISTIC, requestId, service, characteristic,
                            null, callback));
                }
            }
        });
    }

    /*
     * 打开Notifications
     * @param requestId 请求码
     */
    public void requestCharacteristicNotification(@NonNull final String requestId, final UUID service, final UUID characteristic, final IRequestCallback callback, final boolean enable) {
        requestHandler.post(new Runnable() {
            @Override
            public void run() {
                if (currentRequest == null) {
                    performNotificationRequest(requestId, service, characteristic, callback, new byte[]{(byte) (enable ? 1 : 0)});
                } else {
                    requestQueue.add(new Request(Request.RequestType.CHARACTERISTIC_NOTIFICATION, requestId, service,
                            characteristic, null, callback));
                }
            }
        });
    }

    public void requestCharacteristicIndication(@NonNull final String requestId, final UUID service, final UUID characteristic, final IRequestCallback callback, final boolean enable) {
        requestHandler.post(new Runnable() {
            @Override
            public void run() {
                if (currentRequest == null) {
                    performIndicationRequest(requestId, service, characteristic, callback, new byte[]{(byte) (enable ? 1 : 0)});
                } else {
                    requestQueue.add(new Request(Request.RequestType.CHARACTERISTIC_INDICATION, requestId, service,
                            characteristic, null, callback));
                }
            }
        });
    }

    public void requestDescriptorValue(@NonNull final String requestId, final UUID service, final UUID characteristic, final UUID descriptor, final IRequestCallback callback) {
        requestHandler.post(new Runnable() {
            @Override
            public void run() {
                if (currentRequest == null) {
                    performDescriptorValueRequest(requestId, service, characteristic, descriptor, callback);
                } else {
                    requestQueue.add(new Request(Request.RequestType.READ_DESCRIPTOR, requestId, service, characteristic, descriptor, callback));
                }
            }
        });
    }

    public void writeCharacteristicValue(@NonNull final String requestId, final UUID service, final UUID characteristic, final byte[] value, final IRequestCallback callback) {
        requestHandler.post(new Runnable() {
            @Override
            public void run() {
                if (currentRequest == null) {
                    performCharacteristicWrite(requestId, service, characteristic, callback, value);
                } else {
                    requestQueue.add(new Request(Request.RequestType.WRITE_CHARACTERISTIC, requestId, service, characteristic, null, callback, value));
                }
            }
        });
    }

    public void requestRssiValue(@NonNull final String requestId, final IRequestCallback callback) {
        requestHandler.post(new Runnable() {
            @Override
            public void run() {
                if (currentRequest == null) {
                    performRssiValueRequest(requestId, callback);
                } else {
                    requestQueue.add(new Request(Request.RequestType.READ_RSSI, requestId, null, null, null, callback));
                }
            }
        });
    }

    private void processNextRequest() {
        requestHandler.post(new Runnable() {
            @Override
            public void run() {
                if (requestQueue.isEmpty()) {
                    currentRequest = null;
                    return;
                }
                Request request = requestQueue.remove();
                switch (request.type) {
                    case CHARACTERISTIC_NOTIFICATION:
                        performNotificationRequest(request.requestId, request.service, request.characteristic, request.callback, request.value);
                        break;
                    case CHARACTERISTIC_INDICATION:
                        performIndicationRequest(request.requestId, request.service, request.characteristic, request.callback, request.value);
                        break;
                    case READ_CHARACTERISTIC:
                        performCharacteristicValueRequest(request.requestId, request.service, request.characteristic, request.callback);
                        break;
                    case READ_DESCRIPTOR:
                        performDescriptorValueRequest(request.requestId, request.service, request.characteristic, request.descriptor, request.callback);
                        break;
                    case WRITE_CHARACTERISTIC:
                        performCharacteristicWrite(request.requestId, request.service, request.characteristic, request.callback, request.value);
                        break;
                    case READ_RSSI:
                        performRssiValueRequest(request.requestId, request.callback);
                        break;
                    case SET_MTU:
                        performRequestMtu(request.requestId, (int) BleUtils.bytesToLong(request.value, false), request.callback);
                        break;
                }
            }
        });
    }

    private void performRequestMtu(String requestId, int mtu, IRequestCallback callback) {
        if (bluetoothAdapter.isEnabled()) {
            currentRequest = new Request(Request.RequestType.SET_MTU, requestId, null, null, null, callback);
            if (bluetoothGatt != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    if (!bluetoothGatt.requestMtu(mtu)) {
                        handleFaildCallback(callback, currentRequest.requestId, currentRequest.type, IRequestCallback.NONE, currentRequest.value);
                        processNextRequest();
                    }
                } else {
                    handleFaildCallback(callback, currentRequest.requestId, currentRequest.type, IRequestCallback.API_LEVEL_TOO_LOW, currentRequest.value);
                    processNextRequest();
                }
            } else {
                handleFaildCallback(callback, currentRequest.requestId, currentRequest.type, IRequestCallback.GATT_IS_NULL, currentRequest.value);
                processNextRequest();
            }
        }
    }
    
    private void performRssiValueRequest(String requestId, IRequestCallback callback) {
        if (bluetoothAdapter.isEnabled()) {
            currentRequest = new Request(Request.RequestType.READ_RSSI, requestId, null, null, null, callback);
            if (bluetoothGatt != null) {
                if (!bluetoothGatt.readRemoteRssi()) {
                    handleFaildCallback(callback, currentRequest.requestId, currentRequest.type, IRequestCallback.NONE, currentRequest.value);
                    processNextRequest();
                }
            } else {
                handleFaildCallback(callback, currentRequest.requestId, currentRequest.type, IRequestCallback.GATT_IS_NULL, currentRequest.value);
                processNextRequest();
            }
        }
    }

    private void performCharacteristicValueRequest(String requestId, UUID service, UUID characteristic, IRequestCallback callback) {
        if (bluetoothAdapter.isEnabled()) {
            currentRequest = new Request(Request.RequestType.READ_CHARACTERISTIC, requestId, service, characteristic,
                    null, callback);
            if (bluetoothGatt != null) {
                BluetoothGattService gattService = bluetoothGatt.getService(service);
                if (gattService != null) {
                    BluetoothGattCharacteristic gattCharacteristic = gattService.getCharacteristic(characteristic);
                    if (gattCharacteristic != null) {
                        if (!bluetoothGatt.readCharacteristic(gattCharacteristic)) {
                            handleFaildCallback(callback, currentRequest.requestId, currentRequest.type, IRequestCallback.NONE, currentRequest.value);
                            processNextRequest();
                        }
                    } else {
                        handleFaildCallback(callback, currentRequest.requestId, currentRequest.type, IRequestCallback.NULL_CHARACTERISTIC, currentRequest.value);
                        processNextRequest();
                    }
                } else {
                    handleFaildCallback(callback, currentRequest.requestId, currentRequest.type, IRequestCallback.NULL_SERVICE, currentRequest.value);
                    processNextRequest();
                }
            } else {
                handleFaildCallback(callback, currentRequest.requestId, currentRequest.type, IRequestCallback.GATT_IS_NULL, currentRequest.value);
                processNextRequest();
            }
        }
    }

    private void performCharacteristicWrite(final String requestId, final UUID service, final UUID characteristic, final IRequestCallback callback, final byte[] value) {
        requestHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (value.length > getPackageSize()) {
                    List<byte[]> list = BleUtils.splitPackage(value, getPackageSize());
                    for (byte[] bytes : list) {
                        doWrite(requestId, service, characteristic, callback, bytes);
                    }
                } else {
                    doWrite(requestId, service, characteristic, callback, value);
                }
            }
        }, getWriteDelayMillis());
    }

    private void doWrite(String requestId, UUID service, UUID characteristic, IRequestCallback callback, byte[] value) {
        if (bluetoothAdapter.isEnabled()) {
            currentRequest = new Request(Request.RequestType.WRITE_CHARACTERISTIC, requestId, service, characteristic,
                    null, callback, value);
            if (bluetoothGatt != null) {
                BluetoothGattService gattService = bluetoothGatt.getService(service);
                if (gattService != null) {
                    BluetoothGattCharacteristic gattCharacteristic = gattService.getCharacteristic(characteristic);
                    if (gattCharacteristic != null) {
                        gattCharacteristic.setValue(value);
                        if (!bluetoothGatt.writeCharacteristic(gattCharacteristic)) {
                            handleFaildCallback(callback, currentRequest.requestId, currentRequest.type, IRequestCallback.NONE, currentRequest.value);
                            processNextRequest();
                        }
                    } else {
                        handleFaildCallback(callback, currentRequest.requestId, currentRequest.type, IRequestCallback.NULL_CHARACTERISTIC, currentRequest.value);
                        processNextRequest();
                    }
                } else {
                    handleFaildCallback(callback, currentRequest.requestId, currentRequest.type, IRequestCallback.NULL_SERVICE, currentRequest.value);
                    processNextRequest();
                }
            } else {
                handleFaildCallback(callback, currentRequest.requestId, currentRequest.type, IRequestCallback.GATT_IS_NULL, currentRequest.value);
                processNextRequest();
            }
        }
    }

    private void performDescriptorValueRequest(String requestId, UUID service, UUID characteristic, UUID descriptor, IRequestCallback callback) {
        if (bluetoothAdapter.isEnabled()) {
            currentRequest = new Request(Request.RequestType.READ_DESCRIPTOR, requestId, service, characteristic,
                    descriptor, callback);
            if (bluetoothGatt != null) {
                BluetoothGattService gattService = bluetoothGatt.getService(service);
                if (gattService != null) {
                    BluetoothGattCharacteristic gattCharacteristic = gattService.getCharacteristic(characteristic);
                    if (gattCharacteristic != null) {
                        BluetoothGattDescriptor gattDescriptor = gattCharacteristic.getDescriptor(descriptor);
                        if (gattDescriptor != null) {
                            if (!bluetoothGatt.readDescriptor(gattDescriptor)) {
                                handleFaildCallback(callback, currentRequest.requestId, currentRequest.type, IRequestCallback.NONE, currentRequest.value);
                                processNextRequest();
                            }
                        } else {
                            handleFaildCallback(callback, currentRequest.requestId, currentRequest.type, IRequestCallback.NULL_DESCRIPTOR, currentRequest.value);
                            processNextRequest();
                        }
                    } else {
                        handleFaildCallback(callback, currentRequest.requestId, currentRequest.type, IRequestCallback.NULL_CHARACTERISTIC, currentRequest.value);
                        processNextRequest();
                    }
                } else {
                    handleFaildCallback(callback, currentRequest.requestId, currentRequest.type, IRequestCallback.NULL_SERVICE, currentRequest.value);
                    processNextRequest();
                }
            } else {
                handleFaildCallback(callback, currentRequest.requestId, currentRequest.type, IRequestCallback.GATT_IS_NULL, currentRequest.value);
                processNextRequest();
            }
        }
    }

    private void performIndicationRequest(String requestId, UUID service, UUID characteristic, IRequestCallback callback, byte[] value) {
        if (bluetoothAdapter.isEnabled()) {
            currentRequest = new Request(Request.RequestType.CHARACTERISTIC_INDICATION, requestId, service, characteristic,
                    null, callback, value);
            if (bluetoothGatt != null) {
                BluetoothGattService gattService = bluetoothGatt.getService(service);
                if (gattService != null) {
                    pendingCharacteristic = gattService.getCharacteristic(characteristic);
                    if (pendingCharacteristic != null) {
                        BluetoothGattDescriptor gattDescriptor = pendingCharacteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
                        if (gattDescriptor == null || !bluetoothGatt.readDescriptor(gattDescriptor)) {
                            handleFaildCallback(callback, currentRequest.requestId, currentRequest.type, IRequestCallback.NONE, currentRequest.value);
                            processNextRequest();
                        }
                    } else {
                        handleFaildCallback(callback, currentRequest.requestId, currentRequest.type, IRequestCallback.NULL_CHARACTERISTIC, currentRequest.value);
                        processNextRequest();
                    }
                } else {
                    handleFaildCallback(callback, currentRequest.requestId, currentRequest.type, IRequestCallback.NULL_SERVICE, currentRequest.value);
                    processNextRequest();
                }
            } else {
                handleFaildCallback(callback, currentRequest.requestId, currentRequest.type, IRequestCallback.GATT_IS_NULL, currentRequest.value);
                processNextRequest();
            }
        }
    }

    private void performNotificationRequest(String requestId, UUID service, UUID characteristic, IRequestCallback callback, byte[] value) {
        if (bluetoothAdapter.isEnabled()) {
            currentRequest = new Request(Request.RequestType.CHARACTERISTIC_NOTIFICATION, requestId, service, characteristic,
                    null, callback, value);
            if (bluetoothGatt != null) {
                BluetoothGattService gattService = bluetoothGatt.getService(service);
                if (gattService != null) {
                    pendingCharacteristic = gattService.getCharacteristic(characteristic);
                    if (pendingCharacteristic != null) {
                        BluetoothGattDescriptor gattDescriptor = pendingCharacteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
                        if (gattDescriptor == null || !bluetoothGatt.readDescriptor(gattDescriptor)) {
                            handleFaildCallback(callback, currentRequest.requestId, currentRequest.type, IRequestCallback.NONE, currentRequest.value);
                            processNextRequest();
                        }
                    } else {
                        handleFaildCallback(callback, currentRequest.requestId, currentRequest.type, IRequestCallback.NULL_CHARACTERISTIC, currentRequest.value);
                        processNextRequest();
                    }
                } else {
                    handleFaildCallback(callback, currentRequest.requestId, currentRequest.type, IRequestCallback.NULL_SERVICE, currentRequest.value);
                    processNextRequest();
                }
            } else {
                handleFaildCallback(callback, currentRequest.requestId, currentRequest.type, IRequestCallback.GATT_IS_NULL, currentRequest.value);
                processNextRequest();
            }
        }
    }

    private boolean enableNotification(boolean enable, BluetoothGattCharacteristic characteristic) {
        if (!bluetoothAdapter.isEnabled() || bluetoothGatt == null) return false;
        if (!bluetoothGatt.setCharacteristicNotification(characteristic, enable)) return false;
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
        if (descriptor == null) return false;

        if (enable) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        } else {
            descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        }

        return bluetoothGatt.writeDescriptor(descriptor);
    }

    private boolean enableIndication(boolean enable, BluetoothGattCharacteristic characteristic) {
        if (!bluetoothAdapter.isEnabled() || bluetoothGatt == null) return false;
        if (!bluetoothGatt.setCharacteristicNotification(characteristic, enable)) return false;
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
        if (descriptor == null) return false;

        if (enable) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
        }
        else {
            descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        }

        return bluetoothGatt.writeDescriptor(descriptor);
    }
}
