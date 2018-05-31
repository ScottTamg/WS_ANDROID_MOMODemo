package com.wushuangtech.audiocore;

/**
 * Created by apple on 2016/10/27.
 */
public interface ExternalAudioProcessCallback {
    public void onRecordPCMData(byte[] data, int dataOffset, int sizeInBytes, int samplingFreq, boolean isStereo);
    public void onPlaybackPCMData(byte[] data, int dataOffset, int sizeInBytes, int samplingFreq, boolean isStereo);
}
