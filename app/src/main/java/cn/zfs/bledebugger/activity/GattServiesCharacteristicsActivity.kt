package cn.zfs.bledebugger.activity

import android.content.Intent
import android.os.Bundle
import android.os.ParcelUuid
import android.view.Menu
import android.view.MenuItem
import cn.zfs.bledebugger.Consts
import cn.zfs.bledebugger.R
import cn.zfs.bledebugger.adapter.BleServiceListAdapter
import cn.zfs.bledebugger.entity.Item
import cn.zfs.bledebugger.view.LoadDialog
import cn.zfs.blelib.core.Ble
import cn.zfs.blelib.core.Connection
import cn.zfs.blelib.core.Device
import cn.zfs.blelib.core.Request
import cn.zfs.blelib.event.Events
import cn.zfs.common.utils.ToastUtils
import kotlinx.android.synthetic.main.activity_gatt_services_characteristics.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * 描述:
 * 时间: 2018/4/27 10:44
 * 作者: zengfansheng
 */
class GattServiesCharacteristicsActivity : BaseActivity() {
    private var device: Device? = null
    private var itemList = ArrayList<Item>()
    private var adapter: BleServiceListAdapter? = null
    private var notifyService: ParcelUuid? = null
    private var notifyCharacteristic: ParcelUuid? = null
    private var loadDialog: LoadDialog? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gatt_services_characteristics)
        device = intent.getParcelableExtra(Consts.EXTRA_DEVICE)
        if (device == null) {
            finish()
            return
        }
        title = device!!.name
		Ble.getInstance().registerSubscriber(this)
        Ble.getInstance().connect(this, device!!, true, null)        
        initViews()
    }

    private fun initViews() {
        loadDialog = LoadDialog(this)
        loadDialog!!.setCanceledOnTouchOutside(false)
        adapter = BleServiceListAdapter(this, lv, itemList)
        lv.adapter = adapter
        adapter?.itemClickCallback = object : BleServiceListAdapter.OnItemClickCallback {
            override fun onItemClick(type: Int, node: Item) {
                when (type) {
                    BleServiceListAdapter.READ -> {
                        Ble.getInstance().getConnection(device!!)?.readCharacteristic(node.toString(), node.service!!.uuid, node.characteristic!!.uuid)
                    }
                    BleServiceListAdapter.SEND -> {
                        val i = Intent(this@GattServiesCharacteristicsActivity, CommActivity::class.java)
                        i.putExtra(Consts.EXTRA_DEVICE, device)
                        i.putExtra(Consts.EXTRA_WRITE_SERVICE, ParcelUuid(node.service!!.uuid))
                        i.putExtra(Consts.EXTRA_WRITE_CHARACTERISTIC, ParcelUuid(node.characteristic!!.uuid))
                        if (notifyService != null && notifyCharacteristic != null) {
                            i.putExtra(Consts.EXTRA_NOTIFY_SERVICE, notifyService)
                            i.putExtra(Consts.EXTRA_NOTIFY_CHARACTERISTIC, notifyCharacteristic)
                        }
                        startActivity(i)
                    }
                    BleServiceListAdapter.START_NOTI -> {
                        notifyService = ParcelUuid(node.service!!.uuid)
                        notifyCharacteristic = ParcelUuid(node.characteristic!!.uuid)
                        Ble.getInstance().getConnection(device!!)?.toggleNotification("$node", node.service!!.uuid,
                                node.characteristic!!.uuid, true)
                    }
                    BleServiceListAdapter.STOP_NOTI -> {
                        Ble.getInstance().getConnection(device!!)?.toggleNotification("$node", node.service!!.uuid,
                                node.characteristic!!.uuid, false)
                    }
                }
            }

        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onConnectionStateChange(e: Events.ConnectionStateChanged) {
        when (e.state) {
            Connection.STATE_CONNECTED -> {
                loadDialog!!.show()
                loadDialog!!.setText(R.string.connected_not_discover)
            }
            Connection.STATE_CONNECTING -> {
                loadDialog!!.show()
                loadDialog!!.setText(R.string.connecting)
            }
            Connection.STATE_DISCONNECTED -> {
                loadDialog!!.dismiss()
                ToastUtils.showShort(R.string.disconnected)
                itemList.clear()
                adapter?.notifyDataSetChanged()
            }
            Connection.STATE_RECONNECTING -> {
                loadDialog!!.show()
                loadDialog!!.setText(R.string.reconnecting)
            }
            Connection.STATE_SERVICE_DISCOVERING -> {
                loadDialog!!.show()
                loadDialog!!.setText(R.string.connected_discovering)
            }
            Connection.STATE_SERVICE_DISCOVERED -> {
                loadDialog!!.dismiss()
                ToastUtils.showShort(R.string.connected_discorvered)
                itemList.clear()
                val connection = Ble.getInstance().getConnection(device!!)
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
    
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onRequestFialed(e: Events.RequestFailed) {
        when (e.requestType) {
            Request.RequestType.TOGGLE_NOTIFICATION -> {
                if (e.requestId.endsWith("_1")) {
                    notifyService = null
                    notifyCharacteristic = null
                }
                ToastUtils.showShort(if (e.requestId.endsWith("_1")) R.string.notification_enable_failed else R.string.notification_disable_failed)
            }
            Request.RequestType.READ_CHARACTERISTIC -> ToastUtils.showShort(R.string.characteristic_read_failed)
            Request.RequestType.READ_DESCRIPTOR -> ToastUtils.showShort(R.string.descriptor_read_failed)
        } 
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onCharacteristicRead(e: Events.CharacteristicRead) {
        itemList.firstOrNull { e.requestId == it.toString() }?.value = e.characteristic.value
        adapter?.notifyDataSetChanged()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onNotificationChanged(e: Events.NotificationChanged) {
        itemList.firstOrNull { e.requestId == "$it" }?.notification = e.isEnabled
        if (!e.isEnabled) {
            notifyService = null
            notifyCharacteristic = null
        }
        adapter?.notifyDataSetChanged()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onCharacteristicChanged(e: Events.CharacteristicChanged) {
        adapter?.updateValue(e.characteristic.service.uuid, e.characteristic.uuid, e.characteristic.value)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onDescriptorRead(e: Events.DescriptorRead) {
        itemList.firstOrNull { e.requestId == it.toString() }?.value = e.descriptor.value
        adapter?.notifyDataSetChanged()
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
                Ble.getInstance().disconnectConnection(device!!)
            }
            R.id.menuConnect -> {//连接
                Ble.getInstance().connect(this, device!!, true, null)
            }
            R.id.menuHex -> adapter?.setShowInHex(true)
            R.id.menuAscii -> adapter?.setShowInHex(false)
        }
        invalidateOptionsMenu()
        return super.onOptionsItemSelected(item)
    }
    
    override fun onDestroy() {
        Ble.getInstance().unregisterSubscriber(this)//取消监听
        Ble.getInstance().releaseConnection(device!!)
        super.onDestroy()
    }
}