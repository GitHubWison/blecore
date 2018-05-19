package cn.zfs.blelib.data;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import cn.zfs.blelib.core.BaseConnection;

/**
 * 描述: 蓝牙设备
 * 时间: 2018/4/11 15:00
 * 作者: zengfansheng
 */
public class Device implements Comparable<Device>, Cloneable, Parcelable {
    public String name = "";//设备名称
    public String devId = "";//设备id
    public String addr = "";//设备地址
    public String firmware = "";//固件版本
    public String hardware = "";//硬件版本
    public int type = -1;//设备类型
    public int battery = -1;//电量
    public int rssi = -1000;//信号强度
    public int mode;//工作模式    
    public int connectionState = BaseConnection.STATE_DISCONNECTED;//连接状态

    public Device() {
    }
        
    @Override
    public Device clone() {
        Device device = null;
        try {
            device = (Device) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return device;
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && obj instanceof Device && addr.equals(((Device)obj).addr);
    }

    @Override
    public int hashCode() {
        return addr.hashCode();
    }

    @Override
    public int compareTo(@NonNull Device another) {
        int result;
        if (rssi == 0) {
            return -1;
        } else if (another.rssi == 0) {
            return 1;
        } else {
            result = Integer.compare(another.rssi, rssi);
            if (result == 0) {
                result = name.compareTo(another.name);
            }
        }
        return result;
    }

    public boolean isConnected() {
        return connectionState == BaseConnection.STATE_SERVICE_DISCORVERED;
    }

    public boolean isDisconnected() {
        return connectionState == BaseConnection.STATE_DISCONNECTED;
    }

    public boolean isConnecting() {
        return connectionState != BaseConnection.STATE_DISCONNECTED && connectionState != BaseConnection.STATE_SERVICE_DISCORVERED;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.name);
        dest.writeString(this.devId);
        dest.writeString(this.addr);
        dest.writeString(this.firmware);
        dest.writeString(this.hardware);
        dest.writeInt(this.type);
        dest.writeInt(this.battery);
        dest.writeInt(this.rssi);
        dest.writeInt(this.mode);        
        dest.writeInt(this.connectionState);
    }

    protected Device(Parcel in) {
        this.name = in.readString();
        this.devId = in.readString();
        this.addr = in.readString();
        this.firmware = in.readString();
        this.hardware = in.readString();
        this.type = in.readInt();
        this.battery = in.readInt();
        this.rssi = in.readInt();
        this.mode = in.readInt();        
        this.connectionState = in.readInt();
    }

    public static final Creator<Device> CREATOR = new Creator<Device>() {
        @Override
        public Device createFromParcel(Parcel source) {
            return new Device(source);
        }

        @Override
        public Device[] newArray(int size) {
            return new Device[size];
        }
    };
}
