package com.wushuangtech.api;

/**
 * Rtmp推流模块状态回调接口
 *
 */
public interface ExternalRtmpPublishModuleCallback {

    enum RtmpErrorType
    {
        RtmpErrorType_InitError,      //初始化RTMP发送器失败。
        RtmpErrorType_OpenError,      //打开RTMP链接失败。
        RtmpErrorType_AudioNoBuf,     //音频数据缓冲区空间不足。
        RtmpErrorType_VideoNoBuf,     //视频数据缓冲区空间不足
		RtmpErrorType_LinkFailed      //发送视频数据失败。
    }

    /**
     * 状态更新
     *
     * @param errorType @see RtmpErrorType
     */
    void receiveRtmpStatus(RtmpErrorType errorType);
}
