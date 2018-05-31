package com.wushuangtech.jni;

import com.wushuangtech.library.GlobalConfig;
import com.wushuangtech.library.GlobalHolder;
import com.wushuangtech.library.PviewConferenceRequest;
import com.wushuangtech.utils.PviewLog;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class VideoJni {
    private static VideoJni mVideoJni;
    private List<WeakReference<PviewConferenceRequest>> mCallBacks;

    private VideoJni() {
        mCallBacks = new ArrayList<>();
    }


    public static synchronized VideoJni getInstance() {
        if (mVideoJni == null) {
            synchronized (VideoJni.class) {
                if (mVideoJni == null) {
                    mVideoJni = new VideoJni();
                    if (!mVideoJni.initialize(mVideoJni)) {
                        throw new RuntimeException("can't initilaize VideoJni");
                    }
                }
            }
        }
        return mVideoJni;
    }

    /**
     * 添加自定义的回调，监听接收到的服务信令
     *
     * @param callback
     */
    public void addCallback(PviewConferenceRequest callback) {
        this.mCallBacks.add(new WeakReference<PviewConferenceRequest>(callback));
    }

    /**
     * 移除自定义添加的回调
     *
     * @param callback
     */
    public void removeCallback(PviewConferenceRequest callback) {
        for (int i = 0; i < mCallBacks.size(); i++) {
            WeakReference<PviewConferenceRequest> wf = mCallBacks.get(i);
            if (wf != null && wf.get() != null) {
                if (wf.get() == callback) {
                    mCallBacks.remove(wf);
                    return;
                }
            }
        }
    }

    public native boolean initialize(VideoJni request);

    public native void unInitialize();

    /**
     * @param nUserID    目标用户ID
     * @param szDeviceID nUserID的视频设备ID
     * @return None
     * @brief 打开视频设备
     */
    public native void VideoOpenDevice(long nUserID, String szDeviceID);

    /**
     * @param nUserID    目标用户ID
     * @param szDeviceID nUserID的视频设备ID
     * @return None
     * @brief 关闭视频设备 触发OnStartSendVideo
     */
    public native void VideoCloseDevice(long nUserID, String szDeviceID);

    /**
     * 启用摄像头设备, 触发OnStartSendVideo
     *
     * @param szDeviceID
     * @param bInuse
     */
    public native void EnableVideoDev(String szDeviceID, int bInuse);

    /**
     * 设置其他用户视频采集参数
     *
     * @param szDevID
     * @param nSizeIndex
     * @param nFrameRate
     * @param nBitRate
     */
    public native void VideoSetLocalCapParam(String szDevID, int nSizeIndex, int nFrameRate, int nBitRate);

    public native void RtmpAddVideo(long userID, String szDeviceID, int pos);

    public native void RtmpSetSubVideoPosRation(long userID, String szDeviceID, double x, double y, double width, double height);

    public native void RtmpSetH264Sei(String szSei, String szSeiExt);

    private void OnSetSei(long operUserId, String sei) {
        PviewLog.jniCall("OnSetSei", "operUserId : " + operUserId + " | sei : " + sei);
        if (GlobalConfig.trunOnCallback) {
            for (int i = 0; i < mCallBacks.size(); i++) {
                WeakReference<PviewConferenceRequest> wf = mCallBacks.get(i);
                if (wf != null && wf.get() != null) {
                    wf.get().OnSetSei(operUserId, sei);
                }
            }
        }
    }
}
