package cn.zfs.bledebuger.entity

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import cn.zfs.treeadapter.Node

/**
 * 描述:
 * 时间: 2018/4/27 10:56
 * 作者: zengfansheng
 */
class Item(id: Int, pId: Int, level: Int, isExpand: Boolean, var isService: Boolean,
           var service: BluetoothGattService?, var characteristic: BluetoothGattCharacteristic?) : Node<Item>(id, pId, level, isExpand) {
    var hasNotifyProperty = false
    var hasWriteProperty = false
}