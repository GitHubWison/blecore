package cn.zfs.blelib.util;

import android.support.annotation.NonNull;
import android.util.Log;

/**
 * 描述:
 * 时间: 2018/5/17 11:45
 * 作者: zengfansheng
 */
public class LogController {
    public static final int NONE = 1;
    public static final int VERBOSE = NONE << 1;
    public static final int INFO = VERBOSE << 1;
    public static final int DEBUG = INFO << 1;
    public static final int WARN = DEBUG << 1;
    public static final int ERROR = WARN << 1;
    public static final int ALL = VERBOSE|INFO|DEBUG|WARN|ERROR;

    /**
     * 控制输出级别<br>{@link #NONE}, {@link #VERBOSE}, {@link #DEBUG}, {@link #INFO}, {@link #WARN}, {@link #ERROR}
     */
    public static int printLevelControl = NONE;

    public static boolean accept(int priority) {
        int level;
        switch(priority) {
            case Log.ERROR:
                level = ERROR;
        		break;
            case Log.WARN:
                level = WARN;
                break;
            case Log.INFO:
                level = INFO;
                break;
            case Log.DEBUG:
                level = DEBUG;
                break;
            case Log.VERBOSE:
                level = VERBOSE;
                break;
            default:
                level = NONE;
                break;
        }
        return (printLevelControl & NONE) != NONE && (printLevelControl & level) == level;
    }
    
    public interface Filter {
        boolean accept(@NonNull String log);
    }
}
