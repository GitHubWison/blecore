package cn.zfs.bledebugger.entity

import java.util.*

/**
 * 描述:
 * 时间: 2018/6/13 13:30
 * 作者: zengfansheng
 */
data class BroadcastItem(var len: Int, var type: Byte, var value: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BroadcastItem) return false

        if (len != other.len) return false
        if (type != other.type) return false
        if (!Arrays.equals(value, other.value)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = len
        result = 31 * result + type
        result = 31 * result + Arrays.hashCode(value)
        return result
    }
}