package cn.zfs.blelib.exception;

/**
 * 描述:
 * 时间: 2018/5/18 14:59
 * 作者: zengfansheng
 */
public class BleException extends RuntimeException {
    private static final long serialVersionUID = -7318707538204243033L;

    public BleException(String detailMessage) {
        super(detailMessage);
    }

    public BleException(Throwable throwable) {
        super(throwable);
    }

    public BleException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }
}
