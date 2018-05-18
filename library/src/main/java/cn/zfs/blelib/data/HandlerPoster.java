package cn.zfs.blelib.data;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;

import cn.zfs.blelib.exception.BleException;

/**
 * 描述:
 * 时间: 2018/5/18 16:25
 * 作者: zengfansheng
 */
class HandlerPoster extends Handler implements Poster {

    private final PendingPostQueue queue;
    private final int maxMillisInsideHandleMessage;
    private final Observable observable;
    private boolean handlerActive;

    protected HandlerPoster(Observable observable, Looper looper, int maxMillisInsideHandleMessage) {
        super(looper);
        this.observable = observable;
        this.maxMillisInsideHandleMessage = maxMillisInsideHandleMessage;
        queue = new PendingPostQueue();
    }

    public void enqueue(Observation observation, Object event) {
        PendingPost pendingPost = PendingPost.obtainPendingPost(observation, event);
        synchronized (this) {
            queue.enqueue(pendingPost);
            if (!handlerActive) {
                handlerActive = true;
                if (!sendMessage(obtainMessage())) {
                    throw new BleException("Could not send handler message");
                }
            }
        }
    }

    @Override
    public void handleMessage(Message msg) {
        boolean rescheduled = false;
        try {
            long started = SystemClock.uptimeMillis();
            while (true) {
                PendingPost pendingPost = queue.poll();
                if (pendingPost == null) {
                    synchronized (this) {
                        // Check again, this time in synchronized
                        pendingPost = queue.poll();
                        if (pendingPost == null) {
                            handlerActive = false;
                            return;
                        }
                    }
                }
                observable.invokeObserver(pendingPost);
                long timeInMethod = SystemClock.uptimeMillis() - started;
                if (timeInMethod >= maxMillisInsideHandleMessage) {
                    if (!sendMessage(obtainMessage())) {
                        throw new BleException("Could not send handler message");
                    }
                    rescheduled = true;
                    return;
                }
            }
        } finally {
            handlerActive = rescheduled;
        }
    }
}
