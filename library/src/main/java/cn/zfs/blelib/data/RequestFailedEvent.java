package cn.zfs.blelib.data;

import android.support.annotation.NonNull;

import cn.zfs.blelib.callback.IRequestCallback;
import cn.zfs.blelib.core.Request;

/**
 * 描述: 请求失败事件
 * 时间: 2018/5/18 19:05
 * 作者: zengfansheng
 */
public class RequestFailedEvent extends RequestEvent {
    
    /**
     * {@link IRequestCallback#NONE}<br>{@link IRequestCallback#NULL_CHARACTERISTIC}<br>{@link IRequestCallback#NULL_DESCRIPTOR},
     * <br>{@link IRequestCallback#NULL_SERVICE}<br>{@link IRequestCallback#GATT_STATUS_REQUEST_NOT_SUPPORTED}
     * <br>{@link IRequestCallback#GATT_IS_NULL}<br>{@link IRequestCallback#API_LEVEL_TOO_LOW}
     */
    public int failType;    

    public RequestFailedEvent() {
    }

    public RequestFailedEvent(int eventType, Device device, @NonNull String requestId, @NonNull Request.RequestType requestType, byte[] src, int failType) {
        super(eventType, device, requestId, requestType, src);
        this.failType = failType;
    }
}
