package cn.zfs.bledebugger.activity

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.ParcelUuid
import android.support.v4.content.ContextCompat
import android.text.Editable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.view.Menu
import android.view.MenuItem
import android.widget.ScrollView
import cn.zfs.bledebugger.Consts
import cn.zfs.bledebugger.R
import cn.zfs.blelib.core.Ble
import cn.zfs.blelib.core.Connection
import cn.zfs.blelib.core.Device
import cn.zfs.blelib.core.Request
import cn.zfs.blelib.event.Events
import cn.zfs.blelib.util.BleUtils
import cn.zfs.common.utils.PreferencesUtils
import cn.zfs.common.utils.ToastUtils
import kotlinx.android.synthetic.main.activity_comm.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.text.SimpleDateFormat
import kotlin.concurrent.thread

/**
 * 描述:
 * 时间: 2018/4/16 16:28
 * 作者: zengfansheng
 */
class CommActivity : BaseActivity() {
    private var device: Device? = null
    private var writeService: ParcelUuid? = null
    private var writeCharacteristic: ParcelUuid? = null
    private var notifyService: ParcelUuid? = null
    private var notifyCharacteristic: ParcelUuid? = null
    private var pause = false
    private var delay = 0L
    private var run = true
    private var lastUpdateTime = 0L
    private var successCount = 0
    private var failCount = 0
    private val recList = ArrayList<String>()
    private var keywords = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comm)
        Ble.getInstance().registerSubscriber(this)
        device = intent.getParcelableExtra(Consts.EXTRA_DEVICE)
        writeService = intent.getParcelableExtra(Consts.EXTRA_WRITE_SERVICE)
        writeCharacteristic = intent.getParcelableExtra(Consts.EXTRA_WRITE_CHARACTERISTIC)
        notifyService = intent.getParcelableExtra(Consts.EXTRA_NOTIFY_SERVICE)
        notifyCharacteristic = intent.getParcelableExtra(Consts.EXTRA_NOTIFY_CHARACTERISTIC)
        if (device == null || writeService == null || writeCharacteristic == null) {
            finish()
            return
        }
        device = Ble.getInstance().getConnection(device!!)?.device
        title = device!!.name
        initEvents()
        clearCount()
        updateState(device!!.connectionState)
        //加载发送记录
        loadRecs()
        updateRecIcon()
    }

    //更新记录选择图标颜色
    private fun updateRecIcon() {
        if (!recList.isEmpty()) {
            ivRec.setColorFilter(ContextCompat.getColor(this, R.color.colorPrimary))
        }
    }
    
    private fun loadRecs() {
        val recs = PreferencesUtils.getString(Consts.SP_KEY_SEND_REC)
        val split = recs?.split(",")
        split?.forEach { recList.add(it) }        
    }
    
    private fun saveRecs() {
        while (recList.size > 10) {//最多保存10条
            recList.removeAt(10)
        }
        var rec = ""
        recList.forEachIndexed { index, s ->
            rec += if (index != 0) ",$s" else s
        }
        if (!rec.isEmpty()) {
            PreferencesUtils.putString(Consts.SP_KEY_SEND_REC, rec)
        }
    }

    private fun initEvents() {
        //设置
        btnClear.setOnClickListener {
            tvLogs.text = ""
        }
        //暂停日志输出
        btnPause.setOnClickListener { 
            pause = !pause
            if (pause) {
                btnPause.setText(R.string.resume)
            } else {
                btnPause.setText(R.string.pause)
            }
        }
        //发送指令
        btnSend.setOnClickListener {
            val s = etValue.text.trim().toString()
            try {
                val arr = s.trim { it <= ' ' }.replace(" +".toRegex(), " ").split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val bytes = ByteArray(arr.size)
                arr.forEachIndexed { i, str ->
                    if (str.length > 2) {
                        throw Exception()
                    }
                    bytes[i] = Integer.valueOf(str, 16).toByte()
                }
                val hexString = BleUtils.bytesToHexString(bytes)
                if (!recList.contains(hexString)) {
                    recList.add(0, hexString)
                    saveRecs()
                }                
                updateRecIcon()
                if (chk.isChecked) {
                    thread {
                        while (run && chk.isChecked) {
                            Ble.getInstance().getConnection(device!!)?.writeCharacteristic("3", writeService!!.uuid, writeCharacteristic!!.uuid, bytes)
                            Thread.sleep(delay)
                            if (System.currentTimeMillis() - lastUpdateTime > 500) {
                                lastUpdateTime = System.currentTimeMillis()
                                updateCount()
                            }
                        }
                        Ble.getInstance().getConnection(device!!)?.clearRequestQueue()
                        updateCount()
                    }
                } else {
                    Ble.getInstance().getConnection(device!!)?.writeCharacteristic("3", writeService!!.uuid, writeCharacteristic!!.uuid, bytes)
                }                                
            } catch (e: Throwable) {
                ToastUtils.showShort(R.string.error_format)
            }
        }
        //发送延时
        etDelay.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                var d = 0L
                val delayStr = etDelay.text.toString()
                if (!delayStr.isEmpty()) {
                    d = delayStr.toLong()
                }
                delay = d
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        //清空发送结果记录
        btnClearCount.setOnClickListener {
            clearCount()
        }
        //发送指令记录
        ivRec.setOnClickListener { 
            if (!recList.isEmpty()) {
                AlertDialog.Builder(this)
                        .setItems(recList.toTypedArray()) { _, which ->
                            etValue.setText(recList[which])
                            etValue.setSelection(recList[which].length)
                        }
                        .show()
            }
        }
        //日志输出控制
        etFilte.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                keywords = etFilte.text.toString()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }
    
    private fun updateCount() {
        runOnUiThread {
            tvSuccessCount.text = getString(R.string.success_pattern, successCount)
            tvFailCount.text = getString(R.string.failed_pattern, failCount)
        }
    }

    private fun clearCount() {
        successCount = 0
        failCount = 0
        tvSuccessCount.text = getString(R.string.success_pattern, successCount)
        tvFailCount.text = getString(R.string.failed_pattern, failCount)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.comm, menu)
        if (device != null && !device!!.isDisconnected) {
            menu?.findItem(R.id.menuDisconnect)?.isVisible = true
            menu?.findItem(R.id.menuConnect)?.isVisible = false
        } else {
            menu?.findItem(R.id.menuDisconnect)?.isVisible = false
            menu?.findItem(R.id.menuConnect)?.isVisible = true
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menuDisconnect -> {//断开
                Ble.getInstance().disconnectConnection(device!!)
            }
            R.id.menuConnect -> {//连接
                Ble.getInstance().connect(this, device!!, true, null)
            }
            R.id.menuRequestMtu -> {//请求修改mtu
                val intent = Intent(this, RequestMtuActivity::class.java)
                intent.putExtra(Consts.EXTRA_DEVICE, device)
                intent.putExtra(Consts.EXTRA_WRITE_SERVICE, ParcelUuid(writeService!!.uuid))
                intent.putExtra(Consts.EXTRA_WRITE_CHARACTERISTIC, ParcelUuid(writeCharacteristic!!.uuid))
                startActivity(intent)
            }
            R.id.menuCopy -> {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                // 将文本内容放到系统剪贴板里
                cm.primaryClip = ClipData.newPlainText(null, tvLogs.text)
                ToastUtils.showShort(R.string.log_has_copy)
            }
        }
        invalidateOptionsMenu()
        return super.onOptionsItemSelected(item)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onConnectionStateChange(e: Events.ConnectionStateChanged) {
        updateState(e.state)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onCharacteristicChanged(e: Events.CharacteristicChanged) {
        if (!pause) {
            if (tvLogs.text.length > 3 * 1024 * 1024) {
                tvLogs.text = ""
            }
            val hexString = BleUtils.bytesToHexString(e.characteristic.value)
            if (keywords.isEmpty() || (chkPrint.isChecked && hexString.contains(keywords.toUpperCase())) || 
                    (!chkPrint.isChecked && !hexString.contains(keywords.toUpperCase()))) {
                val text = "${SimpleDateFormat("mm:ss.SSS").format(System.currentTimeMillis())}> $hexString\n"
                val builder = SpannableStringBuilder(text)
                builder.setSpan(ForegroundColorSpan(0xFF0BBF43.toInt()), 0, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)//设置颜色
                tvLogs.append(builder)
                scrollView.post {
                    scrollView.fullScroll(ScrollView.FOCUS_DOWN)
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onRequestFialed(e: Events.RequestFailed) {
        if (e.requestType == Request.RequestType.WRITE_CHARACTERISTIC) {
            failCount++
            if (!chk.isChecked) {
                updateCount()
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onCharacteristicWrite(e: Events.CharacteristicWrite) {
        successCount++
        if (!chk.isChecked) {            
            updateCount()
        }
    }
    
    private fun updateState(state: Int) {
        val text = when (state) {
            Connection.STATE_CONNECTED -> {
                getString(R.string.connected_not_discover)
            }
            Connection.STATE_CONNECTING -> {
                getString(R.string.connecting)
            }
            Connection.STATE_DISCONNECTED -> {
                getString(R.string.disconnected)
            }
            Connection.STATE_RECONNECTING -> {
                getString(R.string.reconnecting)
            }
            Connection.STATE_SERVICE_DISCOVERING -> {
                getString(R.string.connected_discovering)
            }
            Connection.STATE_SERVICE_DISCOVERED -> {
                clearCount()
                if (notifyService != null && notifyCharacteristic != null) {
                    Ble.getInstance().getConnection(device!!)?.toggleNotification("1", notifyService!!.uuid,
                            notifyCharacteristic!!.uuid, true)
                }
                getString(R.string.connected_discorvered)
            }
            else -> getString(R.string.connection_released)
        }
        val log = "${SimpleDateFormat("mm:ss.SSS").format(System.currentTimeMillis())}> $text\n"
        val builder = SpannableStringBuilder(log)
        builder.setSpan(ForegroundColorSpan(0xFF2D78E7.toInt()), 0, log.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)//设置颜色
        tvLogs.append(builder)
        scrollView.post {
            scrollView.fullScroll(ScrollView.FOCUS_DOWN)
        }
        invalidateOptionsMenu()
    }

    override fun onDestroy() {
        Ble.getInstance().unregisterSubscriber(this)//取消监听
        super.onDestroy()
    }
}
