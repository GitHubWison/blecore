package cn.zfs.blelib.event;

import android.support.annotation.NonNull;

import cn.zfs.blelib.callback.IRequestCallback;
import cn.zfs.blelib.core.Request;

/**
 * 描述: 请求失败事件，如读特征值、写特征值、开启notification等等
 * 时间: 2018/5/18 19:05
 * 作者: zengfansheng
 */
public class RequestFailedEvent extends RequestIdEvent {

    @NonNull
    public Request.RequestType requestType;
    /** 请求时带的数据 */
    public byte[] src;
    
    /**
     * {@link IRequestCallback#NONE}<br>{@link IRequestCallback#NULL_CHARACTERISTIC}<br>{@link IRequestCallback#NULL_DESCRIPTOR},
     * <br>{@link IRequestCallback#NULL_SERVICE}<br>{@link IRequestCallback#GATT_STATUS_REQUEST_NOT_SUPPORTED}
     * <br>{@link IRequestCallback#GATT_IS_NULL}<br>{@link IRequestCallback#API_LEVEL_TOO_LOW}
     */
    public int failType;   

    public RequestFailedEvent(@NonNull String requestId, @NonNull Request.RequestType requestType, int failType, byte[] src) {
        super(requestId);
        this.requestId = requestId;
        this.requestType = requestType;
        this.failType = failType;
        this.src = src;
    }
}
