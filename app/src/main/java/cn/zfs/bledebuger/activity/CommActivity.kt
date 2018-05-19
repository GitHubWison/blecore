package cn.zfs.bledebuger.activity

import android.content.Intent
import android.os.Bundle
import android.os.ParcelUuid
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.widget.ScrollView
import cn.zfs.bledebuger.R
import cn.zfs.bledebuger.util.ToastUtils
import cn.zfs.blelib.core.BaseConnection
import cn.zfs.blelib.core.Ble
import cn.zfs.blelib.core.Request
import cn.zfs.blelib.data.*
import cn.zfs.blelib.util.BleUtils
import kotlinx.android.synthetic.main.activity_comm.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.text.SimpleDateFormat
import java.util.*

/**
 * 描述:
 * 时间: 2018/4/16 16:28
 * 作者: zengfansheng
 */
class CommActivity : AppCompatActivity() {
    private var device: Device? = null
    private var writeService: ParcelUuid? = null
    private var writeCharacteristic: ParcelUuid? = null
    private var notifyService: ParcelUuid? = null
    private var notifyCharacteristic: ParcelUuid? = null
    private var pause = false
    private var loop = false
    private var delay = 0L
    private var run = true
    private var lastUpdateTime = 0L
    private var successCount = 0
    private var failCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comm)
        Ble.getInstance().registerSubscriber(this)
        device = intent.getParcelableExtra("device")
        writeService = intent.getParcelableExtra("writeService")
        writeCharacteristic = intent.getParcelableExtra("writeCharacteristic")
        notifyService = intent.getParcelableExtra("notifyService")
        notifyCharacteristic = intent.getParcelableExtra("notifyCharacteristic")
        if (device == null || writeService == null || writeCharacteristic == null) {
            finish()
            return
        }
        device = Ble.getInstance().getConnection(device)?.device
        tvName.text = device!!.name
        tvAddr.text = device!!.addr
        initEvents()
        updateState(device!!.connectionState)
        Ble.getInstance().registerSubscriber(this)
    }

    private fun initEvents() {
        //设置
        btnClear.setOnClickListener {
            tvLogs.text = ""
        }
        btnPause.setOnClickListener { 
            pause = true
        }
        btnStart.setOnClickListener { 
            pause = false
        }
        btnSend.setOnClickListener {
            val s = etValue.text.trim().toString()
            try {
                val arr = s.trim({ it <= ' ' }).replace(" +".toRegex(), " ").split(" ".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
                val bytes = ByteArray(arr.size)
                arr.forEachIndexed { i, str ->
                    if (str.length > 2) {
                        throw Exception()
                    }
                    bytes[i] = Integer.valueOf(str, 16).toByte()
                }
                Ble.getInstance().getConnection(device)?.writeCharacteristicValue("3", writeService!!.uuid, writeCharacteristic!!.uuid, bytes)
                while (run && loop) {
                    Ble.getInstance().getConnection(device)?.writeCharacteristicValue("3", writeService!!.uuid, writeCharacteristic!!.uuid, bytes)
                    Thread.sleep(delay)
                    if (System.currentTimeMillis() - lastUpdateTime > 500) {
                        lastUpdateTime = System.currentTimeMillis()
                        runOnUiThread {
                            tvSuccessCount.text = "成功:$successCount"
                            tvFailCount.text = "失败$failCount"
                        }
                    }
                }
                runOnUiThread {
                    tvSuccessCount.text = "成功:$successCount"
                    tvFailCount.text = "失败$failCount"
                }
                Ble.getInstance().getConnection(device)?.clearRequestQueue()
            } catch (e: Exception) {
                ToastUtils.showShort("输入格式错误")
            }
        }
        chk.setOnCheckedChangeListener { _, isChecked ->
            loop = isChecked
        }
        etDelay.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                var d = 0L
                val delayStr = etDelay.text.toString()
                if (!delayStr.isEmpty()) {
                    d = delayStr.toLong()
                }
                delay = d
            }
        })
        btnClearCont.setOnClickListener {
            successCount = 0
            failCount = 0
            tvSuccessCount.text = "成功:$successCount"
            tvFailCount.text = "失败$failCount"
        }
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
                Ble.getInstance().disconnectConnection(device)
            }
            R.id.menuConnect -> {//连接
                Ble.getInstance().connect(this, device, true)
            }
            R.id.menuRequestMtu -> {//请求修改mtu
                val intent = Intent(this, RequestMtuActivity::class.java)
                intent.putExtra("device", device)
                intent.putExtra("writeService", ParcelUuid(writeService!!.uuid))
                intent.putExtra("writeCharacteristic", ParcelUuid(writeCharacteristic!!.uuid))
                startActivity(intent)
            }
        }
        invalidateOptionsMenu()
        return true
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun handleSingleIntEvent(e: SingleValueEvent<Int, Device>) {
        when (e.eventType) {
            EventType.ON_CONNECTION_STATE_CHANGED -> updateState(e.value)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun handleSingleStringEvent(e: SingleValueEvent<String, Device>) {
        when (e.eventType) {
            EventType.ON_CONNECTION_CREATE_FAILED -> tvState.text = "无法建立连接： ${e.value}"
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun handleSingleByteArrayEvent(e: SingleValueEvent<ByteArray, Device>) {
        when (e.eventType) {
            EventType.ON_CHARACTERISTIC_CHANGED -> {
                if (!pause) {
                    if (tvLogs.text.length > 1024 * 1024) {
                        tvLogs.text = ""
                    }
                    tvLogs.append(SimpleDateFormat("mm:ss.SSS").format(Date()))
                    tvLogs.append("> ")
                    tvLogs.append(BleUtils.bytesToHexString(e.value))
                    tvLogs.append("\n")
                    scrollView.post {
                        scrollView.fullScroll(ScrollView.FOCUS_DOWN)
                    }
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun handleRequestEvent(e: RequestEvent<Device>) {
        if (e.eventType == EventType.ON_WRITE_CHARACTERISTIC) {
            successCount++
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun handleRequestFailedEvent(e: RequestFailedEvent<Device>) {
        if (e.requestType == Request.RequestType.WRITE_CHARACTERISTIC) {
            failCount++
        }
    }

    private fun updateState(state: Int) {
        when (state) {
            BaseConnection.STATE_CONNECTED -> {
                tvState.text = "连接成功，未搜索服务"
            }
            BaseConnection.STATE_CONNECTING -> {
                tvState.text = "连接中..."
            }
            BaseConnection.STATE_DISCONNECTED -> {
                tvState.text = "连接断开"
            }
            BaseConnection.STATE_RECONNECTING -> {
                tvState.text = "正在重连..."
            }
            BaseConnection.STATE_SERVICE_DISCORVERING -> {
                tvState.text = "连接成功，正在搜索服务..."
            }
            BaseConnection.STATE_SERVICE_DISCORVERED -> {
                tvState.text = "连接成功，并搜索到服务"
                if (notifyService != null && notifyCharacteristic != null) {
                    Ble.getInstance().getConnection(device)?.requestCharacteristicNotification("1", notifyService!!.uuid,
                            notifyCharacteristic!!.uuid, true)
                }
            }
        }
        invalidateOptionsMenu()
    }

    override fun onDestroy() {
        Ble.getInstance().unregisterSubscriber(this)//取消监听
        super.onDestroy()
    }
}
