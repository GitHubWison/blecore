package cn.zfs.blelib.core;

import android.support.annotation.NonNull;

/**
 * 描述: 蓝牙扫描回调
 * 时间: 2018/5/9 00:08
 */
public interface BleScanListener {
    /**
     * 扫描开始
     */
    void onScanStart();

    /**
     * 扫描结束
     */
    void onScanStop();

    /**
     * 扫描结果
     * @param device 设备
     * @param scanRecord 广播内容
     */
    void onScanResult(@NonNull Device device, byte[] scanRecord);
}
