package cn.zfs.blelib.data;

/**
 * 描述: 事件
 * 时间: 2018/5/18 09:02
 * 作者: zengfansheng
 */
public class Event {
    /**
     * 事件类型<br>
     * {@link EventType#ON_BLUETOOTH_STATE_CHANGED}<br>
     * {@link EventType#ON_CONNECTION_STATE_CHANGED}...
     */
    public int eventType;
    /** 设备 */
    public Device device;

    public Event() {
    }

    public Event(Device device) {
        this.device = device;
    }

    public Event(int eventType, Device device) {
        this.eventType = eventType;
        this.device = device;
    }
}
