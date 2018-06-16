package cn.zfs.bledebugger.activity

import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.text.Html
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import cn.zfs.bledebugger.Consts
import cn.zfs.bledebugger.R
import cn.zfs.bledebugger.R.id.*
import cn.zfs.blelib.callback.ScanListener
import cn.zfs.blelib.core.Ble
import cn.zfs.blelib.core.Device
import cn.zfs.blelib.util.LogController
import cn.zfs.common.base.BaseHolder
import cn.zfs.common.base.BaseListAdapter
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.system.exitProcess


class MainActivity : CheckPermissionsActivity() {
    private var scanning = false
    private var listAdapter: ListAdapter? = null
    private val devList = ArrayList<Device>()
    private var broadcastContentDialog: BroadcastContentDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        title = "蓝牙设备"
        initViews()
        Ble.getInstance().configuration.setDiscoverServicesDelayMillis(500)        
        Ble.getInstance().setLogPrintLevelControl(LogController.ALL)//输出日志
        Ble.getInstance().addScanListener(scanListener)
        Ble.getInstance().configuration.isWaitWriteResult = true
        Ble.getInstance().configuration.isUseBluetoothLeScanner = false
    }

    private fun initViews() {
        refreshLayout.setColorSchemeColors(ContextCompat.getColor(this, R.color.colorAccent))
        listAdapter = ListAdapter(this, devList)
        lv.adapter = listAdapter
        refreshLayout.setOnRefreshListener {
            if (Ble.getInstance().isInitialized) {
                doStartScan()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (Ble.getInstance().isInitialized) {
            if (Ble.getInstance().isBluetoothAdapterEnabled) {
                if (!refreshLayout.isRefreshing) {
                    refreshLayout.isRefreshing = true
                }
                doStartScan()
            } else {
                startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            }
        }
    }
    
    override fun onPause() {
        super.onPause()
        if (Ble.getInstance().isInitialized) {
            Ble.getInstance().stopScan()
        }
    }
    
    private inner class ListAdapter(context: Context, data: List<Device>) : BaseListAdapter<Device>(context, data) {
        override fun getHolder(position: Int): BaseHolder<Device> {
            return object : BaseHolder<Device>() {
                var tvName: TextView? = null
                var tvAddr: TextView? = null
                var tvRssi: TextView? = null
                var tvBondState: TextView? = null
                var ivType: ImageView? = null

                override fun createConvertView(): View {
                    val view = View.inflate(context, R.layout.item_scan, null)
                    tvName = view.findViewById(R.id.tvName)
                    tvAddr = view.findViewById(R.id.tvAddr)
                    tvRssi = view.findViewById(R.id.tvRssi)
                    ivType = view.findViewById(R.id.ivType)
                    tvBondState = view.findViewById(R.id.tvBondState)
                    view.findViewById<View>(R.id.btnConnect).setOnClickListener { 
                        val pos = tvName?.tag.toString().toInt()
                        val i = Intent(context, GattServiesCharacteristicsActivity::class.java)
                        i.putExtra(Consts.EXTRA_DEVICE, data[pos])
                        context.startActivity(i)
                    }
                    view.findViewById<View>(R.id.layoutInfo).setOnClickListener {
                        val pos = tvName?.tag.toString().toInt()
                        //解析广播
                        if (broadcastContentDialog == null) {
                            broadcastContentDialog = BroadcastContentDialog(this@MainActivity)
                        }
                        val scanRecord = devList[pos].scanRecord
                        if (scanRecord != null) {
                            broadcastContentDialog!!.setData(scanRecord)
                            broadcastContentDialog!!.show()
                        }
                    }
                    return view
                }

                override fun setData(data: Device, position: Int) {
                    tvName?.tag = position
                    tvName?.text = data.name
                    tvAddr?.text = data.addr
                    tvRssi?.text = "${data.rssi} dBm"
                    tvBondState?.text = if (data.bondState == BluetoothDevice.BOND_BONDED) "已配对" else "未配对"
                    val bluetoothClass = data.originalDevice.bluetoothClass
                    if (bluetoothClass != null) {
                        when (bluetoothClass.majorDeviceClass) {
                            BluetoothClass.Device.Major.COMPUTER -> ivType?.setBackgroundResource(R.drawable.computer)
                            BluetoothClass.Device.Major.PHONE -> ivType?.setBackgroundResource(R.drawable.phone)
                            BluetoothClass.Device.Major.WEARABLE -> ivType?.setBackgroundResource(R.drawable.wear)
                            else -> ivType?.setBackgroundResource(R.drawable.bluetooth)
                        }
                    }
                }
            }
        }

        fun clear() {
            data.clear()
            notifyDataSetChanged()
        }

        fun add(device: Device) {
            val dev = data.firstOrNull { it.addr == device.addr }
            if (dev == null) {
                data.add(device)
            } else {
                dev.rssi = device.rssi
            }
            notifyDataSetChanged()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == R.id.menuAbout) {
            val verName = packageManager.getPackageInfo(packageName, 0).versionName
            AlertDialog.Builder(this).setTitle("关于")
                    .setMessage(Html.fromHtml("<b>作者:</b>  Zeng Fansheng<br/><b>版本:</b>  $verName"))
                    .setNegativeButton("OK", null).show()
        } else if (item?.itemId == R.id.menuFeedback) {
            startActivity(Intent(this, FeedbackActivity::class.java))
        }
        return super.onOptionsItemSelected(item)
    }

    private val scanListener = object : ScanListener {
        override fun onScanStart() {
            scanning = true
        }

        override fun onScanStop() {
            refreshLayout.isRefreshing = false
            scanning = false
        }

        override fun onScanResult(device: Device) {
            refreshLayout.isRefreshing = false
            layoutEmpty.visibility = View.INVISIBLE
            listAdapter?.add(device)
        }
    }
    
    private fun doStartScan() {
        listAdapter?.clear()
        layoutEmpty.visibility = View.VISIBLE
        Ble.getInstance().startScan(this)
    }

    override fun onPermissionsRequestResult(hasPermission: Boolean) {
        if (hasPermission) {
            Ble.getInstance().initialize(this, null)
        }
    }

    override fun onDestroy() {
        Ble.getInstance().release(this)
        super.onDestroy()
        exitProcess(0)
    }
}
