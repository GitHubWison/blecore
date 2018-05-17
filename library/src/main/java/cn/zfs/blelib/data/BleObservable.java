package cn.zfs.blelib.data;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import java.util.Vector;

/**
 * 描述: 蓝牙设备状态、数据被观察者
 * 时间: 2018/4/17 17:02
 * 作者: zengfansheng
 */
public class BleObservable {
    private Vector<IBleObserver> obs;
    protected Handler handler;

    public BleObservable() {
        obs = new Vector<>();
        handler = new Handler(Looper.getMainLooper());
    }

    public synchronized void addObserver(IBleObserver o) {
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

	public synchronized void removeObserver(IBleObserver o) {
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

    public void notifyBluetoothStateChange(final int state) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                for (Object o : getObservers()) {
                    ((IBleObserver) o).onBluetoothStateChange(state);
                }
            }
        });
    }
		
	public void notifyConnectionStateChange(final @NonNull Device device, final int state) {
		handler.post(new Runnable() {
            @Override
            public void run() {
                for (Object o : getObservers()) {
                    ((IBleObserver) o).onConnectionStateChange(device, state);
                }
            }
        });
	}
	
	public void nofityUnableConnect(final Device device, final String error) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                for (Object o : getObservers()) {
                    ((IBleObserver) o).onUnableConnect(device, error);
                }
            }
        });        
    }

	public void notifyConnectTimeout(final @NonNull Device device, final int type) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                for (Object o : getObservers()) {
                    ((IBleObserver) o).onConnectTimeout(device, type);
                }
            }
        });		
	}
    
    public void notifyRssiRead(final @NonNull Device device, final int rssi) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                for (Object o : getObservers()) {
                    ((IBleObserver) o).onRssiRead(device, rssi);
                }
            }
        });        
    }
    
    public void notifyWriteCharacteristicResult(final @NonNull Device device, final String requestId, 
                                                final boolean result, final byte[] value) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                for (Object o : getObservers()) {
                    ((IBleObserver) o).onWriteCharacteristicResult(device, requestId, result, value);
                }
            }
        });        
    }
}
