package com.wushuangtech.videocore;

/*
 * 视频状态回调接口
 */
public interface VideoStatReportCallback {

    /*
     * 当第一次收到远端视频或视频尺寸改变时回调
     *
     * @param devId 媒体Id
     * @param width 视频宽
     * @param height 视频高
     */
    void UpdateRemoteVideoSize(String devId, int width, int height);
}
