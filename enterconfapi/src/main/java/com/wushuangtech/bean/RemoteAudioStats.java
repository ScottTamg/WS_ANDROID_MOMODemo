package com.wushuangtech.bean;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 远端音频相关的统计信息类
 */
public class RemoteAudioStats implements Parcelable{

    /**
     * 用户ID，指定是哪个用户的视频流
     */
    private long mUid;

    /**
     * 接收码率(kbps)
     */
    private int mReceivedBitrate;

    public RemoteAudioStats(long mUid, int mReceivedBitrate) {
        this.mUid = mUid;
        this.mReceivedBitrate = mReceivedBitrate;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.mUid);
        dest.writeInt(this.mReceivedBitrate);
    }

    private RemoteAudioStats(Parcel in) {
        this.mUid = in.readLong();
        this.mReceivedBitrate = in.readInt();
    }

    public static final Creator<RemoteAudioStats> CREATOR = new Creator<RemoteAudioStats>() {
        @Override
        public RemoteAudioStats createFromParcel(Parcel source) {
            return new RemoteAudioStats(source);
        }

        @Override
        public RemoteAudioStats[] newArray(int size) {
            return new RemoteAudioStats[size];
        }
    };

    public long getUid() {
        return mUid;
    }

    public void setUid(long mUid) {
        this.mUid = mUid;
    }

    public int getReceivedBitrate() {
        return mReceivedBitrate;
    }

    public void setReceivedBitrate(int mReceivedBitrate) {
        this.mReceivedBitrate = mReceivedBitrate;
    }
}
