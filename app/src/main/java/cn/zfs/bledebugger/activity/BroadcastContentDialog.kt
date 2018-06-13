package cn.zfs.bledebugger.activity

import android.app.Activity
import android.content.Context
import android.view.View
import android.widget.ListView
import android.widget.TextView
import cn.zfs.bledebugger.R
import cn.zfs.bledebugger.entity.BroadcastItem
import cn.zfs.common.base.BaseHolder
import cn.zfs.common.base.BaseListAdapter
import cn.zfs.common.utils.StringUtils
import cn.zfs.common.utils.UiUtils
import cn.zfs.common.view.BaseDialog
import java.nio.ByteBuffer

/**
 * 描述:
 * 时间: 2018/6/13 12:38
 * 作者: zengfansheng
 */
class BroadcastContentDialog(activity: Activity) : BaseDialog(activity, R.layout.dialog_broadcast_content) {
    private val dataList = ArrayList<BroadcastItem>()
    private val adapter = ListAdapter(activity, dataList)    
    
    init {
        setSize((UiUtils.getDisplayScreenWidth() * 0.9).toInt(), -2)
        findViewById(R.id.tvOk).setOnClickListener { dismiss() }
        val lv = findViewById(R.id.lv) as ListView
        lv.adapter = adapter
    }

    fun setData(scanRecord: ByteArray) {
        dataList.clear()
        //解析广播
        val byteBuffer = ByteBuffer.wrap(scanRecord)
        while (byteBuffer.remaining() != 0) {
            val len = byteBuffer.get().toInt() and 0xFF
            if (byteBuffer.remaining() == 0)
                break
            val type = byteBuffer.get()
            if (byteBuffer.remaining() == 0)
                break
            if (len < 2)
                break
            val value = ByteArray(len - 1)
            byteBuffer.get(value)
            dataList.add(BroadcastItem(len, type, value))
        }
        adapter.notifyDataSetChanged()
    }
    
    private inner class ListAdapter(context: Context, data: MutableList<BroadcastItem>) : BaseListAdapter<BroadcastItem>(context, data) {
        override fun getHolder(position: Int): BaseHolder<BroadcastItem> {
            return object : BaseHolder<BroadcastItem>() {
                private var tvLen: TextView? = null
                private var tvType: TextView? = null
                private var tvValue: TextView? = null
                
                override fun createConvertView(): View {
                    val view = View.inflate(context, R.layout.item_broadcast_content, null)
                    tvLen = view.findViewById(R.id.tvLen)
                    tvType = view.findViewById(R.id.tvType)
                    tvValue = view.findViewById(R.id.tvValue)
                    return view
                }

                override fun setData(data: BroadcastItem, position: Int) {
                    tvLen?.text = "${data.len}"
                    val typeText = "0x${StringUtils.bytesToHexString(byteArrayOf(data.type), "")}"
                    tvType?.text = typeText
                    val value = "0x${StringUtils.bytesToHexString(data.value, "")}"
                    tvValue?.text = value
                }
            }
        }
    }
}