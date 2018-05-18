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
    private Request.RequestType requestType;
    /**
     * {@link IRequestCallback#NONE}<br>{@link IRequestCallback#NULL_CHARACTERISTIC}<br>{@link IRequestCallback#NULL_DESCRIPTOR},
     * <br>{@link IRequestCallback#NULL_SERVICE}<br>{@link IRequestCallback#GATT_STATUS_REQUEST_NOT_SUPPORTED}
     * <br>{@link IRequestCallback#GATT_IS_NULL}<br>{@link IRequestCallback#API_LEVEL_TOO_LOW}
     */
    public int failType;
    /** 请求时带的数据 */
    public byte[] value;

    public RequestFailedEvent() {
    }

    public RequestFailedEvent(int type, @NonNull Request.RequestType requestType, int failType, byte[] value) {
        this.requestType = requestType;
        this.failType = failType;
        this.value = value;
    }

    public RequestFailedEvent(int type, Device device, @NonNull String requestId, @NonNull Request.RequestType requestType, int failType, byte[] value) {
        super(type, device, requestId);
        this.requestType = requestType;
        this.failType = failType;
        this.value = value;
    }

    public Request.RequestType getRequestType() {
        return requestType;
    }

    public void setRequestType(@NonNull Request.RequestType requestType) {
        this.requestType = requestType;
    }
}
