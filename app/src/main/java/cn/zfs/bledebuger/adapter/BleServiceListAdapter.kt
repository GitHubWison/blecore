package cn.zfs.bledebuger.adapter

import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import cn.zfs.bledebuger.R
import cn.zfs.treeadapter.TreeAdapter
import cn.zfs.bledebuger.entity.Item


/**
 * 描述:
 * 时间: 2018/4/27 11:04
 * 作者: zengfansheng
 */
class BleServiceListAdapter(context: Context, lv: ListView, nodes: MutableList<Item>) : TreeAdapter<Item>(lv, nodes) {
    val serviceName = "Unknown Service"
    val characteristicName = "Unknown Characteristic"
    var context: Context? = null
    
    init {
        this.context = context
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
        when (getItemViewType(position)) {
            1 -> return object : TreeAdapter.Holder<Item>() {
                private var iv: ImageView? = null
                private var tvName: TextView? = null
                private var tvUuid: TextView? = null

                override fun setData(node: Item) {
                    iv!!.visibility = if (node.hasChild()) View.VISIBLE else View.INVISIBLE
                    iv!!.setBackgroundResource(if (node.isExpand) R.drawable.expand else R.drawable.fold)
                    tvName!!.text = serviceName
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
                private var tvName: TextView? = null
                private var tvUuid: TextView? = null
                private var tvProperty: TextView? = null

                override fun setData(node: Item) {
                    tvName!!.text = characteristicName
                    tvUuid!!.text = node.characteristic!!.uuid.toString()
                    tvProperty!!.text = getPropertiesString(node)
                }

                override fun createConvertView(): View {
                    val view = View.inflate(context!!, R.layout.item_characteristic, null)
                    tvName = view.findViewById(R.id.tvName)
                    tvUuid = view.findViewById(R.id.tvUuid)
                    tvProperty = view.findViewById(R.id.tvProperty)
                    return view
                }
            }
        }
    }
    
    fun getPropertiesString(node: Item): String {
        val sb = StringBuilder()
        if (node.characteristic!!.properties and BluetoothGattCharacteristic.PROPERTY_READ != 0) {
            
        }
        
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
                if (property == BluetoothGattCharacteristic.PROPERTY_NOTIFY) {
                    node.hasNotifyProperty = true
                }
                if (property == BluetoothGattCharacteristic.PROPERTY_WRITE || property == BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) {
                    node.hasWriteProperty = true
                }
            }
        }
        return sb.toString()
    }
}