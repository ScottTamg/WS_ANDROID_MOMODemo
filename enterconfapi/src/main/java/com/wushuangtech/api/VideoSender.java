package com.wushuangtech.api;

import java.util.ArrayList;

/**
 * 该接口用于发送编码后的视频数据 ExternalVideoModule、ExternalPublishModule已实现该接口
 */
public interface VideoSender {
    /**
     * 发送视频数据
     *
     * @param h264_nal      h.264 nal数组
     * @param frameType     视频帧类型@see ExternalVideoModuleCallback.VideoFrameType
     * @param videoWidth    视频宽
     * @param videoHeight   视频高
     */
    void pushEncodedVideoData(ArrayList<byte[]> h264_nal,
                              ExternalVideoModuleCallback.VideoFrameType frameType,
                              int videoWidth,
                              int videoHeight);

    /**
     * 发送第二路视频数据
     *
     * @param h264_nal      h.264 nal数组
     * @param frameType     视频帧类型@see ExternalVideoModuleCallback.VideoFrameType
     * @param videoWidth    视频宽
     * @param videoHeight   视频高
     */
    void pushDualEncodedVideoData(ArrayList<byte[]> h264_nal,
                                  ExternalVideoModuleCallback.VideoFrameType frameType,
                                  int videoWidth,
                                  int videoHeight);
}
