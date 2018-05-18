package cn.zfs.blelib.data;

import android.support.annotation.NonNull;

import cn.zfs.blelib.core.Request;

/**
 * 描述: 请求的结果是整型
 * 时间: 2018/5/19 00:16
 * 作者: zengfansheng
 */
public class RequestIntEvent extends RequestEvent {
    /** 请求的结果数据 */
    public int result;

    public RequestIntEvent() {
    }

    public RequestIntEvent(int eventType, Device device, @NonNull String requestId, @NonNull Request.RequestType requestType, int result) {
        super(eventType, device, requestId, requestType);
        this.result = result;
    }

    public RequestIntEvent(int eventType, Device device, @NonNull String requestId, @NonNull Request.RequestType requestType, byte[] src, int result) {
        super(eventType, device, requestId, requestType, src);
        this.result = result;
    }
}
