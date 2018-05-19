package cn.zfs.blelib.data;

import android.support.annotation.NonNull;

import cn.zfs.blelib.core.Request;

/**
 * 描述: 请求事件
 * 时间: 2018/5/18 10:37
 * 作者: zengfansheng
 */
public class RequestEvent<D extends Device> extends Event<D> {
    @NonNull
    public String requestId = "";    
    @NonNull
    public Request.RequestType requestType = Request.RequestType.WRITE_CHARACTERISTIC;    
    /** 请求时带的数据 */
    public byte[] src;    

    public RequestEvent() {}

    public RequestEvent(int eventType, @NonNull String requestId, @NonNull Request.RequestType requestType) {
        super(eventType);
        this.requestId = requestId;
        this.requestType = requestType;
    }

    public RequestEvent(int eventType, @NonNull String requestId, @NonNull Request.RequestType requestType, byte[] src) {
        super(eventType);
        this.requestId = requestId;
        this.requestType = requestType;
        this.src = src;
    }

    public RequestEvent(int eventType, D device, @NonNull String requestId, @NonNull Request.RequestType requestType) {
        super(eventType, device);
        this.requestId = requestId;
        this.requestType = requestType;
    }
    
    public RequestEvent(int eventType, D device, @NonNull String requestId, @NonNull Request.RequestType requestType, byte[] src) {
        super(eventType, device);
        this.requestId = requestId;
        this.requestType = requestType;
        this.src = src;
    }
}
