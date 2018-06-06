package cn.zfs.bledebugger;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;

import java.util.List;

/**
 * Created by Zeng on 2015/7/13.
 */
public class App extends Application {
	private static App inst;
	private Handler handler;
	private int mainId;

	@Override
	public void onCreate() {
		super.onCreate();
		inst = this;
		handler = new Handler();
		mainId = android.os.Process.myTid();
	}

	public static App getInst() {
        return inst;
    }

    public static Handler getHandler() {
        return inst.handler;
    }

    public static int getMainTid() {
        return inst.mainId;
    }
	
	public static PackageInfo getPackageInfo() {
        PackageManager pm = inst.getPackageManager();
        try {
            return pm.getPackageInfo(inst.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
	
	public static String getAppVersionName() {
        PackageInfo info = getPackageInfo();
        return info == null ? null : info.versionName;
    }
	
	public static int getAppVersionCode() {
		PackageInfo info = getPackageInfo();
		return info == null ? 0 : info.versionCode;
	}

	/** 程序是否在前台运行 */
	public static boolean isAppOnForeground() {
		ActivityManager activityManager = (ActivityManager) inst.getSystemService(Context.ACTIVITY_SERVICE);
		String packageName = inst.getPackageName();
		List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
		if (appProcesses == null) return false;
		for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
			if (appProcess.processName.equals(packageName) && appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
				return true;
			}
		}
		return false;
	}
}
