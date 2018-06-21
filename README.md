# BLE蓝牙调试助手

## 代码托管
[![JitPack](https://img.shields.io/badge/JitPack-blecore-green.svg?style=flat)](https://jitpack.io/#fszeng2011/blecore)
[![Download](https://api.bintray.com/packages/fszeng2017/maven/blecore/images/download.svg) ](https://bintray.com/fszeng2017/maven/blecore/_latestVersion)
[![JCenter](https://img.shields.io/badge/JCenter-2.2.2-green.svg?style=flat)](http://jcenter.bintray.com/com/github/fszeng2011/blecore/2.2.2/)

## 配置
	
	Ble.getInstance().configuration.setPackageSize(20)//分包大小
                .setBondController { device ->
                    //连接时配对控制
                    device.name.startsWith("zfs")
                }
                .setConnectTimeoutMillis(8000)//连接超时时间
                .setDiscoverServicesDelayMillis(500)//连接成功后延时执行发现服务操作的时间
                .setScanHandler { device, scanRecord -> 
                    //扫描过滤器，符合规则的才会在扫描回调中
                    device.name.startsWith("zfs")
                }
                .setTryReconnectTimes(3)//尝试重连的次数，默认无限重连
                .setScanPeriodMillis(10000)//扫描周期
                .setUseBluetoothLeScanner(true)//是否使用新版api的扫描器
                .setWaitWriteResult(true)//写入时是否等待写入回调后再写下一包
                .setPackageWriteDelayMillis(10)//每包的写入延时
		.setRequestWriteDelayMillis(10)//第个请求延时
                .setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)//写入类型

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

	//第一个参数：上下文；第二个参数：蓝牙设备；第三个参数：是否自动重连；第四个参数：连接状态回调
	Ble.getInstance().connect(this, device, true, listener)

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
    fun onConnectFailed(e: Events.ConnectFailed) {
        tvState.text = "连接失败： ${e.type}"
    }    

## 其他请求结果

	@Subscribe(threadMode = ThreadMode.MAIN)
    fun onCharacteristicRead(e: Events.CharacteristicRead) {
        //读特征值的结果
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onNotificationChanged(e: Events.NotificationChanged) {}

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onDescriptorRead(e: Events.DescriptorRead) {
        //读Indication结果
    }

	@Subscribe(threadMode = ThreadMode.MAIN)
    fun onCharacteristicChanged(e: Events.CharacteristicChanged) {
        //设备notifycation的数据
    }

## 读特征值

	Ble.getInstance().getConnection(device)?.readCharacteristic("4", serviceUuid, characteristicUuid)

## 写数据

	Ble.getInstance().getConnection(device)?.writeCharacteristic("3", serviceUuid, characteristicUuid, bytes)

## 开启notifycation

	Ble.getInstance().getConnection(device)?.toggleNotification("1", serviceUuid, characteristicUuid, true)

## 示例效果
![image](https://github.com/fszeng2011/blecore/blob/master/screenshot/device-2018-06-20-170935.png)
![image](https://github.com/fszeng2011/blecore/blob/master/screenshot/device-2018-06-20-171118.png)
![image](https://github.com/fszeng2011/blecore/blob/master/screenshot/device-2018-06-20-171141.png)
![image](https://github.com/fszeng2011/blecore/blob/master/screenshot/device-2018-06-20-171316.png)
![image](https://github.com/fszeng2011/blecore/blob/master/screenshot/device-2018-06-20-171412.png)
![image](https://github.com/fszeng2011/blecore/blob/master/screenshot/device-2018-06-20-173852.png)
