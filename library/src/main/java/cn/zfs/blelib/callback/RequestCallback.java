package cn.zfs.blelib.callback;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.support.annotation.NonNull;
import android.util.Log;

import cn.zfs.blelib.core.Ble;
import cn.zfs.blelib.core.Request;
import cn.zfs.blelib.data.Device;
import cn.zfs.blelib.data.EventType;
import cn.zfs.blelib.data.Observable;
import cn.zfs.blelib.data.RequestByteArrayEvent;
import cn.zfs.blelib.data.RequestEvent;
import cn.zfs.blelib.data.RequestFailedEvent;
import cn.zfs.blelib.data.RequestIntEvent;
import cn.zfs.blelib.data.SingleByteArrayEvent;
import cn.zfs.blelib.util.BleUtils;

/**
 * 描述: 接收蓝牙数据消息
 * 时间: 2018/4/23 11:17
 * 作者: zengfansheng
 */
public class RequestCallback implements IRequestCallback {    
    protected Device device;

    public RequestCallback(@NonNull Device device) {
        this.device = device;
    }

    public Device getDevice() {
        return device;
    }

    @Override
    public void onDestroy() {
        
    }

    @Override
    public void onCharacteristicRead(@NonNull String requestId, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        Observable.getInstance().post(new RequestByteArrayEvent(EventType.ON_CHARACTERISTIC_READ, device, requestId, 
                Request.RequestType.READ_CHARACTERISTIC, characteristic.getValue()));
        Ble.println(RequestCallback.class, Log.DEBUG, "onCharacteristicRead！请求ID：" + requestId +
                ", value: " + BleUtils.bytesToHexString(characteristic.getValue()) + ", mac: " + device.addr);
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        Observable.getInstance().post(new SingleByteArrayEvent(EventType.ON_CHARACTERISTIC_CHANGED, device, characteristic.getValue()));
    }

    @Override
    public void onRssiRead(@NonNull String requestId, BluetoothGatt gatt, int rssi) {
        Observable.getInstance().post(new RequestIntEvent(EventType.ON_READ_REMOTE_RSSI, device, requestId,
                Request.RequestType.READ_RSSI, rssi));
        Ble.println(RequestCallback.class, Log.DEBUG, "读到信号强度！rssi: "+ rssi + ", mac: " + device.addr);
    }

    @Override
    public void onMtuChanged(@NonNull String requestId, BluetoothGatt gatt, int mtu) {
        Observable.getInstance().post(new RequestIntEvent(EventType.ON_MTU_CHANGED, device, requestId,
                Request.RequestType.SET_MTU, mtu));
        Ble.println(RequestCallback.class, Log.DEBUG, "Mtu修改成功！mtu: "+ mtu + ", mac: " + device.addr);
    }

    @Override
    public void onRequestFialed(@NonNull String requestId, @NonNull Request.RequestType requestType, int failType, byte[] value) {
        Observable.getInstance().post(new RequestFailedEvent(EventType.ON_BLE_REQUEST_FIALED, device, requestId, requestType, value, failType));
        Ble.println(RequestCallback.class, Log.ERROR, "请求失败！请求ID：" + requestId +
                ", failType: " + failType + ", mac: " + device.addr);
    }

    @Override
    public void onDescriptorRead(@NonNull String requestId, BluetoothGatt gatt, BluetoothGattDescriptor descriptor) {
        Observable.getInstance().post(new RequestByteArrayEvent(EventType.ON_DESCRIPTOR_READ, device, requestId, 
                Request.RequestType.READ_DESCRIPTOR, descriptor.getValue()));
        Ble.println(RequestCallback.class, Log.DEBUG, "onDescriptorRead！请求ID：" + requestId +
                ", value: " + BleUtils.bytesToHexString(descriptor.getValue()) + ", mac: " + device.addr);
    }

    @Override
    public void onNotificationRegistered(@NonNull String requestId, BluetoothGatt gatt, BluetoothGattDescriptor descriptor) {
        Observable.getInstance().post(new RequestEvent(EventType.ON_NOTIFICATION_REGISTERED, device, requestId, 
                Request.RequestType.CHARACTERISTIC_NOTIFICATION));
        Ble.println(RequestCallback.class, Log.DEBUG, "NOTIFICATION_REGISTERED！请求ID：" + requestId + ", mac: " + device.addr);
    }

    @Override
    public void onNotificationUnregistered(@NonNull String requestId, BluetoothGatt gatt, BluetoothGattDescriptor descriptor) {
        Observable.getInstance().post(new RequestEvent(EventType.ON_NOTIFICATION_UNREGISTERED, device, requestId,
                Request.RequestType.CHARACTERISTIC_NOTIFICATION));
        Ble.println(RequestCallback.class, Log.DEBUG, "NOTIFICATION_UNREGISTERED！请求ID：" + requestId + ", mac: " + device.addr);
    }

    @Override
    public void onIndicationRegistered(@NonNull String requestId, BluetoothGatt gatt, BluetoothGattDescriptor descriptor) {
        Observable.getInstance().post(new RequestEvent(EventType.ON_INDICATION_REGISTERED, device, requestId,
                Request.RequestType.CHARACTERISTIC_INDICATION));
        Ble.println(RequestCallback.class, Log.DEBUG, "INDICATION_REGISTERED！请求ID：" + requestId + ", mac: " + device.addr);
    }

    @Override
    public void onIndicationUnregistered(@NonNull String requestId, BluetoothGatt gatt, BluetoothGattDescriptor descriptor) {
        Observable.getInstance().post(new RequestEvent(EventType.ON_INDICATION_UNREGISTERED, device, requestId,
                Request.RequestType.CHARACTERISTIC_INDICATION));
        Ble.println(RequestCallback.class, Log.DEBUG, "INDICATION_UNREGISTERED！请求ID：" + requestId + ", mac: " + device.addr);
    }

    @Override
    public void onCharacteristicWrite(@NonNull String requestId, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        Observable.getInstance().post(new RequestEvent(EventType.ON_WRITE_CHARACTERISTIC, device, requestId, 
                Request.RequestType.WRITE_CHARACTERISTIC, characteristic.getValue()));
        Ble.println(RequestCallback.class, Log.DEBUG, "写入成功！value: "+ BleUtils.bytesToHexString(characteristic.getValue()) +
                ", 请求ID：" + requestId + ", mac: " + device.addr);
    }
}
