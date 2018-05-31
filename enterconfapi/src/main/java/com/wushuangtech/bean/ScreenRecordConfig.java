package com.wushuangtech.bean;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by wangzhiguo on 18/1/11.
 */

public class ScreenRecordConfig implements Parcelable {

    public int mRecordWidth;
    public int mRecordHeight;
    public int mRecordBitRate;
    public int mRecordFrameRate;
    public int mRecordAudioBitRate;

    public ScreenRecordConfig(int mRecordWidth, int mRecordHeight, int mRecordBitRate, int mRecordFrameRate
        , int mRecordAudioBitRate) {
        this.mRecordWidth = mRecordWidth;
        this.mRecordHeight = mRecordHeight;
        this.mRecordBitRate = mRecordBitRate;
        this.mRecordFrameRate = mRecordFrameRate;
        this.mRecordAudioBitRate = mRecordAudioBitRate;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mRecordWidth);
        dest.writeInt(this.mRecordHeight);
        dest.writeInt(this.mRecordBitRate);
        dest.writeInt(this.mRecordFrameRate);
        dest.writeInt(this.mRecordAudioBitRate);
    }

    protected ScreenRecordConfig(Parcel in) {
        this.mRecordWidth = in.readInt();
        this.mRecordHeight = in.readInt();
        this.mRecordBitRate = in.readInt();
        this.mRecordFrameRate = in.readInt();
        this.mRecordAudioBitRate = in.readInt();
    }

    public static final Creator<ScreenRecordConfig> CREATOR = new Creator<ScreenRecordConfig>() {
        @Override
        public ScreenRecordConfig createFromParcel(Parcel source) {
            return new ScreenRecordConfig(source);
        }

        @Override
        public ScreenRecordConfig[] newArray(int size) {
            return new ScreenRecordConfig[size];
        }
    };
}
