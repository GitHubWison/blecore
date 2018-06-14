package cn.zfs.bledebugger.view

import android.app.Activity
import cn.zfs.bledebugger.R
import cn.zfs.common.utils.UiUtils
import cn.zfs.common.view.BaseDialog

/**
 * 描述:
 * 时间: 2018/6/14 12:49
 * 作者: zengfansheng
 */
class LoadDialog(activity: Activity) : BaseDialog(activity, R.layout.dialog_load) {
    init {
        setSize(UiUtils.dip2px(150f), UiUtils.dip2px(150f))
    }
}