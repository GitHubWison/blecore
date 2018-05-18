package cn.zfs.blelib.data;

/**
 * 描述:
 * 时间: 2018/5/18 15:30
 * 作者: zengfansheng
 */
public enum ThreadMode {
    /** post到主线程(UI线程)，直接调用 */
    MAIN,
    /** post到主线程(UI线程)，添加到队列后，有序调用 */
    MAIN_ORDERED,
    /** 发到子线程调用 */
    BACKGROUND, 
    /** 和调用Observable.post事件的同一线程 */
    POSTING
}
