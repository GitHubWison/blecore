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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import cn.zfs.blelib.callback.RequestCallback;
import cn.zfs.blelib.util.BleUtils;

/**
 * 描述: 蓝牙连接基类
 * 时间: 2018/4/11 16:37
 * 作者: zengfansheng
 */
public abstract class Connection extends BluetoothGattCallback {
    private static final UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
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

    Connection() {
        handlerThread = new HandlerThread("ConnectionThread");
        handlerThread.start();
        requestHandler = new Handler(handlerThread.getLooper());
    }

    public void release() {
        requestHandler.removeCallbacksAndMessages(null);
        handlerThread.quit();
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
            if (currentRequest.callback != null) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    currentRequest.callback.onCharacteristicRead(currentRequest.requestId, characteristic);
                } else {
                    performFaildCallback(currentRequest.callback, currentRequest.requestId, currentRequest.type, RequestCallback.NONE, currentRequest.value);
                }
            }
            processNextRequest();
        }
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        if (currentRequest != null && currentRequest.type == Request.RequestType.WRITE_CHARACTERISTIC) {
            if (currentRequest.callback != null) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    currentRequest.callback.onCharacteristicWrite(currentRequest.requestId, characteristic);
                } else if (status == GATT_REQ_NOT_SUPPORTED) {
                    performFaildCallback(currentRequest.callback, currentRequest.requestId, currentRequest.type, RequestCallback.GATT_STATUS_REQUEST_NOT_SUPPORTED, currentRequest.value);
                } else {
                    performFaildCallback(currentRequest.callback, currentRequest.requestId, currentRequest.type, RequestCallback.NONE, currentRequest.value);
                }
            }
            processNextRequest();
        }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        // 收到设备notify值 （设备上报值）
        RequestCallback callback = requestCallbackContainer.getCallback(characteristic.getService().getUuid(), characteristic.getUuid());
        if (callback != null) {
            callback.onCharacteristicChanged(characteristic);
        }
    }

    @Override
    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
        if (currentRequest != null && currentRequest.type == Request.RequestType.READ_RSSI) {
            if (currentRequest.callback != null) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    currentRequest.callback.onRssiRead(rssi);
                } else if (status == GATT_REQ_NOT_SUPPORTED) {
                    performFaildCallback(currentRequest.callback, currentRequest.requestId, currentRequest.type, RequestCallback.GATT_STATUS_REQUEST_NOT_SUPPORTED, currentRequest.value);
                } else {
                    performFaildCallback(currentRequest.callback, currentRequest.requestId, currentRequest.type, RequestCallback.NONE, currentRequest.value);
                }
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
                performFaildCallback(currentRequest.callback, currentRequest.requestId, currentRequest.type, RequestCallback.NONE, currentRequest.value);
                requestCallbackContainer.removeCallback(characteristic.getService().getUuid(), characteristic.getUuid());
            }
            if (characteristic.getService().getUuid().equals(pendingCharacteristic.getService().getUuid())
                    && characteristic.getUuid().equals(pendingCharacteristic.getUuid())) {
                requestCallbackContainer.addCallback(characteristic.getService().getUuid(), characteristic.getUuid(), currentRequest.callback);
                if (!enableNotification(currentRequest.value == null || currentRequest.value.length == 0 || currentRequest.value[0] == 1, characteristic)) {
                    performFaildCallback(currentRequest.callback, currentRequest.requestId, currentRequest.type, RequestCallback.NONE, currentRequest.value);
                    requestCallbackContainer.removeCallback(characteristic.getService().getUuid(), characteristic.getUuid());
                }
            }
        } else if (currentRequest.type == Request.RequestType.CHARACTERISTIC_INDICATION) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                performFaildCallback(currentRequest.callback, currentRequest.requestId, currentRequest.type, RequestCallback.NONE, currentRequest.value);
                requestCallbackContainer.removeCallback(characteristic.getService().getUuid(), characteristic.getUuid());
            }
            if (characteristic.getService().getUuid().equals(pendingCharacteristic.getService().getUuid())
                    && characteristic.getUuid().equals(pendingCharacteristic.getUuid())) {
                requestCallbackContainer.addCallback(characteristic.getService().getUuid(), characteristic.getUuid(), currentRequest.callback);
                if (!enableIndication(currentRequest.value == null || currentRequest.value.length == 0 || currentRequest.value[0] == 1, characteristic)) {
                    performFaildCallback(currentRequest.callback, currentRequest.requestId, currentRequest.type, RequestCallback.NONE, currentRequest.value);
                    requestCallbackContainer.removeCallback(characteristic.getService().getUuid(), characteristic.getUuid());
                }
            }
        } else if (currentRequest.type == Request.RequestType.READ_DESCRIPTOR) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (currentRequest.callback != null) {
                    currentRequest.callback.onDescriptorRead(currentRequest.requestId, descriptor);
                }
                processNextRequest();
            } else {
                performFaildCallback(currentRequest.callback, currentRequest.requestId, currentRequest.type, RequestCallback.NONE, currentRequest.value);
            }
        }
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        if (currentRequest == null) return;
        BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();
        if (currentRequest.type == Request.RequestType.CHARACTERISTIC_NOTIFICATION) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                performFaildCallback(currentRequest.callback, currentRequest.requestId, currentRequest.type, RequestCallback.NONE, currentRequest.value);
                requestCallbackContainer.removeCallback(characteristic.getService().getUuid(), characteristic.getUuid());
            } else if (currentRequest.callback != null) {
                if (currentRequest.value == null || currentRequest.value.length == 0 || currentRequest.value[0] == 1) {
                    currentRequest.callback.onNotificationRegistered(currentRequest.requestId, descriptor);
                } else {
                    currentRequest.callback.onNotificationUnregistered(currentRequest.requestId, descriptor);
                }
            }
            processNextRequest();
        } else if (currentRequest.type == Request.RequestType.CHARACTERISTIC_INDICATION) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                performFaildCallback(currentRequest.callback, currentRequest.requestId, currentRequest.type, RequestCallback.NONE, currentRequest.value);
                requestCallbackContainer.removeCallback(characteristic.getService().getUuid(), characteristic.getUuid());
            } else if (currentRequest.callback != null) {
                if (currentRequest.value == null || currentRequest.value.length == 0 || currentRequest.value[0] == 1) {
                    currentRequest.callback.onIndicationRegistered(currentRequest.requestId, descriptor);
                } else {
                    currentRequest.callback.onIndicationUnregistered(currentRequest.requestId, descriptor);
                }
            }
            processNextRequest();
        }
    }

    
    public void requestMtu(final String requestId, final int mtu, final RequestCallback callback) {
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
    public void requestCharacteristicValue(final String requestId, final UUID service, final UUID characteristic, final RequestCallback callback) {
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
    public void requestCharacteristicNotification(final String requestId, final UUID service, final UUID characteristic, final RequestCallback callback, final boolean enable) {
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

    public void requestCharacteristicIndication(final String requestId, final UUID service, final UUID characteristic, final RequestCallback callback, final boolean enable) {
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

    public void requestDescriptorValue(final String requestId, final UUID service, final UUID characteristic, final UUID descriptor, final RequestCallback callback) {
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

    public void writeCharacteristicValue(final String requestId, final UUID service, final UUID characteristic, final byte[] value, final RequestCallback callback) {
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

    public void requestRssiValue(final String requestId, final RequestCallback callback) {
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

    private void performRequestMtu(String requestId, int mtu, RequestCallback callback) {
        if (bluetoothAdapter.isEnabled()) {
            currentRequest = new Request(Request.RequestType.SET_MTU, requestId, null, null, null, callback);
            if (bluetoothGatt != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    if (!bluetoothGatt.requestMtu(mtu)) {
                        performFaildCallback(callback, currentRequest.requestId, currentRequest.type, RequestCallback.NONE, currentRequest.value);
                        processNextRequest();
                    }
                } else {
                    performFaildCallback(callback, currentRequest.requestId, currentRequest.type, RequestCallback.API_LEVEL_TOO_LOW, currentRequest.value);
                    processNextRequest();
                }
            } else {
                performFaildCallback(callback, currentRequest.requestId, currentRequest.type, RequestCallback.GATT_IS_NULL, currentRequest.value);
                processNextRequest();
            }
        }
    }
    
    private void performRssiValueRequest(String requestId, RequestCallback callback) {
        if (bluetoothAdapter.isEnabled()) {
            currentRequest = new Request(Request.RequestType.READ_RSSI, requestId, null, null, null, callback);
            if (bluetoothGatt != null) {
                if (!bluetoothGatt.readRemoteRssi()) {
                    performFaildCallback(callback, currentRequest.requestId, currentRequest.type, RequestCallback.NONE, currentRequest.value);
                    processNextRequest();
                }
            } else {
                performFaildCallback(callback, currentRequest.requestId, currentRequest.type, RequestCallback.GATT_IS_NULL, currentRequest.value);
                processNextRequest();
            }
        }
    }

    private void performFaildCallback(RequestCallback callback, String requestId, Request.RequestType requestType, int failType, byte[] value) {
        if (callback != null) {
            callback.onRequestFialed(requestId, requestType, failType, value);
        }
    }

    private void performCharacteristicValueRequest(String requestId, UUID service, UUID characteristic, RequestCallback callback) {
        if (bluetoothAdapter.isEnabled()) {
            currentRequest = new Request(Request.RequestType.READ_CHARACTERISTIC, requestId, service, characteristic,
                    null, callback);
            if (bluetoothGatt != null) {
                BluetoothGattService gattService = bluetoothGatt.getService(service);
                if (gattService != null) {
                    BluetoothGattCharacteristic gattCharacteristic = gattService.getCharacteristic(characteristic);
                    if (gattCharacteristic != null) {
                        if (!bluetoothGatt.readCharacteristic(gattCharacteristic)) {
                            performFaildCallback(callback, currentRequest.requestId, currentRequest.type, RequestCallback.NONE, currentRequest.value);
                            processNextRequest();
                        }
                    } else {
                        performFaildCallback(callback, currentRequest.requestId, currentRequest.type, RequestCallback.NULL_CHARACTERISTIC, currentRequest.value);
                        processNextRequest();
                    }
                } else {
                    performFaildCallback(callback, currentRequest.requestId, currentRequest.type, RequestCallback.NULL_SERVICE, currentRequest.value);
                    processNextRequest();
                }
            } else {
                performFaildCallback(callback, currentRequest.requestId, currentRequest.type, RequestCallback.GATT_IS_NULL, currentRequest.value);
                processNextRequest();
            }
        }
    }

    private void performCharacteristicWrite(final String requestId, final UUID service, final UUID characteristic, final RequestCallback callback, final byte[] value) {
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

    private void doWrite(String requestId, UUID service, UUID characteristic, RequestCallback callback, byte[] value) {
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
                            performFaildCallback(callback, currentRequest.requestId, currentRequest.type, RequestCallback.NONE, currentRequest.value);
                            processNextRequest();
                        }
                    } else {
                        performFaildCallback(callback, currentRequest.requestId, currentRequest.type, RequestCallback.NULL_CHARACTERISTIC, currentRequest.value);
                        processNextRequest();
                    }
                } else {
                    performFaildCallback(callback, currentRequest.requestId, currentRequest.type, RequestCallback.NULL_SERVICE, currentRequest.value);
                    processNextRequest();
                }
            } else {
                performFaildCallback(callback, currentRequest.requestId, currentRequest.type, RequestCallback.GATT_IS_NULL, currentRequest.value);
                processNextRequest();
            }
        }
    }

    private void performDescriptorValueRequest(String requestId, UUID service, UUID characteristic, UUID descriptor, RequestCallback callback) {
        if (bluetoothAdapter.isEnabled()) {
            currentRequest = new Request(Request.RequestType.READ_CHARACTERISTIC, requestId, service, characteristic,
                    descriptor, callback);
            if (bluetoothGatt != null) {
                BluetoothGattService gattService = bluetoothGatt.getService(service);
                if (gattService != null) {
                    BluetoothGattCharacteristic gattCharacteristic = gattService.getCharacteristic(characteristic);
                    if (gattCharacteristic != null) {
                        BluetoothGattDescriptor gattDescriptor = gattCharacteristic.getDescriptor(descriptor);
                        if (gattDescriptor != null) {
                            if (!bluetoothGatt.readDescriptor(gattDescriptor)) {
                                performFaildCallback(callback, currentRequest.requestId, currentRequest.type, RequestCallback.NONE, currentRequest.value);
                                processNextRequest();
                            }
                        } else {
                            performFaildCallback(callback, currentRequest.requestId, currentRequest.type, RequestCallback.NULL_DESCRIPTOR, currentRequest.value);
                            processNextRequest();
                        }
                    } else {
                        performFaildCallback(callback, currentRequest.requestId, currentRequest.type, RequestCallback.NULL_CHARACTERISTIC, currentRequest.value);
                        processNextRequest();
                    }
                } else {
                    performFaildCallback(callback, currentRequest.requestId, currentRequest.type, RequestCallback.NULL_SERVICE, currentRequest.value);
                    processNextRequest();
                }
            } else {
                performFaildCallback(callback, currentRequest.requestId, currentRequest.type, RequestCallback.GATT_IS_NULL, currentRequest.value);
                processNextRequest();
            }
        }
    }

    private void performIndicationRequest(String requestId, UUID service, UUID characteristic, RequestCallback callback, byte[] value) {
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
                            performFaildCallback(callback, currentRequest.requestId, currentRequest.type, RequestCallback.NONE, currentRequest.value);
                            processNextRequest();
                        }
                    } else {
                        performFaildCallback(callback, currentRequest.requestId, currentRequest.type, RequestCallback.NULL_CHARACTERISTIC, currentRequest.value);
                        processNextRequest();
                    }
                } else {
                    performFaildCallback(callback, currentRequest.requestId, currentRequest.type, RequestCallback.NULL_SERVICE, currentRequest.value);
                    processNextRequest();
                }
            } else {
                performFaildCallback(callback, currentRequest.requestId, currentRequest.type, RequestCallback.GATT_IS_NULL, currentRequest.value);
                processNextRequest();
            }
        }
    }

    private void performNotificationRequest(String requestId, UUID service, UUID characteristic, RequestCallback callback, byte[] value) {
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
                            performFaildCallback(callback, currentRequest.requestId, currentRequest.type, RequestCallback.NONE, currentRequest.value);
                            processNextRequest();
                        }
                    } else {
                        performFaildCallback(callback, currentRequest.requestId, currentRequest.type, RequestCallback.NULL_CHARACTERISTIC, currentRequest.value);
                        processNextRequest();
                    }
                } else {
                    performFaildCallback(callback, currentRequest.requestId, currentRequest.type, RequestCallback.NULL_SERVICE, currentRequest.value);
                    processNextRequest();
                }
            } else {
                performFaildCallback(callback, currentRequest.requestId, currentRequest.type, RequestCallback.GATT_IS_NULL, currentRequest.value);
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
