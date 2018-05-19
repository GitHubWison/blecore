package cn.zfs.blelib.event;

import android.support.annotation.NonNull;

/**
 * 描述: 带请求ID的事件
 * 时间: 2018/5/19 19:06
 * 作者: zengfansheng
 */
public class RequestIdEvent {
    @NonNull
    public String requestId;

    public RequestIdEvent(@NonNull String requestId) {
        this.requestId = requestId;
    }
}
