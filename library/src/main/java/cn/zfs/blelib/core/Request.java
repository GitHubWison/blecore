package cn.zfs.blelib.core;

import android.support.annotation.NonNull;

import java.util.Queue;
import java.util.UUID;

/**
 * 描述: 用作请求队列
 * 时间: 2018/4/11 15:15
 * 作者: zengfansheng
 */
public class Request {
    
    public enum RequestType {
        CHARACTERISTIC_NOTIFICATION, CHARACTERISTIC_INDICATION, READ_CHARACTERISTIC, READ_DESCRIPTOR, READ_RSSI, WRITE_CHARACTERISTIC, WRITE_DESCRIPTOR, CHANGE_MTU
    }

    public RequestType type;
    public UUID service;
    public UUID characteristic;
    public UUID descriptor;
    public String requestId;
    public byte[] value;
    boolean waitWriteResult;
    //-----分包发送时用到-----
    Queue<byte[]> remainQueue;
    byte[] writeOverValue;
    int writeDelay;
    //----------------------

    public Request(@NonNull RequestType type, @NonNull String requestId, UUID service, UUID characteristic, UUID descriptor) {
        this.type = type;
        this.requestId = requestId;
        this.service = service;
        this.characteristic = characteristic;
        this.descriptor = descriptor;
        this.value = null;
    }

    public Request(@NonNull RequestType type, @NonNull String requestId, UUID service, UUID characteristic, UUID descriptor, byte[] value) {
        this.type = type;
        this.requestId = requestId;
        this.service = service;
        this.characteristic = characteristic;
        this.descriptor = descriptor;
        this.value = value;
    }
}
