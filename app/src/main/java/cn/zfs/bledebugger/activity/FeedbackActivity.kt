package cn.zfs.bledebugger.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.ImageView
import cn.zfs.bledebugger.R
import cn.zfs.bledebugger.entity.MailInfo
import cn.zfs.bledebugger.util.sendFileMail
import cn.zfs.bledebugger.util.sendTextMail
import cn.zfs.bledebugger.view.LoadDialog
import cn.zfs.common.utils.ImageUtils
import cn.zfs.common.utils.ToastUtils
import cn.zfs.common.utils.UiUtils
import kotlinx.android.synthetic.main.activity_feedback.*
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.thread

/**
 * 描述: 反馈和建议
 * 时间: 2018/6/13 22:12
 * 作者: zengfansheng
 */
class FeedbackActivity : BaseActivity() {
    private val paths = ArrayList<String>()
    private var mailInfo = MailInfo()
    private var loadDialog: LoadDialog? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "建议和反馈"
        setContentView(R.layout.activity_feedback)
        initMainInfo()
        initViews()
    }

    private fun initViews() {
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.itemAnimator = DefaultItemAnimator()
        recyclerView.adapter = PicAdapter()
        loadDialog = LoadDialog(this)
        loadDialog!!.setText("正在提交...")
        loadDialog!!.setCancelable(false)
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
            val content = etContent.text.toString()
            if (!content.isEmpty()) {
                loadDialog!!.show()
                mailInfo.content = "反馈来源：BLE Debugger<br>反馈人邮箱：${etMail.text}<br>反馈内容：$content"
                thread {
                    val pics = ArrayList<File>()
                    paths.forEach { 
                        //压缩图片
                        pics.add(compress(it, 100 * 1024))
                    }
                    if (if (paths.isEmpty()) sendTextMail(mailInfo) else sendFileMail(mailInfo, pics)) {
                        ToastUtils.showShort("提交成功")
                        finish()
                    } else {
                        runOnUiThread { 
                            loadDialog!!.dismiss()                        
                        }
                        ToastUtils.showShort("提交失败")
                    }
                }
            } else {
                ToastUtils.showShort("请输入问题或建议")
            }
        }
        return super.onOptionsItemSelected(item)
    }
    
    private class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val iv: ImageView = itemView.findViewById(R.id.iv)
        val ivDel: ImageView = itemView.findViewById(R.id.ivDel)
    }
    
    private inner class PicAdapter : RecyclerView.Adapter<MyViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            val holder = MyViewHolder(LayoutInflater.from(this@FeedbackActivity).inflate(R.layout.item_recyclerview_pic, parent, false))
            holder.ivDel.setOnClickListener { 
                paths.removeAt(it.tag as Int)
                notifyDataSetChanged()
            }
            holder.iv.setOnClickListener { 
                if ((holder.ivDel.tag as Int) == paths.size) {
                    if (paths.size >= 5) {
                        ToastUtils.showShort("最多只能5张")
                    } else {
                        val openAlbumIntent = Intent(Intent.ACTION_PICK)
                        openAlbumIntent.type = "image/*"
                        startActivityForResult(openAlbumIntent, REQUEST_SELECT_FROM_ALBUM)
                    }
                }
            }
            return holder            
        }

        override fun getItemCount() = paths.size + 1

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            holder.ivDel.visibility = if (position == paths.size) View.INVISIBLE else View.VISIBLE
            holder.ivDel.tag = position
            if (position < paths.size) {
                val params = holder.iv.layoutParams
                val bitmap = ImageUtils.getBitmap(paths[position], params.width, params.height)
                holder.iv.setImageBitmap(bitmap)
            } else {
                holder.iv.setImageResource(R.drawable.add)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_SELECT_FROM_ALBUM) {
            val path = ImageUtils.getImagePath(this, Objects.requireNonNull(data?.data))
            if (path != null) {
                paths.add(path)
                recyclerView.adapter.notifyDataSetChanged()
            }
        }
    }
    
    private fun compress(imgPath: String, limit: Long): File {
        //缩放后压缩图片
        var quality = 100
        val resolution = UiUtils.getRealScreenResolution(this)
        val bitmap = ImageUtils.getBitmap(imgPath, resolution[0], resolution[1])
        val file = File(cacheDir, UUID.randomUUID().toString() + ".jpg")
        ImageUtils.saveBitmapToFile(bitmap, file, quality)
        while (file.length() > limit) {
            quality -= 10
            ImageUtils.saveBitmapToFile(bitmap, file, quality)
        }
        return file
    }
    
    companion object {
        private const val REQUEST_SELECT_FROM_ALBUM = 10
    }
}