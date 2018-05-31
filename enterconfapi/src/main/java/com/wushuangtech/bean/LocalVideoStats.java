package com.wushuangtech.bean;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 本地视频相关的统计信息类
 */
public class LocalVideoStats implements Parcelable{
    /**
     * （上次统计后）发送的码率(kbps)
     */
    private int mSentBitrate;
    /**
     * (上次统计后）发送的帧率(fps)
     */
    private int mSentFrameRate;

    public LocalVideoStats(int mSentBitrate, int mSentFrameRate) {
        this.mSentBitrate = mSentBitrate;
        this.mSentFrameRate = mSentFrameRate;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mSentBitrate);
        dest.writeInt(this.mSentFrameRate);
    }

    private LocalVideoStats(Parcel in) {
        this.mSentBitrate = in.readInt();
        this.mSentFrameRate = in.readInt();
    }

    public static final Creator<LocalVideoStats> CREATOR = new Creator<LocalVideoStats>() {
        @Override
        public LocalVideoStats createFromParcel(Parcel source) {
            return new LocalVideoStats(source);
        }

        @Override
        public LocalVideoStats[] newArray(int size) {
            return new LocalVideoStats[size];
        }
    };

    public int getSentBitrate() {
        return mSentBitrate;
    }

    public int getSentFrameRate() {
        return mSentFrameRate;
    }

    public void setSentBitrate(int mSentBitrate) {
        this.mSentBitrate = mSentBitrate;
    }

    public void setSentFrameRate(int mSentFrameRate) {
        this.mSentFrameRate = mSentFrameRate;
    }
}
