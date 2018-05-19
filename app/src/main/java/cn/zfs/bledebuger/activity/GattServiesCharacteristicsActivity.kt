package cn.zfs.bledebuger.activity

import android.content.Intent
import android.os.Bundle
import android.os.ParcelUuid
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import cn.zfs.bledebuger.R
import cn.zfs.bledebuger.adapter.BleServiceListAdapter
import cn.zfs.bledebuger.entity.Item
import cn.zfs.bledebuger.util.ToastUtils
import cn.zfs.blelib.core.BaseConnection
import cn.zfs.blelib.core.Ble
import cn.zfs.blelib.core.Request
import cn.zfs.blelib.data.*
import kotlinx.android.synthetic.main.activity_gatt_services_characteristics.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * 描述:
 * 时间: 2018/4/27 10:44
 * 作者: zengfansheng
 */
class GattServiesCharacteristicsActivity : AppCompatActivity() {
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
        Ble.getInstance().registerSubscriber(this)
        initViews()
    }

    private fun initViews() {
        adapter = BleServiceListAdapter(this, lv, itemList)
        lv.adapter = adapter
        adapter?.itemClickCallback = object : BleServiceListAdapter.OnItemClickCallback {
            override fun onItemClick(type: Int, node: Item) {
                when (type) {
                    BleServiceListAdapter.READ -> {
                        Ble.getInstance().getConnection(device)?.requestCharacteristicValue(node.toString(), node.service!!.uuid, node.characteristic!!.uuid)
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
                        notifyService = ParcelUuid(node.service!!.uuid)
                        notifyCharacteristic = ParcelUuid(node.characteristic!!.uuid)
                        Ble.getInstance().getConnection(device)?.requestCharacteristicNotification("${node}_1", node.service!!.uuid,
                                node.characteristic!!.uuid, true)
                    }
                    BleServiceListAdapter.STOP_NOTI -> {
                        Ble.getInstance().getConnection(device)?.requestCharacteristicNotification("${node}_0", node.service!!.uuid,
                                node.characteristic!!.uuid, false)
                    }
                }
            }

        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun handleSingleIntEvent(e: SingleValueEvent<Int, Device>) {
        when (e.eventType) {
            EventType.ON_CONNECTION_STATE_CHANGED -> {
                when (e.value) {
                    BaseConnection.STATE_CONNECTED -> {
                        ToastUtils.showShort("连接成功，未搜索服务")
                    }
                    BaseConnection.STATE_CONNECTING -> {
                        ToastUtils.showShort("连接中...")
                    }
                    BaseConnection.STATE_DISCONNECTED -> {
                        ToastUtils.showShort("连接断开")
                        itemList.clear()
                        adapter?.notifyDataSetChanged()
                    }
                    BaseConnection.STATE_RECONNECTING -> {
                        ToastUtils.showShort("正在重连...")
                    }
                    BaseConnection.STATE_SERVICE_DISCORVERING -> {
                        ToastUtils.showShort("连接成功，正在搜索服务...")
                    }
                    BaseConnection.STATE_SERVICE_DISCORVERED -> {
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
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun handleSingleStringEvent(e: SingleValueEvent<String, Device>) {
        when (e.eventType) {
            EventType.ON_CONNECTION_CREATE_FAILED -> ToastUtils.showShort("无法建立连接： ${e.value}")
        }
    }
    
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun handleRequestFailedEvent(e: RequestFailedEvent<Device>) {
        when (e.requestType) {
            Request.RequestType.CHARACTERISTIC_NOTIFICATION -> {
                if (e.requestId.endsWith("_1")) {
                    notifyService = null
                    notifyCharacteristic = null
                }
                ToastUtils.showShort(if (e.requestId.endsWith("_1")) "Notification开启失败" else "Notification关闭失败")
            }
            Request.RequestType.READ_CHARACTERISTIC -> ToastUtils.showShort("characteristic读取失败")
            Request.RequestType.READ_DESCRIPTOR -> ToastUtils.showShort("descriptor读取失败")
        } 
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun handleRequestByteArrayEvent(e: RequestSingleValueEvent<ByteArray, Device>) {
        when (e.eventType) {
            EventType.ON_CHARACTERISTIC_READ, EventType.ON_DESCRIPTOR_READ -> {
                itemList.firstOrNull { e.requestId == it.toString() }?.value = e.result
                adapter?.notifyDataSetChanged()
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun handleRequestEvent(e: RequestEvent<Device>) {
        when (e.eventType) {
            EventType.ON_NOTIFICATION_REGISTERED, EventType.ON_INDICATION_REGISTERED -> {
                itemList.firstOrNull { e.requestId == "${it}_1" }?.notification = true
                adapter?.notifyDataSetChanged()
            }
            EventType.ON_NOTIFICATION_UNREGISTERED, EventType.ON_INDICATION_UNREGISTERED -> {
                itemList.firstOrNull { e.requestId == "${it}_0" }?.notification = false
                adapter?.notifyDataSetChanged()
                if (e.eventType == EventType.ON_NOTIFICATION_UNREGISTERED) {
                    notifyService = null
                    notifyCharacteristic = null
                }
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
        Ble.getInstance().unregisterSubscriber(this)//取消监听
        Ble.getInstance().releaseConnection(device)
        super.onDestroy()
    }
}