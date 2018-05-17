package cn.zfs.blelib.data;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import java.util.Vector;

import cn.zfs.blelib.core.Ble;

/**
 * 描述: 蓝牙设备状态、数据被观察者
 * 时间: 2018/4/17 17:02
 * 作者: zengfansheng
 */
public class BleObservable {
    private Vector<IBleObserver> obs;
    private Looper backgroundLooper;
    protected Handler handler;

    public BleObservable(Looper looper) {
        backgroundLooper = looper;
        obs = new Vector<>();        
        handler = new Handler(looper);
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

	private synchronized void checkIfRightThread() {
        if (Ble.getInstance().getConfig().isPostObserverMsgMainThread()) {
            if (handler.getLooper() != Looper.getMainLooper()) {
                handler = new Handler(Looper.getMainLooper());
            }
        } else {
            if (handler.getLooper() == Looper.getMainLooper()) {
                handler = new Handler(backgroundLooper);
            }
        }
    }
	
    public void notifyBluetoothStateChange(final int state) {
        checkIfRightThread();
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
        checkIfRightThread();
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
        checkIfRightThread();
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
        checkIfRightThread();
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
        checkIfRightThread();
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
        checkIfRightThread();
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
