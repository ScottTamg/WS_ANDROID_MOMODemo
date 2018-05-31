package com.wushuangtech.audiocore;

/**
 * Rtmp推流模块状态回调接口
 *
 */
public interface AudioDecoderModuleCallback {

    enum AudioDecoderStatus
    {
        AudioDecoderStatus_eof      //发送视频数据失败。
    }

    void OnAudioDecoderStatus(AudioDecoderStatus status);
    void OnReportFileDuration(int duration);
    void OnReportPlayoutSeconds(int seconds);
}
