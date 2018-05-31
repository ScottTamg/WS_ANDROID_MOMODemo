package com.wushuangtech.api;


/**
 * 视频处理模块需要实现该接口
 * 内置视频模块MyVideo已实现该接口。
 * 用户也可以自己实现该接口，替换内置的视频模块
 */
public interface ExternalVideoModuleCallback {

    /**
     * 视频帧类型，暂不支持B帧
     */
    enum VideoFrameType {
        FRAMETYPE_INVALID,
        FRAMETYPE_SPS_PPS,
        FRAMETYPE_I,
        FRAMETYPE_P
    }

    /**
     * 开始采集视频，调用该接口后，视频模块需要向VideoSender发送编码后的视频数据
     */
    boolean startCapture();

    /**
     * 停止采集视频，调用该接口后，视频模块停止向VideoSender发送编码后的视频数据
     */
    boolean stopCapture();

    /**
     * 开始第二路视频，调用该接口后，视频模块需要向VideoSender发送编码后的视频数据
     */
    boolean startDualCapture();
    /**
     * 停止第二路视频，调用该接口后，视频模块停止向VideoSender发送编码后的视频数据
     */
    boolean stopDualCapture();

    /**
     * 视频模块接收数据，用于解码并显示
     */
    void receiveVideoData(byte[] data, String devID, long timeStamp, int width, int height, VideoFrameType frameType);

    /**
     * 添加视频数据接收对象
     */
    void addVideoSender(VideoSender sender);

    /**
     * 移除视频数据接收对象
     */
    void removeVideoSender(VideoSender sender);

    /**
     * 获取编码字节数
     */
    int getEncodeDataSize();

    /**
     * 获取编码帧数
     */
    int getEncodeFrameCount();

    /**
     * 获取采集帧数
     */
    int getCaptureFrameCount();

    /**
     * 获取解码帧数
     */
    int getDecodeFrameCount();

    /**
     * 获取播放帧数
     */
    int getRenderFrameCount();

    /**
     * 获取编码视频尺寸
     * width at index of 0
     * height at index of 1
     */
    int[] getEncodeSize();

    boolean isCapturing();

    /**
     * 申请产生I帧
     */
    void requestIFrame();
    /**
     * 在第二路（小流）上申请产生I帧
     */
    void requestDualIFrame();

    /**
     * 当前编码的最高帧率
     * 该值为网络流畅时正常输出视频流的帧率, changeFps上调帧率的上限
     *
     */
    int maxFps();
    /**
     * 请求改变编码帧率（编码器应当能够在帧率较低后输出更低码率的视频流）
     * 当网络发生拥塞时，会请求下调帧率，反之会请求上调帧率
     * 适用于画质优先模式
     */
    void changeFps(int fps);

    /**
     * 当前编码的最高码率
     * 该值为网络流畅时正常输出视频流的码率, changeBitrate上调码率的上限
     *
     */
    int maxBitrate();
    /**
     * 请求改变编码码率
     * 当网络发生拥塞时，会请求下调码率，反之会请求上调码率
     * 适用于流畅度优先模式
     */
    void changeBitrate(int bitrate);
}
