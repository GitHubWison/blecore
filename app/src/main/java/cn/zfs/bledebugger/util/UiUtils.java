package cn.zfs.bledebugger.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Typeface;
import android.os.Process;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ImageView;
import android.widget.TextView;

import cn.zfs.bledebugger.MyApp;


/**
 * Created by Zeng on 2015/7/13.
 * 其中很多方法都依赖于自定义Application类
 */
public class UiUtils {
    public static Context getContext() {
        return MyApp.getInst();
    }
    
    public static Resources getResources() {
        return MyApp.getInst().getResources();
    }
    
    /**
	 * 获取实际屏幕宽高，API >= 17。
	 * @return int[0]:宽度，int[1]:高度。
	 */
	public static int[] getRealScreenResolution(Activity activity) {
		DisplayMetrics metrics = new DisplayMetrics();
		activity.getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
		return new int[]{metrics.widthPixels, metrics.heightPixels};
	}

	/**
	 * 获取显示屏幕宽度，不包含状态栏和导航栏
	 */
	public static int getDisplayScreenWidth() {
		return getResources().getDisplayMetrics().widthPixels;
	}

	/**
	 * 获取显示屏幕高度
	 */
	public static int getDisplayScreenHeight() {
		return getResources().getDisplayMetrics().heightPixels;
	}
    
    /** 
     * 根据手机的分辨率从 dip 的单位 转成为 px(像素) 
     */  
    public static int dip2px(float dpValue) {  
        final float scale = getResources().getDisplayMetrics().density;  
        return (int) (dpValue * scale + 0.5f);  
    }  
  
    /** 
     * 根据手机的分辨率从 px(像素) 的单位 转成为 dp 
     */  
    public static int px2dip(float pxValue) {  
        final float scale = getResources().getDisplayMetrics().density;  
        return (int) (pxValue / scale + 0.5f);  
    } 
    
	/**
	 * 将任务放到主线程执行。无需关心当前在什么线程，直接调用
	 */
	public static void runOnUiThread(Runnable runnable) {
		if (isMainThread()) {
		    runnable.run();
		} else {
		    MyApp.getHandler().post(runnable);
		}
	}

	/**
	 * 延时执行Runnable任务
	 */
	public static boolean postDelayed(Runnable task, long delayMillis) {
		return MyApp.getHandler().postDelayed(task, delayMillis);
	}

	/**
	 * 停止Runnable任务
	 */
	public static void cancel(Runnable task) {
		MyApp.getHandler().removeCallbacks(task);
	}

	/**
	 * 判断当前是否是主线程
	 */
	public static boolean isMainThread() {
		return Process.myTid() == MyApp.getMainTid();
	}
    
    /**
     * Color转换为字符串
     */
    public static String toHexWithoutAlpha(int color) {
        StringBuilder sb = new StringBuilder(Integer.toHexString(color & 0x00FFFFFF));        
        while (sb.length() < 6) {
            sb.insert(0, "0");
        }
        sb.insert(0, "#");
        return sb.toString();
    }
    	
	/**
	 * 将自己从容器中移除
	 */
	public static void removeFromContainer(View view) {
		ViewParent parent = view.getParent();
		if (parent instanceof ViewGroup) {
			((ViewGroup) parent).removeView(view);
		}
	}

	/**
	 * 获取状态栏高度
	 */
	public static int getStatusBarHeight() {
		int result = 0;
		int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
		if (resourceId > 0) {
			result = getResources().getDimensionPixelSize(resourceId);
		}
		return result;
	}

	/**
	 * 获取ActionBar的高度
	 */
	public static float getActionBarSize(Context context) {
		TypedArray ta = context.obtainStyledAttributes(new int[]{android.R.attr.actionBarSize});
		float height = ta.getDimension(0, 0);
		ta.recycle();
		return height;
	}
    
    public static void sendBroadcast(Intent intent) {
        MyApp.getInst().sendBroadcast(intent);
    }

    /**
     * 设置TextView的字体，字体为外部文件，目录在assets
     * @param context 上下文
     * @param root TextView所在的根布局
     * @param fontName 字体名
     */
    public static void applyFont(Context context, View root, String fontName) {
        try {
            if (root instanceof ViewGroup) {
                ViewGroup viewGroup = (ViewGroup) root;
                for (int i = 0; i < viewGroup.getChildCount(); i++)
                    applyFont(context, viewGroup.getChildAt(i), fontName);
            } else if (root instanceof TextView) {
				((TextView) root).setTypeface(Typeface.createFromAsset(context.getAssets(), fontName));
			}
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

	/**
	 * 设置TextView的字体
	 * @param root TextView所在的根布局
	 * @param tf 字体
	 */
	public static void applyFont(View root, Typeface tf) {
		try {
			if (root instanceof ViewGroup) {
				ViewGroup viewGroup = (ViewGroup) root;
				for (int i = 0; i < viewGroup.getChildCount(); i++)
					applyFont(viewGroup.getChildAt(i), tf);
			} else if (root instanceof TextView) {
				((TextView) root).setTypeface(tf);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

    /**
     * 获取字体
     * @param path 字体在assets的路径
     */
    public static Typeface getTypeface(String path) {
        return Typeface.createFromAsset(MyApp.getInst().getAssets(), path);
    }

	/**
	 * 转黑白
	 */
	public static void colourToMonochrome(ImageView iv) {
		ColorMatrix matrix = new ColorMatrix();
		matrix.setSaturation(0);
		ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
		iv.setColorFilter(filter);
	}
	
	/**将View的高度设置成状态栏高*/
	public static void setToStatusBarHeight(View view) {
		ViewGroup.LayoutParams params = view.getLayoutParams();
		params.height = getStatusBarHeight();
		view.setLayoutParams(params);
	}

    /**
     * 取过渡色
     * @param offset 取值范围：0 ~ 1
     */
    public static int getColor(int startColor, int endColor, float offset) {
        int Aa = startColor >> 24 & 0xff;
        int Ra = startColor >> 16 & 0xff;
        int Ga = startColor >> 8 & 0xff;
        int Ba = startColor & 0xff;
        int Ab = endColor >> 24 & 0xff;
        int Rb = endColor >> 16 & 0xff;
        int Gb = endColor >> 8 & 0xff;
        int Bb = endColor & 0xff;
        int a = (int) (Aa + (Ab - Aa) * offset);
        int r = (int) (Ra + (Rb - Ra) * offset);
        int g = (int) (Ga + (Gb - Ga) * offset);
        int b = (int) (Ba + (Bb - Ba) * offset);
        return Color.argb(a, r, g, b);
    }

	/**
     * @param normal 正常时的颜色
     * @param pressed 按压时的颜色
     * @param disable 不可用时的颜色
     */
    public static ColorStateList createColorStateList(int normal, int pressed, int disable) {
        //normal一定要最后
        int[][] states = new int[][]{
                {-android.R.attr.state_enabled}, 
                {android.R.attr.state_pressed, android.R.attr.state_enabled}, 
                {}
        };
        return new ColorStateList(states, new int[]{disable, pressed, normal});
    }
}
