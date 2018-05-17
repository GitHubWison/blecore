package cn.zfs.bledebuger.activity

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.content.Intent
import android.os.Bundle
import android.os.ParcelUuid
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import cn.zfs.bledebuger.R
import cn.zfs.bledebuger.adapter.BleServiceListAdapter
import cn.zfs.bledebuger.entity.Item
import cn.zfs.bledebuger.entity.MyBleObserver
import cn.zfs.bledebuger.util.ToastUtils
import cn.zfs.blelib.core.Ble
import cn.zfs.blelib.core.Connection
import cn.zfs.blelib.core.Request
import cn.zfs.blelib.data.BleObserver
import cn.zfs.blelib.data.Device
import kotlinx.android.synthetic.main.activity_gatt_services_characteristics.*

/**
 * 描述:
 * 时间: 2018/4/27 10:44
 * 作者: zengfansheng
 */
class GattServiesCharacteristicsActivity : AppCompatActivity() {
    private var observer: BleObserver? = null
    private var device: Device? = null
    private var itemList = ArrayList<Item>()
    private var adapter: BleServiceListAdapter? = null
    private var notifyService: ParcelUuid? = null
    private var notifyCharacteristic: ParcelUuid? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gatt_services_characteristics)
        device = intent.getParcelableExtra("device")
        if (device == null) {
            finish()
            return
        }
        Ble.getInstance().connect(this, device, true)
        observer = MyObserver()
        Ble.getInstance().registerObserver(observer)
        initViews()
    }

    private fun initViews() {
        adapter = BleServiceListAdapter(this, lv, itemList)
        lv.adapter = adapter
        adapter?.itemClickCallback = object : BleServiceListAdapter.OnItemClickCallback {
            override fun onItemClick(type: Int, node: Item) {
                when (type) {
                    BleServiceListAdapter.READ -> {
                        Ble.getInstance().getConnection(device)?.requestCharacteristicValue(node.toString(), node.service!!.uuid, 
                                node.characteristic!!.uuid, Ble.getInstance().getRequestCallback(device))
                    }
                    BleServiceListAdapter.SEND -> {
                        val i = Intent(this@GattServiesCharacteristicsActivity, CommActivity::class.java)
                        i.putExtra("device", device)
                        i.putExtra("writeService", ParcelUuid(node.service!!.uuid))
                        i.putExtra("writeCharacteristic", ParcelUuid(node.characteristic!!.uuid))
                        if (notifyService != null && notifyCharacteristic != null) {
                            i.putExtra("notifyService", notifyService)
                            i.putExtra("notifyCharacteristic", notifyCharacteristic)
                        }
                        startActivity(i)
                    }
                    BleServiceListAdapter.START_NOTI -> {
                        Ble.getInstance().getConnection(device)?.requestCharacteristicNotification(node.toString(), node.service!!.uuid,
                                node.characteristic!!.uuid, Ble.getInstance().getRequestCallback(device), true)
                    }
                    BleServiceListAdapter.STOP_NOTI -> {
                        Ble.getInstance().getConnection(device)?.requestCharacteristicNotification(node.toString(), node.service!!.uuid,
                                node.characteristic!!.uuid, Ble.getInstance().getRequestCallback(device), false)
                    }
                }
            }

        }
    }

    private inner class MyObserver : MyBleObserver() {

        override fun onUnableConnect(device: Device?, error: String?) {
            runOnUiThread {
                ToastUtils.showShort("无法连接")
            }
        }

        override fun onConnectionStateChange(device: Device, state: Int) {
            runOnUiThread {
                when (state) {
                    Connection.STATE_CONNECTED -> {
                        ToastUtils.showShort("连接成功，未搜索服务")
                    }
                    Connection.STATE_CONNECTING -> {
                        ToastUtils.showShort("连接中...")
                    }
                    Connection.STATE_DISCONNECTED -> {
                        ToastUtils.showShort("连接断开")
                        itemList.clear()
                        adapter?.notifyDataSetChanged()
                    }
                    Connection.STATE_RECONNECTING -> {
                        ToastUtils.showShort("正在重连...")
                    }
                    Connection.STATE_SERVICE_DISCORVERING -> {
                        ToastUtils.showShort("连接成功，正在搜索服务...")
                    }
                    Connection.STATE_SERVICE_DISCORVERED -> {
                        ToastUtils.showShort("连接成功，并搜索到服务")
                        itemList.clear()
                        val connection = Ble.getInstance().getConnection(device)
                        if (connection != null) {
                            var id = 0
                            connection.gattServices.forEach { service ->
                                val pid = id
                                itemList.add(Item(pid, 0, 0, false, true, service, null))
                                id++
                                service.characteristics.forEach { characteristic ->
                                    itemList.add(Item(id++, pid, 1, false, false, service, characteristic))
                                }
                            }
                        }
                        adapter?.notifyDataSetChanged()
                    }
                }
                invalidateOptionsMenu()
            }
        }

        override fun onNotificationRegistered(requestId: String, descriptor: BluetoothGattDescriptor?) {
            if (descriptor != null && descriptor.characteristic != null && descriptor.characteristic.service != null) {
                notifyService = ParcelUuid(descriptor.characteristic.service.uuid)
                notifyCharacteristic = ParcelUuid(descriptor.characteristic.uuid)
            }
            runOnUiThread {
                itemList.firstOrNull { requestId == it.toString() }?.notification = true
                adapter?.notifyDataSetChanged()
            }
        }

        override fun onNotificationUnregistered(requestId: String, descriptor: BluetoothGattDescriptor?) {
            notifyService = null
            notifyCharacteristic = null
            runOnUiThread {
                itemList.firstOrNull { requestId == it.toString() }?.notification = false
                adapter?.notifyDataSetChanged()
            }
        }
        
        override fun onRequestFialed(requestId: String, requestType: Request.RequestType?, failType: Int) {
            if (requestType == Request.RequestType.CHARACTERISTIC_NOTIFICATION) {
                ToastUtils.showShort(if (requestId == "1") "Notification开启失败" else "Notification关闭失败")
            }
        }

        override fun onCharacteristicRead(requestId: String, gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            runOnUiThread {
                itemList.firstOrNull { requestId == it.toString() }?.value = characteristic?.value
                adapter?.notifyDataSetChanged()
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
            R.id.menuHex -> adapter?.setShowInHex(true)
            R.id.menuUtf8 -> adapter?.setShowInHex(false)
        }
        invalidateOptionsMenu()
        return true
    }
    
    override fun onDestroy() {
        Ble.getInstance().unregisterObserver(observer)//取消监听
        Ble.getInstance().releaseConnection(device)
        super.onDestroy()
    }
}