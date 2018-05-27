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
import android.support.annotation.NonNull;

import java.lang.reflect.Method;
import java.util.Arrays;
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

    private static final short  GATT_REQ_NOT_SUPPORTED = 6;
    protected BluetoothDevice bluetoothDevice;
    protected BluetoothGatt bluetoothGatt;
    protected Queue<Request> requestQueue = new ConcurrentLinkedQueue<>();
    protected Request currentRequest;
    private BluetoothGattCharacteristic pendingCharacteristic;
    protected BluetoothAdapter bluetoothAdapter;
    private IRequestCallback requestCallback;
    private Handler handler;
    private HandlerThread handlerThread;

    BaseConnection(BluetoothDevice bluetoothDevice) {
        this.bluetoothDevice = bluetoothDevice;
        requestCallback = getRequestCallback();
        handlerThread = new HandlerThread("WriteThread");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    public void clearRequestQueue() {
        requestQueue.clear();
        currentRequest = null;
    }

    public void release() {
        handlerThread.quit();
    }

    protected abstract @NonNull IRequestCallback getRequestCallback();

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
                requestCallback.onCharacteristicRead(currentRequest.requestId, gatt, characteristic);
            } else {
                handleFaildCallback(currentRequest.requestId, currentRequest.type, IRequestCallback.NONE, currentRequest.value, false);
            }
            processNextRequest();
        }
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        if (currentRequest != null && currentRequest.waitWriteResult && currentRequest.type == Request.RequestType.WRITE_CHARACTERISTIC) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                requestCallback.onCharacteristicWrite(currentRequest.requestId, gatt, characteristic);
            } else if (status == GATT_REQ_NOT_SUPPORTED) {
                handleFaildCallback(currentRequest.requestId, currentRequest.type, IRequestCallback.GATT_STATUS_REQUEST_NOT_SUPPORTED, currentRequest.value, false);
            } else {
                handleFaildCallback(currentRequest.requestId, currentRequest.type, IRequestCallback.NONE, currentRequest.value, false);
            }
            processNextRequest();
        }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        // 收到设备notify值 （设备上报值）
        requestCallback.onCharacteristicChanged(gatt, characteristic);
    }

    @Override
    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
        if (currentRequest != null && currentRequest.type == Request.RequestType.READ_RSSI) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                requestCallback.onReadRemoteRssi(currentRequest.requestId, gatt, rssi);
            } else if (status == GATT_REQ_NOT_SUPPORTED) {
                handleFaildCallback(currentRequest.requestId, currentRequest.type, IRequestCallback.GATT_STATUS_REQUEST_NOT_SUPPORTED, currentRequest.value, false);
            } else {
                handleFaildCallback(currentRequest.requestId, currentRequest.type, IRequestCallback.NONE, currentRequest.value, false);
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
                handleFaildCallback(currentRequest.requestId, currentRequest.type, IRequestCallback.NONE, currentRequest.value, false);
            }
            if (characteristic.getService().getUuid().equals(pendingCharacteristic.getService().getUuid()) && characteristic.getUuid().equals(pendingCharacteristic.getUuid())) {
                if (!enableNotification(Arrays.equals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, currentRequest.value), characteristic)) {
                    handleFaildCallback(currentRequest.requestId, currentRequest.type, IRequestCallback.NONE, currentRequest.value, false);
                }
            }
        } else if (currentRequest.type == Request.RequestType.CHARACTERISTIC_INDICATION) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                handleFaildCallback(currentRequest.requestId, currentRequest.type, IRequestCallback.NONE, currentRequest.value, false);
            }
            if (characteristic.getService().getUuid().equals(pendingCharacteristic.getService().getUuid()) && characteristic.getUuid().equals(pendingCharacteristic.getUuid())) {
                if (!enableIndication(Arrays.equals(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE, currentRequest.value), characteristic)) {
                    handleFaildCallback(currentRequest.requestId, currentRequest.type, IRequestCallback.NONE, currentRequest.value, false);
                }
            }
        } else if (currentRequest.type == Request.RequestType.READ_DESCRIPTOR) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                requestCallback.onDescriptorRead(currentRequest.requestId, gatt, descriptor);
            } else {
                handleFaildCallback(currentRequest.requestId, currentRequest.type, IRequestCallback.NONE, currentRequest.value, false);
            }
            processNextRequest();
        }
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        if (currentRequest == null) return;
        if (currentRequest.type == Request.RequestType.CHARACTERISTIC_NOTIFICATION) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                handleFaildCallback(currentRequest.requestId, currentRequest.type, IRequestCallback.NONE, currentRequest.value, false);
            } else {
                if (Arrays.equals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, currentRequest.value)) {
                    requestCallback.onNotificationRegistered(currentRequest.requestId, gatt, descriptor);
                } else {
                    requestCallback.onNotificationUnregistered(currentRequest.requestId, gatt, descriptor);
                }
            }
            processNextRequest();
        } else if (currentRequest.type == Request.RequestType.CHARACTERISTIC_INDICATION) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                handleFaildCallback(currentRequest.requestId, currentRequest.type, IRequestCallback.NONE, currentRequest.value, false);
            } else {
                if (Arrays.equals(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE, currentRequest.value)) {
                    requestCallback.onIndicationRegistered(currentRequest.requestId, gatt, descriptor);
                } else {
                    requestCallback.onIndicationUnregistered(currentRequest.requestId, gatt, descriptor);
                }
            }
            processNextRequest();
        }
    }

    @Override
    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
        if (currentRequest != null && currentRequest.type == Request.RequestType.SET_MTU) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                requestCallback.onMtuChanged(currentRequest.requestId, gatt, mtu);
            } else if (status == GATT_REQ_NOT_SUPPORTED) {
                handleFaildCallback(currentRequest.requestId, currentRequest.type, IRequestCallback.GATT_STATUS_REQUEST_NOT_SUPPORTED, currentRequest.value, false);
            } else {
                handleFaildCallback(currentRequest.requestId, currentRequest.type, IRequestCallback.NONE, currentRequest.value, false);
            }
            processNextRequest();
        }
    }

    private void handleFaildCallback(String requestId, Request.RequestType requestType, int failType, byte[] value, boolean processNext) {
        requestCallback.onRequestFialed(requestId, requestType, failType, value);
        if (processNext) {
            processNextRequest();
        }
    }

    public void requestMtu(@NonNull final String requestId, final int mtu) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (currentRequest == null) {
                    performRequestMtu(requestId, mtu);
                } else {
                    requestQueue.add(new Request(Request.RequestType.SET_MTU, requestId, null, null, null, BleUtils.numberToBytes(mtu, false)));
                }
            }
        });
    }

    /*
     * 请求读取characteristic的值
     * @param requestId 请求码
     */
    public void requestCharacteristicValue(@NonNull final String requestId, final UUID service, final UUID characteristic) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (currentRequest == null) {
                    performCharacteristicValueRequest(requestId, service, characteristic);
                } else {
                    requestQueue.add(new Request(Request.RequestType.READ_CHARACTERISTIC, requestId, service, characteristic,
                            null));
                }
            }
        });
    }

    /**
     * 打开Notifications
     * @param requestId 请求码
     * @param enable 开启还是关闭
     */
    public void requestCharacteristicNotification(@NonNull final String requestId, final UUID service, final UUID characteristic, final boolean enable) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (currentRequest == null) {
                    performNotificationRequest(requestId, service, characteristic, enable ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                } else {
                    requestQueue.add(new Request(Request.RequestType.CHARACTERISTIC_NOTIFICATION, requestId, service,
                            characteristic, null));
                }
            }
        });
    }

    /**
     * @param enable 开启还是关闭
     */
    public void requestCharacteristicIndication(@NonNull final String requestId, final UUID service, final UUID characteristic, final boolean enable) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (currentRequest == null) {
                    performIndicationRequest(requestId, service, characteristic, enable ? BluetoothGattDescriptor.ENABLE_INDICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                } else {
                    requestQueue.add(new Request(Request.RequestType.CHARACTERISTIC_INDICATION, requestId, service,
                            characteristic, null));
                }
            }
        });
    }

    public void requestDescriptorValue(@NonNull final String requestId, final UUID service, final UUID characteristic, final UUID descriptor) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (currentRequest == null) {
                    performDescriptorValueRequest(requestId, service, characteristic, descriptor);
                } else {
                    requestQueue.add(new Request(Request.RequestType.READ_DESCRIPTOR, requestId, service, characteristic, descriptor));
                }
            }
        });
    }

    public void writeCharacteristicValue(@NonNull final String requestId, final UUID service, final UUID characteristic, final byte[] value) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (currentRequest == null) {
                    performCharacteristicWrite(requestId, service, characteristic, value);
                } else {
                    requestQueue.add(new Request(Request.RequestType.WRITE_CHARACTERISTIC, requestId, service, characteristic, null, value));
                }
            }
        });
    }

    public void requestRssiValue(@NonNull final String requestId) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (currentRequest == null) {
                    performRssiValueRequest(requestId);
                } else {
                    requestQueue.add(new Request(Request.RequestType.READ_RSSI, requestId, null, null, null));
                }
            }
        });
    }

    private synchronized void processNextRequest() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (requestQueue.isEmpty()) {
                    currentRequest = null;
                    return;
                }
                Request request = requestQueue.remove();
                switch (request.type) {
                    case CHARACTERISTIC_NOTIFICATION:
                        performNotificationRequest(request.requestId, request.service, request.characteristic, request.value);
                        break;
                    case CHARACTERISTIC_INDICATION:
                        performIndicationRequest(request.requestId, request.service, request.characteristic, request.value);
                        break;
                    case READ_CHARACTERISTIC:
                        performCharacteristicValueRequest(request.requestId, request.service, request.characteristic);
                        break;
                    case READ_DESCRIPTOR:
                        performDescriptorValueRequest(request.requestId, request.service, request.characteristic, request.descriptor);
                        break;
                    case WRITE_CHARACTERISTIC:
                        performCharacteristicWrite(request.requestId, request.service, request.characteristic, request.value);
                        break;
                    case READ_RSSI:
                        performRssiValueRequest(request.requestId);
                        break;
                    case SET_MTU:
                        performRequestMtu(request.requestId, (int) BleUtils.bytesToLong(request.value, false));
                        break;
                }
            }
        });
    }

    private void performRequestMtu(String requestId, int mtu) {
        if (bluetoothAdapter.isEnabled()) {
            currentRequest = new Request(Request.RequestType.SET_MTU, requestId, null, null, null);
            if (bluetoothGatt != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    if (!bluetoothGatt.requestMtu(mtu)) {
                        handleFaildCallback(currentRequest.requestId, currentRequest.type, IRequestCallback.NONE, currentRequest.value, true);
                    }
                } else {
                    handleFaildCallback(currentRequest.requestId, currentRequest.type, IRequestCallback.API_LEVEL_TOO_LOW, currentRequest.value, true);
                }
            } else {
                handleFaildCallback(currentRequest.requestId, currentRequest.type, IRequestCallback.GATT_IS_NULL, currentRequest.value, true);
            }
        }
    }

    private void performRssiValueRequest(String requestId) {
        if (bluetoothAdapter.isEnabled()) {
            currentRequest = new Request(Request.RequestType.READ_RSSI, requestId, null, null, null);
            if (bluetoothGatt != null) {
                if (!bluetoothGatt.readRemoteRssi()) {
                    handleFaildCallback(currentRequest.requestId, currentRequest.type, IRequestCallback.NONE, currentRequest.value, true);
                }
            } else {
                handleFaildCallback(currentRequest.requestId, currentRequest.type, IRequestCallback.GATT_IS_NULL, currentRequest.value, true);
            }
        }
    }

    private void performCharacteristicValueRequest(String requestId, UUID service, UUID characteristic) {
        if (bluetoothAdapter.isEnabled()) {
            currentRequest = new Request(Request.RequestType.READ_CHARACTERISTIC, requestId, service, characteristic, null);
            if (bluetoothGatt != null) {
                BluetoothGattService gattService = bluetoothGatt.getService(service);
                if (gattService != null) {
                    BluetoothGattCharacteristic gattCharacteristic = gattService.getCharacteristic(characteristic);
                    if (gattCharacteristic != null) {
                        if (!bluetoothGatt.readCharacteristic(gattCharacteristic)) {
                            handleFaildCallback(currentRequest.requestId, currentRequest.type, IRequestCallback.NONE, currentRequest.value, true);
                        }
                    } else {
                        handleFaildCallback(currentRequest.requestId, currentRequest.type, IRequestCallback.NULL_CHARACTERISTIC, currentRequest.value, true);
                    }
                } else {
                    handleFaildCallback(currentRequest.requestId, currentRequest.type, IRequestCallback.NULL_SERVICE, currentRequest.value, true);
                }
            } else {
                handleFaildCallback(currentRequest.requestId, currentRequest.type, IRequestCallback.GATT_IS_NULL, currentRequest.value, true);
            }
        }
    }

    private void performCharacteristicWrite(final String requestId, final UUID service, final UUID characteristic, final byte[] value) {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                int packSize = Ble.getInstance().getConfiguration().getPackageSize();
                if (value.length > packSize) {
                    List<byte[]> list = BleUtils.splitPackage(value, packSize);
                    for (byte[] bytes : list) {
                        doWrite(requestId, service, characteristic, bytes);
                    }
                } else {
                    doWrite(requestId, service, characteristic, value);
                }
            }
        }, Ble.getInstance().getConfiguration().getWriteDelayMillis());
    }

    private void doWrite(String requestId, UUID service, UUID characteristic, byte[] value) {
        if (bluetoothAdapter.isEnabled()) {
            currentRequest = new Request(Request.RequestType.WRITE_CHARACTERISTIC, requestId, service, characteristic, null, value);
            currentRequest.waitWriteResult = Ble.getInstance().getConfiguration().isWaitWriteResult();
            if (bluetoothGatt != null) {
                BluetoothGattService gattService = bluetoothGatt.getService(service);
                if (gattService != null) {
                    BluetoothGattCharacteristic gattCharacteristic = gattService.getCharacteristic(characteristic);
                    if (gattCharacteristic != null) {
                        gattCharacteristic.setValue(value);
                        int writeType = Ble.getInstance().getConfiguration().getWriteType();
                        if (writeType == BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT || writeType == BluetoothGattCharacteristic.WRITE_TYPE_SIGNED ||
                                writeType == BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE) {
                            gattCharacteristic.setWriteType(writeType);
                        }
                        if (!bluetoothGatt.writeCharacteristic(gattCharacteristic)) {
                            handleFaildCallback(currentRequest.requestId, currentRequest.type, IRequestCallback.NONE, currentRequest.value, true);
                        } else if (!currentRequest.waitWriteResult) {
                            processNextRequest();
                        }
                    } else {
                        handleFaildCallback(currentRequest.requestId, currentRequest.type, IRequestCallback.NULL_CHARACTERISTIC, currentRequest.value, true);
                    }
                } else {
                    handleFaildCallback(currentRequest.requestId, currentRequest.type, IRequestCallback.NULL_SERVICE, currentRequest.value, true);
                }
            } else {
                handleFaildCallback(currentRequest.requestId, currentRequest.type, IRequestCallback.GATT_IS_NULL, currentRequest.value, true);
            }
        }
    }

    private void performDescriptorValueRequest(String requestId, UUID service, UUID characteristic, UUID descriptor) {
        if (bluetoothAdapter.isEnabled()) {
            currentRequest = new Request(Request.RequestType.READ_DESCRIPTOR, requestId, service, characteristic, descriptor);
            if (bluetoothGatt != null) {
                BluetoothGattService gattService = bluetoothGatt.getService(service);
                if (gattService != null) {
                    BluetoothGattCharacteristic gattCharacteristic = gattService.getCharacteristic(characteristic);
                    if (gattCharacteristic != null) {
                        BluetoothGattDescriptor gattDescriptor = gattCharacteristic.getDescriptor(descriptor);
                        if (gattDescriptor != null) {
                            if (!bluetoothGatt.readDescriptor(gattDescriptor)) {
                                handleFaildCallback(currentRequest.requestId, currentRequest.type, IRequestCallback.NONE, currentRequest.value, true);
                            }
                        } else {
                            handleFaildCallback(currentRequest.requestId, currentRequest.type, IRequestCallback.NULL_DESCRIPTOR, currentRequest.value, true);
                        }
                    } else {
                        handleFaildCallback(currentRequest.requestId, currentRequest.type, IRequestCallback.NULL_CHARACTERISTIC, currentRequest.value, true);
                    }
                } else {
                    handleFaildCallback(currentRequest.requestId, currentRequest.type, IRequestCallback.NULL_SERVICE, currentRequest.value, true);
                }
            } else {
                handleFaildCallback(currentRequest.requestId, currentRequest.type, IRequestCallback.GATT_IS_NULL, currentRequest.value, true);
            }
        }
    }

    private void performIndicationRequest(String requestId, UUID service, UUID characteristic, byte[] value) {
        if (bluetoothAdapter.isEnabled()) {
            currentRequest = new Request(Request.RequestType.CHARACTERISTIC_INDICATION, requestId, service, characteristic, null, value);
            if (bluetoothGatt != null) {
                BluetoothGattService gattService = bluetoothGatt.getService(service);
                if (gattService != null) {
                    pendingCharacteristic = gattService.getCharacteristic(characteristic);
                    if (pendingCharacteristic != null) {
                        BluetoothGattDescriptor gattDescriptor = pendingCharacteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
                        if (gattDescriptor == null || !bluetoothGatt.readDescriptor(gattDescriptor)) {
                            handleFaildCallback(currentRequest.requestId, currentRequest.type, IRequestCallback.NONE, currentRequest.value, true);
                        }
                    } else {
                        handleFaildCallback(currentRequest.requestId, currentRequest.type, IRequestCallback.NULL_CHARACTERISTIC, currentRequest.value, true);
                    }
                } else {
                    handleFaildCallback(currentRequest.requestId, currentRequest.type, IRequestCallback.NULL_SERVICE, currentRequest.value, true);
                }
            } else {
                handleFaildCallback(currentRequest.requestId, currentRequest.type, IRequestCallback.GATT_IS_NULL, currentRequest.value, true);
            }
        }
    }

    private void performNotificationRequest(String requestId, UUID service, UUID characteristic, byte[] value) {
        if (bluetoothAdapter.isEnabled()) {
            currentRequest = new Request(Request.RequestType.CHARACTERISTIC_NOTIFICATION, requestId, service, characteristic, null, value);
            if (bluetoothGatt != null) {
                BluetoothGattService gattService = bluetoothGatt.getService(service);
                if (gattService != null) {
                    pendingCharacteristic = gattService.getCharacteristic(characteristic);
                    if (pendingCharacteristic != null) {
                        BluetoothGattDescriptor gattDescriptor = pendingCharacteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
                        if (gattDescriptor == null || !bluetoothGatt.readDescriptor(gattDescriptor)) {
                            handleFaildCallback(currentRequest.requestId, currentRequest.type, IRequestCallback.NONE, currentRequest.value, true);
                        }
                    } else {
                        handleFaildCallback(currentRequest.requestId, currentRequest.type, IRequestCallback.NULL_CHARACTERISTIC, currentRequest.value, true);
                    }
                } else {
                    handleFaildCallback(currentRequest.requestId, currentRequest.type, IRequestCallback.NULL_SERVICE, currentRequest.value, true);
                }
            } else {
                handleFaildCallback(currentRequest.requestId, currentRequest.type, IRequestCallback.GATT_IS_NULL, currentRequest.value, true);
            }
        }
    }


    private boolean enableNotification(boolean enable, BluetoothGattCharacteristic characteristic) {
        if (!bluetoothAdapter.isEnabled() || bluetoothGatt == null || !bluetoothGatt.setCharacteristicNotification(characteristic, enable)) {
            return false;
        }
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
        if (descriptor == null) {
            return false;
        }
        if (enable) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        } else {
            descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        }
        return bluetoothGatt.writeDescriptor(descriptor);
    }

    private boolean enableIndication(boolean enable, BluetoothGattCharacteristic characteristic) {
        if (!bluetoothAdapter.isEnabled() || bluetoothGatt == null || !bluetoothGatt.setCharacteristicNotification(characteristic, enable)) {
            return false;
        }
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
        if (descriptor == null) {
            return false;
        }
        if (enable) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
        }
        else {
            descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        }
        return bluetoothGatt.writeDescriptor(descriptor);
    }
}