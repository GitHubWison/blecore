package cn.zfs.bledebuger.activity

import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import cn.zfs.bledebuger.R
import cn.zfs.bledebuger.base.BaseHolder
import cn.zfs.bledebuger.base.BaseListAdapter
import cn.zfs.bledebuger.entity.MyBleObservable
import cn.zfs.bledebuger.entity.MyRequestCallback
import cn.zfs.bledebuger.util.ToastUtils
import cn.zfs.blelib.callback.BleScanListener
import cn.zfs.blelib.callback.InitCallback
import cn.zfs.blelib.core.Ble
import cn.zfs.blelib.data.Device
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
        Ble.getInstance().config.setDiscoverServicesDelayMillis(2000)
                .setRequestCallbackClass(MyRequestCallback::class.java)
                .setBleObservableClass(MyBleObservable::class.java)
        
        Ble.getInstance().setLogPrintLevelControl(LogController.ALL)//输出日志
        Ble.getInstance().addScanListener(scanListener)
    }

    private fun initViews() {
        listAdapter = ListAdapter(this, devList)
        lv.adapter = listAdapter
        lv.setOnItemClickListener { _, _, position, _ ->
            val i = Intent(this, GattServiesCharacteristicsActivity::class.java)
            i.putExtra("device", devList[position])
            startActivity(i)
        }
    }

    override fun onResume() {
        super.onResume()
        if (Ble.getInstance().isInited) {
            if (Ble.getInstance().isBluetoothAdapterEnabled) {
                doStartScan()
            } else {
                startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            }
        }
    }
    
    override fun onPause() {
        super.onPause()
        if (Ble.getInstance().isInited) {
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
        if (!Ble.getInstance().isInited) {
            menu?.findItem(R.id.menuStop)?.isVisible = false
            menu?.findItem(R.id.menuScan)?.isVisible = false
            menu?.findItem(R.id.menuProgress)?.actionView = null
        } else if (!scanning) {
            menu?.findItem(R.id.menuStop)?.isVisible = false
            menu?.findItem(R.id.menuScan)?.isVisible = true
            menu?.findItem(R.id.menuProgress)?.actionView = null
        } else {
            menu?.findItem(R.id.menuStop)?.isVisible = true
            menu?.findItem(R.id.menuScan)?.isVisible = false
            menu?.findItem(R.id.menuProgress)?.setActionView(R.layout.progress)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuScan -> {
                doStartScan()
            }
            R.id.menuStop -> {
                Ble.getInstance().stopScan()
            }
        }
        return true
    }

    private val scanListener = object : BleScanListener {
        override fun onScanStart() {
            scanning = true
            invalidateOptionsMenu()
        }

        override fun onScanStop() {
            scanning = false
            invalidateOptionsMenu()
        }

        override fun onScanResult(device: Device, scanRecord: ByteArray?) {
            listAdapter?.add(device)
        }
    }
    
    private fun doStartScan() {
        listAdapter?.clear()
        Ble.getInstance().startScan(this)
    }

    override fun onPermissionsRequestResult(hasPermission: Boolean) {
        if (hasPermission) {
            Ble.getInstance().init(this, object : InitCallback {
                override fun onSuccess() {
                    ToastUtils.showShort("初始化成功")
//                    val state = FyBle.getBondState("D3:18:FC:EE:38:42")
//                    Log.d("d", "blelib--绑定状态：$state")

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
