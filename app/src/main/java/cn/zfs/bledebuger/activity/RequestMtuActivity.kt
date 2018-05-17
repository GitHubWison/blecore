package cn.zfs.bledebuger.activity

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.os.Bundle
import android.os.ParcelUuid
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import cn.zfs.bledebuger.R
import cn.zfs.bledebuger.util.ToastUtils
import cn.zfs.blelib.callback.RequestCallback
import cn.zfs.blelib.core.Ble
import cn.zfs.blelib.core.Request
import cn.zfs.blelib.data.Device
import cn.zfs.blelib.util.BleUtils
import kotlinx.android.synthetic.main.activity_request_mtu.*
import kotlin.concurrent.thread

/**
 * 描述:
 * 时间: 2018/5/17 15:32
 * 作者: zengfansheng
 */
class RequestMtuActivity : AppCompatActivity() {
    private var device: Device? = null
    private var writeService: ParcelUuid? = null
    private var writeCharacteristic: ParcelUuid? = null
    private var mtu = 0
    private var data = ByteArray(0)
    private var callback: MyCallback? = null
    private var loop = false
    private var delay = 0L
    private var run = true

    override fun onDestroy() {
        run = false
        super.onDestroy()
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        device = intent.getParcelableExtra("device")
        writeService = intent.getParcelableExtra("writeService")
        writeCharacteristic = intent.getParcelableExtra("writeCharacteristic")
        if (device == null || writeService == null || writeCharacteristic == null) {
            finish()
            return
        }
        device = Ble.getInstance().getConnection(device)?.device
        callback = MyCallback(device!!)
        setContentView(R.layout.activity_request_mtu)
        btnRequest.setOnClickListener { 
            val numStr = etMtu.text.toString()
            when {
                numStr.isEmpty() -> ToastUtils.showShort("请设置数值")
                numStr.toInt() > 512 -> ToastUtils.showShort("数值超过范围")
                numStr.toInt() < 20 -> ToastUtils.showShort("数值不合法")
                else -> Ble.getInstance().getConnection(device)?.requestMtu("REQUEST_MTU", numStr.toInt(), callback)
            }
        }
        btnGenerateByteArr.setOnClickListener {
            var max = mtu
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
            thread {
                Ble.getInstance().getConnection(device)?.writeCharacteristicValue("write", writeService!!.uuid,
                        writeCharacteristic!!.uuid, data, callback)
                while (run && loop) {
                    Ble.getInstance().getConnection(device)?.writeCharacteristicValue("write", writeService!!.uuid,
                            writeCharacteristic!!.uuid, data, callback)                    
                    Thread.sleep(delay)
                }
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
    
    private inner class MyCallback(dev: Device) : RequestCallback(dev) {
        override fun onMtuChanged(requestId: String, gatt: BluetoothGatt?, mtu: Int) {
            runOnUiThread {
                this@RequestMtuActivity.mtu = mtu
                tvMtu.text = "当前MTU： $mtu"
                btnGenerateByteArr.visibility = View.VISIBLE
                btnSendData.visibility = View.VISIBLE
            }
        }

        override fun onRequestFialed(requestId: String, requestType: Request.RequestType, failType: Int, value: ByteArray?) {
            if (!loop) {
                if (requestId == "REQUEST_MTU" && requestType == Request.RequestType.SET_MTU) {
                    ToastUtils.showShort("修改失败")
                } else if (requestId == "write") {
                    ToastUtils.showShort("写入失败")
                }
            }
        }

        override fun onCharacteristicWrite(requestId: String, gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            if (!loop) {
                ToastUtils.showShort("写入成功")
            }
        }
    }
}