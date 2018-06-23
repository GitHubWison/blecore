package cn.zfs.bledebugger.entity

import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import kotlin.concurrent.thread

/**
 * 描述:
 * 时间: 2018/6/20 13:36
 * 作者: zengfansheng
 */
class LogSaver(dir: String) {
    private val file = File(dir, SimpleDateFormat("yyyyMMdd").format(System.currentTimeMillis()) + ".txt")
    private val outputStream = FileOutputStream(file, true)
    private val cache = StringBuilder()
        
    fun write(log: String) {
        val text = "${SimpleDateFormat("HH:mm:ss.SSS").format(System.currentTimeMillis())}> $log\n"
        cache.append(text)
        if (cache.length >= 2 * 1024 * 1024) {
            flush()
        }
    }
    
    fun flush() {
        val cacheStr = cache.toString()
        cache.setLength(0)//清空
        thread {
            try {
                outputStream.write(cacheStr.toByteArray())
                outputStream.flush()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun close() {
        try {
            if (!cache.isEmpty()) {
                outputStream.write(cache.toString().toByteArray())
            }
            outputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}