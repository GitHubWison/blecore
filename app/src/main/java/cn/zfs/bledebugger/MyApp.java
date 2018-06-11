package cn.zfs.bledebugger;

import cn.zfs.common.base.App;

/**
 * Created by Zeng on 2015/7/13.
 */
public class MyApp extends App {

	public static MyApp getInst() {
        return (MyApp) App.getInst();
    }
}
