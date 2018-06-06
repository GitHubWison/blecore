package cn.zfs.bledebugger.entity;

import android.content.Context;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by zeng on 2016/8/24.
 * 任意时长Toast
 */
public class AnyDurationToast {
    private Timer loopTimer;
    private Timer taskTimer;
    private Toast toast;
        
    public AnyDurationToast(Context context) {
        toast = Toast.makeText(context, "", Toast.LENGTH_SHORT);
    }

    private void cancelTask() {
        if (loopTimer != null) {
            loopTimer.cancel();
			loopTimer = null;
        }
        if (taskTimer != null) {
            taskTimer.cancel();
			taskTimer = null;
        }
    }

    public void show(int duration) {
        cancelTask();
        loopTimer = new Timer();
        loopTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                toast.show();
            }
        }, 0, Toast.LENGTH_LONG);
        taskTimer = new Timer();
        taskTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                cancel();
				loopTimer.cancel();
				taskTimer = null;                
				loopTimer = null;
            }
        }, duration);
    }
    
    public void showShort() {
        cancelTask();
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.show();
    }
    
    public void showLong() {
        cancelTask();
        toast.setDuration(Toast.LENGTH_LONG);
        toast.show();
    }

    public AnyDurationToast setText(CharSequence text) {
        toast.setText(text);
        return this;
    }
    
    public AnyDurationToast setText(int resid) {
        toast.setText(resid);
        return this;
    }
}
