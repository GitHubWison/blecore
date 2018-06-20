package cn.zfs.bledebugger;

import java.io.File;

import cn.zfs.common.base.App;

/**
 * Created by Zeng on 2015/7/13.
 */
public class MyApp extends App {
    private String logDirPath;

	public static MyApp getInst() {
        return (MyApp) App.getInst();
    }
    
    /**
     * 日志目录
     */
    public static String getLogDirPath() {
	    if (getInst().logDirPath == null) {
            File file = new File(getInst().getFilesDir(), "logs");
            if (!file.exists()) {
                file.mkdir();
            }
            getInst().logDirPath = file.getAbsolutePath();
	    }
	    return getInst().logDirPath;
    }
}
