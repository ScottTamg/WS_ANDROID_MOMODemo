package com.wushuangtech.bean;

/**
 * SDK运行时的状态信息类
 */
public class RtcStats {

    /**
     * 通话时长(秒)，累计值
     */
    private int mTotalDuration;
    /**
     * 发送字节数(bytes), 累计值
     */
    private int mTxBytes;
    /**
     * 接收字节数(bytes)，累计值
     */
    private int mRxBytes;
    /**
     * 发送音频码率(kbps)，瞬时值
     */
    private int mTxAudioKBitRate;
    /**
     * 发送视频码率(kbps)，瞬时值
     */
    private int mTxVideoKBitRate;
    /**
     * 接受音频码率(kbps)，瞬时值
     */
    private int mRxAudioKBitRate;
    /**
     * 接受视频码率(kbps)，瞬时值
     */
    private int mRxVideoKBitRate;

    public int getTotalDuration() {
        return mTotalDuration;
    }

    public void setTotalDuration(int mTotalDuration) {
        this.mTotalDuration = mTotalDuration;
    }

    public int getTxBytes() {
        return mTxBytes;
    }

    public void setTxBytes(int mTxBytes) {
        this.mTxBytes = mTxBytes;
    }

    public int getRxBytes() {
        return mRxBytes;
    }

    public void setRxBytes(int mRxBytes) {
        this.mRxBytes = mRxBytes;
    }

    public int getTxAudioKBitRate() {
        return mTxAudioKBitRate;
    }

    public void setTxAudioKBitRate(int mTxAudioKBitRate) {
        this.mTxAudioKBitRate = mTxAudioKBitRate;
    }

    public int getTxVideoKBitRate() {
        return mTxVideoKBitRate;
    }

    public void setTxVideoKBitRate(int mTxVideoKBitRate) {
        this.mTxVideoKBitRate = mTxVideoKBitRate;
    }

    public int getRxAudioKBitRate() {
        return mRxAudioKBitRate;
    }

    public void setRxAudioKBitRate(int mRxAudioKBitRate) {
        this.mRxAudioKBitRate = mRxAudioKBitRate;
    }

    public int getRxVideoKBitRate() {
        return mRxVideoKBitRate;
    }

    public void setRxVideoKBitRate(int mRxVideoKBitRate) {
        this.mRxVideoKBitRate = mRxVideoKBitRate;
    }

    @Override
    public String toString() {
        return " mTotalDuration : " + mTotalDuration + " | mTxBytes : " + mTxBytes
                + " | mRxBytes : " + mRxBytes
                + " | mTxAudioKBitRate : " + mTxAudioKBitRate
                + " | mTxVideoKBitRate : " + mTxVideoKBitRate
                + " | mRxAudioKBitRate : " + mRxAudioKBitRate
                + " | mRxVideoKBitRate : " + mRxVideoKBitRate;
    }
}
