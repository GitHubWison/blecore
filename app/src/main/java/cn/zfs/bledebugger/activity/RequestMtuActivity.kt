package cn.zfs.bledebugger.activity

import android.os.Bundle
import android.os.ParcelUuid
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import cn.zfs.bledebugger.R
import cn.zfs.blelib.core.Ble
import cn.zfs.blelib.core.Device
import cn.zfs.blelib.core.Request
import cn.zfs.blelib.event.Events
import cn.zfs.blelib.util.BleUtils
import cn.zfs.common.utils.ToastUtils
import kotlinx.android.synthetic.main.activity_request_mtu.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import kotlin.concurrent.thread

/**
 * 描述:
 * 时间: 2018/5/17 15:32
 * 作者: zengfansheng
 */
class RequestMtuActivity : BaseActivity() {
    private var device: Device? = null
    private var writeService: ParcelUuid? = null
    private var writeCharacteristic: ParcelUuid? = null
    private var mtu = 0
    private var data = ByteArray(0)
    private var loop = false
    private var delay = 0L
    private var run = true

    override fun onDestroy() {
        run = false
        Ble.getInstance().unregisterSubscriber(this)
        super.onDestroy()
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Ble.getInstance().registerSubscriber(this)
        device = intent.getParcelableExtra("device")
        writeService = intent.getParcelableExtra("writeService")
        writeCharacteristic = intent.getParcelableExtra("writeCharacteristic")
        if (device == null || writeService == null || writeCharacteristic == null) {
            finish()
            return
        }
        title = "修改MTU"
        device = Ble.getInstance().getConnection(device)?.device
        setContentView(R.layout.activity_request_mtu)
        btnRequest.setOnClickListener { 
            val numStr = etMtu.text.toString()
            when {
                numStr.isEmpty() -> ToastUtils.showShort("请设置数值")
                numStr.toInt() > 512 -> ToastUtils.showShort("数值超过范围")
                numStr.toInt() < 20 -> ToastUtils.showShort("数值不合法")
                else -> Ble.getInstance().getConnection(device)?.changeMtu("REQUEST_MTU", numStr.toInt())
            }
        }
        btnGenerateByteArr.setOnClickListener {
            var max = if (mtu == 23) 20 else mtu
            val numStr = etMtu.text.toString()
            if (!numStr.isEmpty()) {
                max = Math.min(mtu, numStr.toInt())
            }
            data = ByteArray(max)
            var i = 0
            while (i < max) {
                data[i] = i.toByte()
                i++
            }
            tvData.text = BleUtils.bytesToHexString(data)
        }
        btnSendData.setOnClickListener {
            Ble.getInstance().configuration.packageSize = data.size
            if (loop) {
                thread {
                    while (run && loop) {
                        Ble.getInstance().getConnection(device)?.writeCharacteristic("write", writeService!!.uuid,
                                writeCharacteristic!!.uuid, data)
                        Thread.sleep(delay)
                    }
                    Ble.getInstance().getConnection(device)?.clearRequestQueue()                
                }                
            } else {
                Ble.getInstance().getConnection(device)?.writeCharacteristic("write", writeService!!.uuid,
                        writeCharacteristic!!.uuid, data)
            }
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
        chk.setOnCheckedChangeListener { _, isChecked -> 
            loop = isChecked
        }
    }
    
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMtuChanged(e: Events.MtuChanged) {
        if (e.requestId == "REQUEST_MTU") {
            mtu = e.mtu
            tvMtu.text = "当前MTU： $mtu"
            btnGenerateByteArr.visibility = View.VISIBLE
            btnSendData.visibility = View.VISIBLE
        }
    }

    @Subscribe(threadMode = ThreadMode.POSTING)
    fun onCharacteristicWrite(e: Events.CharacteristicWrite) {
        if (e.requestId == "write" && !loop) {
            ToastUtils.showShort("写入成功")
        }
    }

    @Subscribe(threadMode = ThreadMode.POSTING)
    fun onRequestFialed(e: Events.RequestFailed) {
        if (!loop) {
            if (e.requestId == "REQUEST_MTU" && e.requestType == Request.RequestType.CHANGE_MTU) {
                ToastUtils.showShort("MTU修改失败")
            } else if (e.requestId == "write" && e.requestType == Request.RequestType.WRITE_CHARACTERISTIC) {
                ToastUtils.showShort("写入失败")
            }
        }
    }
}