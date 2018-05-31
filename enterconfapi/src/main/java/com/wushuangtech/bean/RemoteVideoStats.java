package com.wushuangtech.bean;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 远端视频相关的统计信息类
 */
public class RemoteVideoStats implements Parcelable{

    /**
     * 用户ID，指定是哪个用户的视频流
     */
    private long mUid;

    /**
     * 延迟（毫秒）
     */
    private int mDelay; // 暂未生效

    /**
     * 接收码率(kbps)
     */
    private int mReceivedBitrate;

    /**
     * 接收帧率(fps)
     */
    private int mReceivedFrameRate;

    /**
     * 视频流类型，大流或小流
     */
    private int mRxStreamType; // 暂未生效

    public RemoteVideoStats(long mUid, int mDelay, int mReceivedBitrate,
                            int mReceivedFrameRate) {
        this.mUid = mUid;
        this.mDelay = mDelay;
        this.mReceivedBitrate = mReceivedBitrate;
        this.mReceivedFrameRate = mReceivedFrameRate;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.mUid);
        dest.writeInt(this.mDelay);
        dest.writeInt(this.mReceivedBitrate);
        dest.writeInt(this.mReceivedFrameRate);
        dest.writeInt(this.mRxStreamType);
    }

    private RemoteVideoStats(Parcel in) {
        this.mUid = in.readLong();
        this.mDelay = in.readInt();
        this.mReceivedBitrate = in.readInt();
        this.mReceivedFrameRate = in.readInt();
        this.mRxStreamType = in.readInt();
    }

    public static final Creator<RemoteVideoStats> CREATOR = new Creator<RemoteVideoStats>() {
        @Override
        public RemoteVideoStats createFromParcel(Parcel source) {
            return new RemoteVideoStats(source);
        }

        @Override
        public RemoteVideoStats[] newArray(int size) {
            return new RemoteVideoStats[size];
        }
    };
    public long getUid() {
        return mUid;
    }

    public void setUid(long mUid) {
        this.mUid = mUid;
    }

    public int getDelay() {
        return mDelay;
    }

    public void setDelay(int mDelay) {
        this.mDelay = mDelay;
    }

    public int getReceivedBitrate() {
        return mReceivedBitrate;
    }

    public void setReceivedBitrate(int mReceivedBitrate) {
        this.mReceivedBitrate = mReceivedBitrate;
    }

    public int getReceivedFrameRate() {
        return mReceivedFrameRate;
    }

    public void setReceivedFrameRate(int mReceivedFrameRate) {
        this.mReceivedFrameRate = mReceivedFrameRate;
    }


}
