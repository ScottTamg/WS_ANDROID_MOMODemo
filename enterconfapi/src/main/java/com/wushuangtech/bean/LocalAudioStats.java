package com.wushuangtech.bean;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 本地音频相关的统计信息类
 */
public class LocalAudioStats implements Parcelable {
    /**
     * （上次统计后）发送的码率(kbps)
     */
    private int mSentBitrate;

    public LocalAudioStats(int mSentBitrate) {
        this.mSentBitrate = mSentBitrate;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mSentBitrate);
    }

    private LocalAudioStats(Parcel in) {
        this.mSentBitrate = in.readInt();
    }

    public static final Creator<LocalAudioStats> CREATOR = new Creator<LocalAudioStats>() {
        @Override
        public LocalAudioStats createFromParcel(Parcel source) {
            return new LocalAudioStats(source);
        }

        @Override
        public LocalAudioStats[] newArray(int size) {
            return new LocalAudioStats[size];
        }
    };

    public int getSentBitrate() {
        return mSentBitrate;
    }

    public void setSentBitrate(int mSentBitrate) {
        this.mSentBitrate = mSentBitrate;
    }
}
