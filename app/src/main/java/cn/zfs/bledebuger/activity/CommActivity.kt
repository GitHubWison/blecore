package cn.zfs.bledebuger.activity

import android.bluetooth.BluetoothGattCharacteristic
import android.os.Bundle
import android.os.ParcelUuid
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ScrollView
import cn.zfs.bledebuger.R
import cn.zfs.bledebuger.entity.MyBleObserver
import cn.zfs.bledebuger.util.ToastUtils
import cn.zfs.blelib.core.*
import cn.zfs.blelib.data.BleObserver
import cn.zfs.blelib.data.Device
import cn.zfs.blelib.util.BleUtils
import kotlinx.android.synthetic.main.activity_comm.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * 描述:
 * 时间: 2018/4/16 16:28
 * 作者: zengfansheng
 */
class CommActivity : AppCompatActivity() {
    private var observer: BleObserver? = null
    private var device: Device? = null
    private var writeService: ParcelUuid? = null
    private var writeCharacteristic: ParcelUuid? = null
    private var notifyService: ParcelUuid? = null
    private var notifyCharacteristic: ParcelUuid? = null
    private var pause = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comm)
        observer = MyObserver()
        Ble.getInstance().registerObserver(observer)
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
                Ble.getInstance().getConnection(device)?.writeCharacteristicValue("3", writeService!!.uuid, writeCharacteristic!!.uuid, bytes,
                        Ble.getInstance().getRequestCallback(device))
            } catch (e: Exception) {
                ToastUtils.showShort("输入格式错误")
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.connection, menu)
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
        }
        invalidateOptionsMenu()
        return true
    }

    private inner class MyObserver : MyBleObserver() {

        override fun onUnableConnect(device: Device?, error: String?) {
            runOnUiThread {
                tvState.text = "无法连接： $error"
            }
        }

        override fun onConnectionStateChange(device: Device, state: Int) {
            runOnUiThread {
                updateState(state)
            }
        }

        override fun onCharacteristicChanged(characteristic: BluetoothGattCharacteristic) {
            Log.d("CommActivity", "bledebuger--" + BleUtils.bytesToHexString(characteristic.value))
            runOnUiThread {
                if (!pause) {
                    if (tvLogs.text.length > 1024 * 1024) {
                        tvLogs.text = ""
                    }
                    tvLogs.append(SimpleDateFormat("mm:ss.SSS").format(Date()))
                    tvLogs.append("> ")
                    tvLogs.append(BleUtils.bytesToHexString(characteristic.value))
                    tvLogs.append("\n")
                    scrollView.post {
                        scrollView.fullScroll(ScrollView.FOCUS_DOWN)
                    }
                }
            }
        }
    }

    private fun updateState(state: Int) {
        when (state) {
            Connection.STATE_CONNECTED -> {
                tvState.text = "连接成功，未搜索服务"
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
                tvState.text = "连接成功，正在搜索服务..."
            }
            Connection.STATE_SERVICE_DISCORVERED -> {
                tvState.text = "连接成功，并搜索到服务"
                if (notifyService != null && notifyCharacteristic != null) {
                    Ble.getInstance().getConnection(device)?.requestCharacteristicNotification("1", notifyService!!.uuid,
                            notifyCharacteristic!!.uuid, Ble.getInstance().getRequestCallback(device), true)
                }
            }
        }
        invalidateOptionsMenu()
    }

    override fun onDestroy() {
        Ble.getInstance().unregisterObserver(observer)//取消监听
        super.onDestroy()
    }
}
