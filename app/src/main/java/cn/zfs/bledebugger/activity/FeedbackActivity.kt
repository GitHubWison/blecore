package cn.zfs.bledebugger.activity

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import cn.zfs.bledebugger.R
import cn.zfs.bledebugger.entity.MailInfo
import cn.zfs.bledebugger.util.sendFileMail
import cn.zfs.bledebugger.util.sendTextMail
import cn.zfs.common.utils.ToastUtils
import kotlinx.android.synthetic.main.activity_feedback.*
import java.io.File
import kotlin.concurrent.thread

/**
 * 描述: 反馈和建议
 * 时间: 2018/6/13 22:12
 * 作者: zengfansheng
 */
class FeedbackActivity : BaseActivity() {
    private val files = ArrayList<File>()
    private var mailInfo = MailInfo()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "建议和反馈"
        setContentView(R.layout.activity_feedback)
        initMainInfo()
    }

    private fun initMainInfo() {
        mailInfo.userName = "bingmo977"
        mailInfo.password = "b000000"
        mailInfo.fromAddress = "bingmo977@163.com"
        mailInfo.toAddress = "bingmo977@163.com"
        mailInfo.mailServerHost = "smtp.163.com"
        mailInfo.subject = "建议反馈"        
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.feedback, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == R.id.menuSubmit) {
            mailInfo.content = etContent.text.toString()
            thread {
                if (if (files.isEmpty()) sendTextMail(mailInfo) else sendFileMail(mailInfo, files)) {
                    ToastUtils.showShort("提交成功")
                } else {
                    ToastUtils.showShort("提交失败")
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }
}