package cn.zfs.blelib.core;

/**
 * 描述: 初始化回调
 * 时间: 2018/4/12 17:16
 * 作者: zengfansheng
 */
public interface InitCallback {
    /** 缺少权限 */
    int ERROR_LACK_PERMISSION = 1;
    
    /** BluetoothManager初始化失败 */
    int ERROR_INIT_FAIL = 2;
    
    /** 不支持BLE */
    int ERROR_NOT_SUPPORT_BLE = 3;

    /**
     * 初始化成功
     */
    void onSuccess();

    /**
     * 初始化失败
     * @param errorCode {@linkplain #ERROR_LACK_PERMISSION}, {@linkplain #ERROR_NOT_SUPPORT_BLE}, {@linkplain #ERROR_INIT_FAIL}
     */
    void onFail(int errorCode);
}
