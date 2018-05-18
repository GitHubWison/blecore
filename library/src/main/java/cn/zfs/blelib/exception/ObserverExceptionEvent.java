package cn.zfs.blelib.exception;

import cn.zfs.blelib.data.Observable;

/**
 * 描述:
 * 时间: 2018/5/18 15:40
 * 作者: zengfansheng
 */
public class ObserverExceptionEvent {
    public final Observable observable;
    /** 观察者抛的 */
    public final Throwable throwable;

    /** 无法送达的事件 */
    public final Object causingEvent;

    /** 抛异常的观察者 */
    public final Object causingObserver;

    public ObserverExceptionEvent(Observable observable, Throwable throwable, Object causingEvent, Object causingObserver) {
        this.observable = observable;
        this.throwable = throwable;
        this.causingEvent = causingEvent;
        this.causingObserver = causingObserver;
    }
}
