package com.wushuangtech.api;


import android.util.LongSparseArray;

import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * 音频处理模块需要实现该接口
 * 内置音频模块MyAudio已实现该接口。
 * 用户也可以自己实现该接口，替换内置的音频模块
 */
public interface ExternalAudioModuleCallback {

    /**
     * 开始采集，调用该接口后，音频模块需要向AudioSender发送编码后的音频数据
     *
     * @return  true-成功，false-失败
     */
    boolean startCapture();

    /**
     * 停止采集，调用该接口后，音频模块停止向AudioSender发送编码后的音频数据
     *
     * @return true-成功，false-失败
     */
    boolean stopCapture();

    /**
     * 开始播放，用于提醒音频模块准备接收数据
     *
     * @return  true-成功，false-失败
     */
    boolean startPlay(long userid);

    /**
     * 停止播放
     *
     * @return  true-成功，false-失败
     */
    boolean stopPlay(long userid);

    /**
     * 接收音频数据，并播放
     *
     * @param buffer  音频数据AAC
     */
    boolean receiveAudioData(long userid , ByteBuffer buffer, int len);
    boolean receiveRTCPMessage(byte[]rtcp_package, int len);

    /**
     * 设置回声消除参数，内置音频模块使用，用户自定义音频模块无需处理
     *
     * @param delayOffsetMS 回声消除参数
     */
    void setDelayoffset(int delayOffsetMS);

    /**
     * 添加音频发送对象
     *
     * @param sender    发送对象
     */
    void addAudioSender(AudioSender sender);

    /**
     * 移除音频发送对象
     *
     * @param sender    发送对象
     */
    void removeAudioSender(AudioSender sender);

    int getRecvBytes(long userid);
    int getCaptureDataSize();
    int getEncodeDataSize();
    int getEncodeFrameCount();
    int getDecodeFrameCount();
    //void saveDelayOffset(int delayOffset);
    void setSendCodec(int type, int bitrate);
    int getDelayEstimate(long userid);
    void setSleepMS(long userid, int ts);
    int getSizeOfMuteAudioPlayed();
    int getSpeechInputLevel();
    int getSpeechInputLevelFullRange();
    int getSpeechOutputLevel(long userid);
    int getSpeechOutputLevelFullRange(long userid);
    void pauseRecordOnly(boolean pause);
    void muteLocal(boolean mute);
    boolean isLocalMuted();
    void remoteAudioMuted(boolean muted, long userid);
    void restartPlayUseVoip(boolean useVoip);

    ArrayList<Long> getSpeakers();
    float audioSoloVolumeScale();

    boolean isCapturing();
    LongSparseArray<ExternalAudioModule.AudioStatistics> getRemoteAudioStatistics();
}
