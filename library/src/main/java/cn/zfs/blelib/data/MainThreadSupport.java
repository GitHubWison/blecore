package cn.zfs.blelib.data;

import android.os.Looper;

/**
 * 描述:
 * 时间: 2018/5/18 16:23
 * 作者: zengfansheng
 */
interface MainThreadSupport {
    boolean isMainThread();

    Poster createPoster(Observable observable);

    class AndroidHandlerMainThreadSupport implements MainThreadSupport {

        private final Looper looper;

        AndroidHandlerMainThreadSupport(Looper looper) {
            this.looper = looper;
        }

        @Override
        public boolean isMainThread() {
            return looper == Looper.myLooper();
        }

        @Override
        public Poster createPoster(Observable observable) {
            return new HandlerPoster(observable, looper, 10);
        }
    }
}
