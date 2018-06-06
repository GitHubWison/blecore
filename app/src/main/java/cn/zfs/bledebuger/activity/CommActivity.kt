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
import cn.zfs.blelib.core.Ble
import cn.zfs.blelib.core.Connection
import cn.zfs.blelib.core.Device
import cn.zfs.blelib.core.Request
import cn.zfs.blelib.event.Events
import cn.zfs.blelib.util.BleUtils
import kotlinx.android.synthetic.main.activity_comm.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread

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
                if (loop) {
                    thread {
                        while (run && loop) {
                            Ble.getInstance().getConnection(device)?.writeCharacteristic("3", writeService!!.uuid, writeCharacteristic!!.uuid, bytes)
                            Thread.sleep(delay)
                            if (System.currentTimeMillis() - lastUpdateTime > 500) {
                                lastUpdateTime = System.currentTimeMillis()
                                updateCount()
                            }
                        }
                        Ble.getInstance().getConnection(device)?.clearRequestQueue()
                        updateCount()
                    }
                } else {
                    Ble.getInstance().getConnection(device)?.writeCharacteristic("3", writeService!!.uuid, writeCharacteristic!!.uuid, bytes)
                }                                
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
        btnClearCount.setOnClickListener {
            clearCount()
        }
    }

    private fun updateCount() {
        runOnUiThread {
            tvSuccessCount.text = "成功:$successCount"
            tvFailCount.text = "失败$failCount"
        }
    }

    private fun clearCount() {
        successCount = 0
        failCount = 0
        tvSuccessCount.text = "成功:$successCount"
        tvFailCount.text = "失败$failCount"
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
    fun onConnectionStateChange(e: Events.ConnectionStateChanged) {
        updateState(e.state)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onConnectionCreateFailed(e: Events.ConnectionCreateFailed) {
        tvState.text = "无法建立连接： ${e.error}"
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onCharacteristicChanged(e: Events.CharacteristicChanged) {
        if (!pause) {
            if (tvLogs.text.length > 1024 * 1024) {
                tvLogs.text = ""
            }
            tvLogs.append(SimpleDateFormat("mm:ss.SSS").format(Date()))
            tvLogs.append("> ")
            tvLogs.append(BleUtils.bytesToHexString(e.characteristic.value))
            tvLogs.append("\n")
            scrollView.post {
                scrollView.fullScroll(ScrollView.FOCUS_DOWN)
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onRequestFialed(e: Events.RequestFailed) {
        if (e.requestType == Request.RequestType.WRITE_CHARACTERISTIC) {
            failCount++
            if (!loop) {
                updateCount()
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onCharacteristicWrite(e: Events.CharacteristicWrite) {
        successCount++
        if (!loop) {
            updateCount()
        }
    }
    
    private fun updateState(state: Int) {
        when (state) {
            Connection.STATE_CONNECTED -> {
                tvState.text = "连接成功，等待发现服务"
            }
            Connection.STATE_CONNECTING -> {
                tvState.text = "连接中..."
            }
            Connection.STATE_DISCONNECTED -> {
                tvState.text = "连接断开"
            }
            Connection.STATE_RECONNECTING -> {
                tvState.text = "正在重连..."
            }
            Connection.STATE_SERVICE_DISCORVERING -> {
                tvState.text = "连接成功，正在发现服务..."
            }
            Connection.STATE_SERVICE_DISCORVERED -> {
                tvState.text = "连接成功，并成功发现服务"
                clearCount()
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
