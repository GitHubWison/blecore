package cn.zfs.bledebugger.adapter

import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.text.TextUtils
import android.view.View
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import cn.zfs.bledebugger.R
import cn.zfs.bledebugger.entity.Item
import cn.zfs.bledebugger.util.UuidLib
import cn.zfs.blelib.util.BleUtils
import cn.zfs.common.utils.UiUtils
import cn.zfs.treeadapter.TreeAdapter
import java.util.*
import kotlin.collections.HashMap


/**
 * 描述:
 * 时间: 2018/4/27 11:04
 * 作者: zengfansheng
 */
class BleServiceListAdapter(context: Context, lv: ListView, nodes: MutableList<Item>) : TreeAdapter<Item>(lv, nodes) {
    private val serviceName = "Unknown Service"
    private val characteristicName = "Unknown Characteristic"
    private var context: Context? = null
    private var showInHex = true
    var itemClickCallback: OnItemClickCallback? = null
    private val holderMap = HashMap<String, ViewHolder>()
    
    init {
        this.context = context
    }

    /**
     * 切换显示方式
     */
    fun setShowInHex(hex: Boolean) {
        showInHex = hex
        notifyDataSetChanged()
    }
    
    fun updateValue(service: UUID, characteristic: UUID, value: ByteArray) {
        val holder = holderMap[service.toString() + characteristic.toString()]
        if (holder != null) {
            if (holder.layoutValue!!.visibility != View.VISIBLE) {
                val params = holder.rootView!!.layoutParams
                params.height = UiUtils.dip2px(95f)
                holder.layoutValue!!.visibility = View.VISIBLE
                holder.rootView!!.layoutParams = params
            }
            val valueStr = if (showInHex) BleUtils.bytesToHexString(value) else String(value)
            holder.tvValue!!.text = valueStr
        }
    }
    
    private inner class ViewHolder {
        var rootView: View? = null
        var tvValue: TextView? = null
        var layoutValue: View? = null
        
        fun setViews(rootView: View, layoutValue: View, tvValue: TextView) {
            this.rootView = rootView
            this.layoutValue = layoutValue
            this.tvValue = tvValue
        }
    }
    
    override fun getViewTypeCount(): Int {
        return super.getViewTypeCount() + 1
    }

    /**
     * 获取当前位置的条目类型
     */
    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).isService) 1 else 0
    }
    
    override fun getHolder(position: Int): Holder<Item> {
        //根据位置返回不同布局
        when (getItemViewType(position)) {
            1 -> return object : TreeAdapter.Holder<Item>() {
                private var iv: ImageView? = null
                private var tvName: TextView? = null
                private var tvUuid: TextView? = null

                override fun setData(node: Item, position: Int) {
                    iv!!.visibility = if (node.hasChild()) View.VISIBLE else View.INVISIBLE
                    iv!!.setBackgroundResource(if (node.isExpand) R.drawable.expand else R.drawable.fold)
                    val name = UuidLib.getServiceName(node.service!!.uuid)
                    tvName!!.text = if (TextUtils.isEmpty(name)) serviceName else name
                    tvUuid!!.text = node.service!!.uuid.toString()
                }

                override fun createConvertView(): View {
                    val view = View.inflate(context!!, R.layout.item_service, null)
                    iv = view.findViewById(R.id.ivIcon)
                    tvName = view.findViewById(R.id.tvName)
                    tvUuid = view.findViewById(R.id.tvUuid)
                    return view
                }
            }
            else -> return object : TreeAdapter.Holder<Item>() {
                private var rootView: View? = null
                private var tvName: TextView? = null
                private var tvUuid: TextView? = null
                private var tvProperty: TextView? = null
                private var tvValue: TextView? = null
                private var layoutValue: View? = null
                private var btnRead: ImageView? = null
                private var btnSend: ImageView? = null
                private var btnStartNoti: ImageView? = null
                private var btnStopNoti: ImageView? = null

                override fun setData(node: Item, position: Int) {
                    holderMap[node.service!!.uuid.toString() + node.characteristic!!.uuid.toString()]?.setViews(rootView!!, layoutValue!!, tvValue!!)
                    val name = UuidLib.getCharacteristicName(node.characteristic!!.uuid)
                    tvName?.text = if (TextUtils.isEmpty(name)) characteristicName else name
                    tvUuid?.text = node.characteristic!!.uuid.toString()
                    val propertiesString = getPropertiesString(node)//获取权限列表
                    tvProperty?.text = propertiesString
                    //读取到值，改变布局
                    val params = rootView?.layoutParams
                    if (node.value != null) {
                        params?.height = UiUtils.dip2px(95f)
                        layoutValue?.visibility = View.VISIBLE
                        val value = if (showInHex) BleUtils.bytesToHexString(node.value) else String(node.value!!)
                        tvValue?.text = value
                    } else {
                        params?.height = UiUtils.dip2px(80f)
                        tvValue?.text = ""
                        layoutValue?.visibility = View.GONE
                    }
                    rootView?.layoutParams = params
                    btnSend?.visibility = if (node.hasWriteProperty) View.VISIBLE else View.GONE
                    btnRead?.visibility = if (node.hasReadProperty) View.VISIBLE else View.GONE
                    btnStartNoti?.visibility = if (node.hasNotifyProperty && !node.notification) View.VISIBLE else View.GONE
                    btnStopNoti?.visibility = if (node.hasNotifyProperty && node.notification) View.VISIBLE else View.GONE
                    tvName?.tag = node
                }

                override fun createConvertView(): View {
                    val view = View.inflate(context!!, R.layout.item_characteristic, null)
                    rootView = view.findViewById(R.id.root)
                    tvName = view.findViewById(R.id.tvName)
                    tvUuid = view.findViewById(R.id.tvUuid)
                    tvProperty = view.findViewById(R.id.tvProperty)
                    tvValue = view.findViewById(R.id.tvValue)
                    layoutValue = view.findViewById(R.id.layoutValue)
                    btnRead = view.findViewById(R.id.btnRead)
                    btnSend = view.findViewById(R.id.btnSend)
                    btnStartNoti = view.findViewById(R.id.btnStartNoti)
                    btnStopNoti = view.findViewById(R.id.btnStopNoti)
                    val clickListener = View.OnClickListener {
                        val type = when (it.id) {
                            R.id.btnRead -> READ
                            R.id.btnSend -> SEND
                            R.id.btnStartNoti -> START_NOTI
                            else -> STOP_NOTI
                        }
                        itemClickCallback?.onItemClick(type, tvName?.tag as Item)
                    }
                    btnRead?.setOnClickListener(clickListener)
                    btnSend?.setOnClickListener(clickListener)
                    btnStartNoti?.setOnClickListener(clickListener)
                    btnStopNoti?.setOnClickListener(clickListener)
                    val item = getItem(position)
                    holderMap[item.service!!.uuid.toString() + item.characteristic!!.uuid.toString()] = ViewHolder()
                    return view
                }
            }
        }
    }
    
    fun getPropertiesString(node: Item): String {
        val sb = StringBuilder()
        val properties = arrayOf(BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PROPERTY_INDICATE, 
                BluetoothGattCharacteristic.PROPERTY_NOTIFY, BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE, BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)
        val propertyStrs = arrayOf("WRITE", "INDICATE", "NOTIFY", "READ", "SIGNED_WRITE", "WRITE_NO_RESPONSE")
        properties.forEachIndexed { index, property ->
            if (node.characteristic!!.properties and property != 0) {
                if (!sb.isEmpty()) {
                    sb.append(", ")
                }
                sb.append(propertyStrs[index])
                if (property == BluetoothGattCharacteristic.PROPERTY_NOTIFY || property == BluetoothGattCharacteristic.PROPERTY_INDICATE) {
                    node.hasNotifyProperty = true
                }
                if (property == BluetoothGattCharacteristic.PROPERTY_WRITE || property == BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) {
                    node.hasWriteProperty = true
                }
                if (property == BluetoothGattCharacteristic.PROPERTY_READ) {
                    node.hasReadProperty = true
                }
            }
        }        
        return sb.toString()
    }
    
    interface OnItemClickCallback {
        fun onItemClick(type: Int, node: Item)
    }
    
    companion object {
        const val READ = 0
        const val SEND = 1
        const val START_NOTI = 2
        const val STOP_NOTI = 3
    }
}