package cn.zfs.blelib.data;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.zfs.blelib.core.Ble;

/**
 * 描述: 蓝牙设备状态、数据被观察者
 * 时间: 2018/4/17 17:02
 * 作者: zengfansheng
 */
public class BleObservable {
    private Vector<IBleObserver> obs;
    protected Handler handler;
    protected ExecutorService threadPool = Executors.newCachedThreadPool();

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
	
    protected void execute(Runnable task) {
        //如果设置post到UI线程则使用handler执行post，否则使用线程池执行
        if (Ble.getInstance().getConfig().isPostObserverMsgMainThread()) {
            handler.post(task);
        } else {
            threadPool.execute(task);
        }
    }
    
    public void notifyBluetoothStateChange(final int state) {
        execute(new Runnable() {
            @Override
            public void run() {
                for (Object o : getObservers()) {
                    ((IBleObserver) o).onBluetoothStateChange(state);
                }
            }
        });
    }
		
	public void notifyConnectionStateChange(final @NonNull Device device, final int state) {
        execute(new Runnable() {
            @Override
            public void run() {
                for (Object o : getObservers()) {
                    ((IBleObserver) o).onConnectionStateChange(device, state);
                }
            }
        });
	}
	
	public void nofityUnableConnect(final Device device, final String error) {
        execute(new Runnable() {
            @Override
            public void run() {
                for (Object o : getObservers()) {
                    ((IBleObserver) o).onUnableConnect(device, error);
                }
            }
        });        
    }

	public void notifyConnectTimeout(final @NonNull Device device, final int type) {
        execute(new Runnable() {
            @Override
            public void run() {
                for (Object o : getObservers()) {
                    ((IBleObserver) o).onConnectTimeout(device, type);
                }
            }
        });		
	}
    
    public void notifyRssiRead(final @NonNull Device device, final int rssi) {
        execute(new Runnable() {
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
        execute(new Runnable() {
            @Override
            public void run() {
                for (Object o : getObservers()) {
                    ((IBleObserver) o).onWriteCharacteristicResult(device, requestId, result, value);
                }
            }
        });        
    }
}
