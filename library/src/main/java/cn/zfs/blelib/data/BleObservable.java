package cn.zfs.blelib.data;

import android.support.annotation.NonNull;

import java.util.Vector;

/**
 * 描述: 蓝牙设备状态、数据被观察者
 * 时间: 2018/4/17 17:02
 * 作者: zengfansheng
 */
public class BleObservable {
    private Vector<BleObserver> obs;

    public BleObservable() {
        obs = new Vector<>();
    }

    public synchronized void addObserver(BleObserver o) {
		if (o == null) {
			return;
		}
		synchronized (this) {
			if (!obs.contains(o)) {
                obs.addElement(o);
            }
		}
	}

	public synchronized int countObservers() {
		return obs.size();
	}

	public synchronized void removeObserver(BleObserver o) {
		obs.removeElement(o);
	}
	
	protected Object[] getObservers() {
        Object[] arrLocal;
        synchronized (this) {
            arrLocal = obs.toArray();
        }
		return arrLocal;
	}

	public synchronized void clearObservers() {
		obs.removeAllElements();
	}
		
	public void notifyConnectionStateChange(@NonNull Device device, int state) {
		for (Object o : getObservers()) {
            ((BleObserver) o).onConnectionStateChange(device, state);
		}
	}
	
	public void nofityUnableConnect(Device device, String error) {
        for (Object o : getObservers()) {
            ((BleObserver) o).onUnableConnect(device, error);
        }
    }

	public void notifyConnectTimeout(@NonNull Device device, int type) {
		for (Object o : getObservers()) {
            ((BleObserver) o).onConnectTimeout(device, type);
		}
	}
    
    public void notifyRssiRead(@NonNull Device device, int rssi) {
        for (Object o : getObservers()) {
            ((BleObserver) o).onRssiRead(device, rssi);
        }
    }
    
    public void notifyWriteCharacteristicResult(@NonNull Device device, String requestId, boolean result, byte[] value) {
        for (Object o : getObservers()) {
            ((BleObserver) o).onWriteCharacteristicResult(device, requestId, result, value);
        }
    }
}
