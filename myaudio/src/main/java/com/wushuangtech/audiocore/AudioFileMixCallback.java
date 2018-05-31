package com.wushuangtech.audiocore;

/**
 * Rtmp推流模块状态回调接口
 *
 */
public interface AudioFileMixCallback {

    enum AudioFileMixStatus
    {
        AudioFileMixStatus_eof      //发送视频数据失败。
    }

    void OnAudioDecoderStatus(AudioFileMixStatus status);
    void OnReportFileDuration(int duration);
    void OnReportPlayoutSeconds(int seconds);
}
