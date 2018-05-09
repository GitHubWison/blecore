package cn.zfs.bledebuger.util;


import cn.zfs.bledebuger.App;
import cn.zfs.bledebuger.entity.AnyDurationToast;

/**
 * 时间: 2017/10/10 15:10
 * 作者: 曾繁盛
 * 邮箱: 43068145@qq.com
 * 功能: 单例Toast工具类
 */

public class ToastUtils {
    private static AnyDurationToast toast;
    
    private static void show(final Object obj, final int duration) {
        App.getHandler().post(new Runnable() {
            @Override
            public void run() {
                if (toast == null) {
                    toast = new AnyDurationToast(App.getInst());
                }
                if (obj instanceof CharSequence) {
                    toast.setText((CharSequence) obj);
                } else if (obj instanceof Integer) {
                    toast.setText((int) obj);
                } else {
                    return;
                }
                if (duration == -1) {
                    toast.showShort();
                } else if (duration == -2) {
                    toast.showLong();
                } else {
                    toast.show(duration);
                }
            }
        });        
    }

    /**
     * 显示一个短时Toast
     */
    public static void showShort(CharSequence text) {
        show(text, -1);
    }

    /**
     * 显示一个短时Toast
     */
    public static void showShort(int resId) {
        show(resId, -1);
    }

    /**
     * 显示一个长时Toast
     */
    public static void showLong(CharSequence text) {
        show(text, -2);
    }

    /**
     * 显示一个长时Toast
     */
    public static void showLong(int resId) {
        show(resId, -2);
    }

    /**
     * 显示一个任意时长Toast
     */
    public static void showAnyDuration(CharSequence text, int duration) {
        show(text, duration);
    }

    /**
     * 显示一个任意时长Toast
     */
    public static void showAnyDuration(int resId, int duration) {
        show(resId, duration);
    }
}
