package cn.zfs.blelib.data;

import android.support.annotation.NonNull;

import cn.zfs.blelib.core.Request;

/**
 * 描述: 带请求的结果
 * 时间: 2018/5/19 00:16
 * 作者: zengfansheng
 */
public class RequestSingleValueEvent<T, D extends Device> extends RequestEvent<D> {
    /** 请求的结果数据 */
    public T result;

    public RequestSingleValueEvent() {
    }

    public RequestSingleValueEvent(int eventType, D device, @NonNull String requestId, @NonNull Request.RequestType requestType, T result) {
        super(eventType, device, requestId, requestType);
        this.result = result;
    }

    public RequestSingleValueEvent(int eventType, D device, @NonNull String requestId, @NonNull Request.RequestType requestType, byte[] src, T result) {
        super(eventType, device, requestId, requestType, src);
        this.result = result;
    }
}
