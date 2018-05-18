package cn.zfs.blelib.data;

/**
 * 描述:
 * 时间: 2018/5/18 18:03
 * 作者: zengfansheng
 */
class BackgroundPoster implements Runnable, Poster {

    private final PendingPostQueue queue;
    private final Observable observable;

    private volatile boolean executorRunning;

    BackgroundPoster(Observable observable) {
        this.observable = observable;
        queue = new PendingPostQueue();
    }

    public void enqueue(Observation subscription, Object event) {
        PendingPost pendingPost = PendingPost.obtainPendingPost(subscription, event);
        synchronized (this) {
            queue.enqueue(pendingPost);
            if (!executorRunning) {
                executorRunning = true;
                observable.getExecutorService().execute(this);
            }
        }
    }

    @Override
    public void run() {
        try {
            try {
                while (true) {
                    PendingPost pendingPost = queue.poll(1000);
                    if (pendingPost == null) {
                        synchronized (this) {
                            pendingPost = queue.poll();
                            if (pendingPost == null) {
                                executorRunning = false;
                                return;
                            }
                        }
                    }
                    observable.invokeObserver(pendingPost);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } finally {
            executorRunning = false;
        }
    }
}