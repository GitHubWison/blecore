package cn.zfs.blelib.data;

import android.support.annotation.NonNull;

/**
 * 描述: 请求事件
 * 时间: 2018/5/18 10:37
 * 作者: zengfansheng
 */
public class RequestEvent extends Event {
    protected String requestId;

    public RequestEvent() {}

    public RequestEvent(int type, Device device, @NonNull String requestId) {
        super(type, device);
        this.requestId = requestId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(@NonNull String requestId) {
        this.requestId = requestId;
    }
}
