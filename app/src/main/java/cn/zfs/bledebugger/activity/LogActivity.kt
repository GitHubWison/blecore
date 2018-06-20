package cn.zfs.bledebugger.activity

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.Menu
import android.view.MenuItem
import cn.zfs.bledebugger.Consts
import cn.zfs.bledebugger.R
import cn.zfs.bledebugger.view.LoadDialog
import cn.zfs.common.utils.FileUtils
import cn.zfs.common.utils.ToastUtils
import cn.zfs.fileselector.FileSelector
import kotlinx.android.synthetic.main.activity_log.*
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import kotlin.concurrent.thread

/**
 * 描述: 日志
 * 时间: 2018/6/20 13:56
 * 作者: zengfansheng
 */
class LogActivity : BaseActivity() {
    private var fileSelector = FileSelector()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val path = intent.getStringExtra(Consts.EXTRA_LOG_PATH)
        title = "${FileUtils.getFileNameWithoutSuffix(path)}日志"
        setContentView(R.layout.activity_log)
        val loadDialog = LoadDialog(this)
        loadDialog.setText("加载中...")
        loadDialog.show()
        thread { 
            val sb = StringBuilder()
            try {
                val reader = BufferedReader(FileReader(path))
                var line = reader.readLine()
                while (line != null) {
                    sb.appendln(line)
                    line = reader.readLine()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            runOnUiThread {
                loadDialog.dismiss()
                tvLogs.text = sb.toString()
            }
        }
        fileSelector.setRoot(Environment.getExternalStorageDirectory())
        fileSelector.setFilenameFilter { _, name -> !name.startsWith(".") }
        fileSelector.setTitle("选择导出目录")
        fileSelector.setOnFileSelectListener { 
            loadDialog.setText("保存中...")
            loadDialog.show()
            thread {
                val src = File(path)
                val target = File(it[0], src.name)
                FileUtils.copy(src, target)
                runOnUiThread { 
                    loadDialog.dismiss()
                    ToastUtils.showShort("导出成功")
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.log, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when {
            item?.itemId == R.id.menuExport -> {
                fileSelector.select(this)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        fileSelector.onActivityResult(requestCode, resultCode, data)
    }
}