package cn.zfs.bledebugger.view

import android.app.Activity
import android.widget.TextView
import cn.zfs.bledebugger.R
import cn.zfs.common.utils.UiUtils
import cn.zfs.common.view.BaseDialog
import com.wang.avi.AVLoadingIndicatorView

/**
 * 描述: 加载对话框
 * 时间: 2018/5/11 14:48
 * 作者: zengfansheng
 */
class LoadDialog(activity: Activity) : BaseDialog(activity, R.layout.dialog_load) {
    private val tvMsg: TextView = contentView.findViewById(R.id.tvMsg)

    init {
        setSize(UiUtils.dip2px(160f), UiUtils.dip2px(160f))
        contentView.setBackgroundResource(R.drawable.white_round_bg)
        val indicator: AVLoadingIndicatorView = contentView.findViewById(R.id.indicator)
        indicator.setIndicatorColor(0xFF588DFF.toInt())
        indicator.show()
        setCanceledOnTouchOutside(false)
    }

    fun setText(resId: Int) {
        tvMsg.setText(resId)
    }

    fun setText(text: CharSequence) {
        tvMsg.text = text
    }
}
