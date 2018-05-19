package cn.zfs.blelib.event;

import cn.zfs.blelib.core.Device;

/**
 * 描述: 连接创建失败
 * 时间: 2018/5/19 19:35
 * 作者: zengfansheng
 */
public class ConnectionCreateFailedEvent<D extends Device> {
    /** 设备 */
    public D device;
    /** 失败详情 */
    public String error;

    public ConnectionCreateFailedEvent(D device, String error) {
        this.device = device;
        this.error = error;
    }
}
