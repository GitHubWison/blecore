package cn.zfs.blelib.data;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

/**
 * 描述: 写数据结果事件
 * 时间: 2018/5/18 10:28
 * 作者: zengfansheng
 */
public class WriteCharacteristicEvent extends RequestEvent implements Parcelable {
    public boolean result;
    public byte[] value;

    public WriteCharacteristicEvent() {}

    public WriteCharacteristicEvent(int type, Device device, @NonNull String requestId, boolean result, byte[] value) {
        super(type, device, requestId);
        this.result = result;
        this.value = value;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte(this.result ? (byte) 1 : (byte) 0);
        dest.writeByteArray(this.value);
        dest.writeString(this.requestId);
        dest.writeInt(this.eventType);
        dest.writeParcelable(this.device, flags);
    }

    protected WriteCharacteristicEvent(Parcel in) {
        this.result = in.readByte() != 0;
        this.value = in.createByteArray();
        this.requestId = in.readString();
        this.eventType = in.readInt();
        this.device = in.readParcelable(Device.class.getClassLoader());
    }

    public static final Parcelable.Creator<WriteCharacteristicEvent> CREATOR = new Parcelable.Creator<WriteCharacteristicEvent>() {
        @Override
        public WriteCharacteristicEvent createFromParcel(Parcel source) {
            return new WriteCharacteristicEvent(source);
        }

        @Override
        public WriteCharacteristicEvent[] newArray(int size) {
            return new WriteCharacteristicEvent[size];
        }
    };
}
