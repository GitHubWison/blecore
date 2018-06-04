# BLE蓝牙调试助手

## 代码托管
[![JitPack](https://img.shields.io/badge/JitPack-blecore-green.svg?style=flat)](https://jitpack.io/#fszeng2011/blecore)
[![Download](https://api.bintray.com/packages/fszeng2017/maven/blecore/images/download.svg) ](https://bintray.com/fszeng2017/maven/blecore/_latestVersion)
[![JCenter](https://img.shields.io/badge/JCenter-2.1.17-green.svg?style=flat)](http://jcenter.bintray.com/com/github/fszeng2011/blecore/2.1.17/)

## 搜索设备
    
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

## 订阅蓝牙消息

	//订阅消息后，才可以收到蓝牙状态，数据
	Ble.getInstance().registerSubscriber(this)

## 取消订阅
	
	Ble.getInstance().unregisterSubscriber(this)//取消订阅

## 建立连接

	//第一个参数：上下文；第二个参数：蓝牙设备；第三个参数：是否自动重连
	Ble.getInstance().connect(this, device, true)

## 监听连接状态

	@Subscribe(threadMode = ThreadMode.MAIN)
    fun onConnectionStateChange(e: Events.ConnectionStateChanged) {
        when (e.state) {
            Connection.STATE_CONNECTED -> {
                ToastUtils.showShort("连接成功，等待发现服务")
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
                ToastUtils.showShort("连接成功，正在发现服务...")
            }
            Connection.STATE_SERVICE_DISCORVERED -> {
                ToastUtils.showShort("连接成功，并成功发现服务")
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onConnectionCreateFailed(e: Events.ConnectionCreateFailed) {
        ToastUtils.showShort("无法建立连接： ${e.error}")
    }    

## 其他请求结果

	@Subscribe(threadMode = ThreadMode.MAIN)
    fun onCharacteristicRead(e: Events.CharacteristicRead) {
        //读特征值的结果
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onNotificationRegistered(e: Events.NotificationRegistered) {
        //notifycation开启成功
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onNotificationUnregistered(e: Events.NotificationUnregistered) {
        //notifycation关闭成功
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onIndicationRegistered(e: Events.IndicationRegistered) {
        //Indication开启成功
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onIndicationUnregistered(e: Events.IndicationUnregistered) {
        //Indication关闭成功
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onDescriptorRead(e: Events.DescriptorRead) {
        //读Indication结果
    }

	@Subscribe(threadMode = ThreadMode.MAIN)
    fun onCharacteristicChanged(e: Events.CharacteristicChanged) {
        //设备notifycation的数据
    }

## 写数据

	Ble.getInstance().getConnection(device)?.writeCharacteristicValue("3", writeService!!.uuid, writeCharacteristic!!.uuid, bytes)

## 开启notifycation

	Ble.getInstance().getConnection(device)?.requestCharacteristicNotification("1", notifyService!!.uuid,
                            notifyCharacteristic!!.uuid, true)