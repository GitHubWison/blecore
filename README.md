# BLE蓝牙调试助手

### 搜索设备
    
	Ble.getInstance().addScanListener(object : BleScanListener {
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
    })
	Ble.getInstance().startScan(this)

### 建立连接及监听状态

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
	        adapter!!.setOnInnerItemClickListener { item, _, _, _ -> 
	            if (item.hasNotifyProperty) {
	                AlertDialog.Builder(this).setItems(arrayOf("开启Notification", "关闭Notification")) { _, which ->
	                    if (which == 0) {
	                        Ble.getInstance().getConnection(device)?.requestCharacteristicNotification("1", item.service!!.uuid,
	                                item.characteristic!!.uuid, Ble.getInstance().getRequestCallback(device), true)
	                    } else {
	                        Ble.getInstance().getConnection(device)?.requestCharacteristicNotification("2", item.service!!.uuid,
	                                item.characteristic!!.uuid, Ble.getInstance().getRequestCallback(device), false)
	                    }
	                }.show()
	            } else if (item.hasWriteProperty) {
	                val i = Intent(this, CommActivity::class.java)
	                i.putExtra("device", device)
	                i.putExtra("writeService", ParcelUuid(item.service!!.uuid))
	                i.putExtra("writeCharacteristic", ParcelUuid(item.characteristic!!.uuid))
	                if (notifyService != null && notifyCharacteristic != null) {
	                    i.putExtra("notifyService", notifyService)
	                    i.putExtra("notifyCharacteristic", notifyCharacteristic)
	                }
	                startActivity(i)
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
	
	        override fun onNotificationRegistered(requestId: String?, descriptor: BluetoothGattDescriptor?) {
	            ToastUtils.showShort("Notification开启成功")
	            if (descriptor != null && descriptor.characteristic != null && descriptor.characteristic.service != null) {
	                notifyService = ParcelUuid(descriptor.characteristic.service.uuid)
	                notifyCharacteristic = ParcelUuid(descriptor.characteristic.uuid)
	            }         
	        }
	
	        override fun onNotificationUnregistered(requestId: String?, descriptor: BluetoothGattDescriptor?) {
	            ToastUtils.showShort("Notification关闭成功")
	            notifyService = null
	            notifyCharacteristic = null
	        }
	        
	        override fun onRequestFialed(requestId: String, requestType: Request.RequestType?, failType: Int) {
	            if (requestType == Request.RequestType.CHARACTERISTIC_NOTIFICATION) {
	                ToastUtils.showShort(if (requestId == "1") "Notification开启失败" else "Notification关闭失败")
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
	    
	    override fun onDestroy() {
	        Ble.getInstance().unregisterObserver(observer)//取消监听
	        Ble.getInstance().releaseConnection(device)
	        super.onDestroy()
	    }
	}

