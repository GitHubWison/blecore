package cn.zfs.blelib.data;

import android.support.annotation.NonNull;

import cn.zfs.blelib.core.Request;

/**
 * 描述: 请求的结果是字节数组形式
 * 时间: 2018/5/19 00:13
 * 作者: zengfansheng
 */
public class RequestByteArrayEvent extends RequestEvent {
    /** 请求的结果数据 */
    public byte[] result;

    public RequestByteArrayEvent() {
    }

    public RequestByteArrayEvent(int eventType, Device device, @NonNull String requestId, @NonNull Request.RequestType requestType, byte[] result) {
        super(eventType, device, requestId, requestType);
        this.result = result;
    }

    public RequestByteArrayEvent(int eventType, Device device, @NonNull String requestId, @NonNull Request.RequestType requestType, byte[] src, byte[] result) {
        super(eventType, device, requestId, requestType, src);
        this.result = result;
    }
}
