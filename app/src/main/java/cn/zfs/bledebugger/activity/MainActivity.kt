package cn.zfs.bledebugger.activity

import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.text.Html
import android.view.Menu
import android.view.View
import android.widget.TextView
import cn.zfs.bledebugger.R
import cn.zfs.bledebugger.base.BaseHolder
import cn.zfs.bledebugger.base.BaseListAdapter
import cn.zfs.bledebugger.util.ToastUtils
import cn.zfs.blelib.callback.InitCallback
import cn.zfs.blelib.callback.ScanListener
import cn.zfs.blelib.core.Ble
import cn.zfs.blelib.core.Device
import cn.zfs.blelib.util.LogController
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : CheckPermissionsActivity() {
    private var aboutView: View? = null
    private var scanning = false
    private var listAdapter: ListAdapter? = null
    private val devList = ArrayList<Device>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initViews()
        Ble.getInstance().configuration.setDiscoverServicesDelayMillis(2000)        
        Ble.getInstance().setLogPrintLevelControl(LogController.ALL)//输出日志
        Ble.getInstance().addScanListener(scanListener)
        Ble.getInstance().configuration.isWaitWriteResult = true
    }

    private fun initViews() {
        refreshLayout.setColorSchemeColors(ContextCompat.getColor(this, R.color.colorAccent))
        listAdapter = ListAdapter(this, devList)
        lv.adapter = listAdapter
        lv.setOnItemClickListener { _, _, position, _ ->
            val i = Intent(this, GattServiesCharacteristicsActivity::class.java)
            i.putExtra("device", devList[position])
            startActivity(i)
        }
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
    
    private class ListAdapter(context: Context?, data: MutableList<Device>?) : BaseListAdapter<Device>(context, data) {
        override fun getHolder(): BaseHolder<Device> {
            return object : BaseHolder<Device>() {
                var tvName: TextView? = null
                var tvAddr: TextView? = null
                var tvRssi: TextView? = null

                override fun createConvertView(): View {
                    val view = View.inflate(context, R.layout.item_scan, null)
                    tvName = view.findViewById(R.id.tvName)
                    tvAddr = view.findViewById(R.id.tvAddr)
                    tvRssi = view.findViewById(R.id.tvRssi)
                    return view
                }

                override fun setData(data: Device?, position: Int) {
                    tvName?.text = data?.name
                    tvAddr?.text = data?.addr
                    tvRssi?.text = data?.rssi?.toString()
                }
            }
        }

        fun clear() {
            data.clear()
            notifyDataSetChanged()
        }

        fun add(device: Device) {
            if (!data.contains(device)) {
                data.add(device)
                notifyDataSetChanged()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        if (aboutView == null) {
            aboutView = layoutInflater.inflate(R.layout.about, null)
            aboutView!!.setOnClickListener {
                val verName = packageManager.getPackageInfo(packageName, 0).versionName
                AlertDialog.Builder(this).setTitle("About")
                        .setMessage(Html.fromHtml("<b>Author:</b>  Zeng Fansheng<br/><b>Ver:</b>  $verName"))
                        .setNegativeButton("OK", null).show()
            }
        }
        menu?.findItem(R.id.menuAbout)?.actionView = aboutView
        return true
    }

    private val scanListener = object : ScanListener {
        override fun onScanStart() {
            scanning = true
        }

        override fun onScanStop() {
            scanning = false
        }

        override fun onScanResult(device: Device, scanRecord: ByteArray?) {
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
            Ble.getInstance().initialize(this, object : InitCallback {
                override fun onSuccess() {
                    ToastUtils.showShort("初始化成功")
                }

                override fun onFail(errorCode: Int) {
                    ToastUtils.showShort("初始化失败：$errorCode")
                }
            })
        }
    }

    override fun onDestroy() {
        Ble.getInstance().release(this)
        super.onDestroy()
    }
}
