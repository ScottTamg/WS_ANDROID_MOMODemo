package com.wushuangtech.api;


import com.wushuangtech.library.GlobalHolder;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Rtmp 推流接口
 */
public class ExternalRtmpPublishModule implements VideoSender,AudioSender {

    private static ExternalRtmpPublishModule mExternalRtmpPublishModule;
    private WeakReference<ExternalVideoModuleCallback> mVideoCallback;
    private WeakReference<ExternalAudioModuleCallback> mAudioCallback;
    private WeakReference<ExternalRtmpPublishModuleCallback> mCallback;

    /**
     *  获取api对象实例，singleton
     *
     *  @return 对象实例
     */
    public static synchronized ExternalRtmpPublishModule getInstance() {
        if (mExternalRtmpPublishModule == null) {
            synchronized (ExternalRtmpPublishModule.class) {
                if (mExternalRtmpPublishModule == null) {
                    mExternalRtmpPublishModule = new ExternalRtmpPublishModule();
                    mExternalRtmpPublishModule.Initialize(mExternalRtmpPublishModule);
                }
            }
        }
        return mExternalRtmpPublishModule;
    }

    /**
     * 设置视频处理模块，例如MyVideo或用户自定义的视频模块
     *
     * @param callback  视频处理模块
     */
    public void setExternalVideoModuleCallback(ExternalVideoModuleCallback callback)
    {
        mVideoCallback = new WeakReference<ExternalVideoModuleCallback>(callback);
    }

    /**
     * 设置音频处理模块，例如MyAudio或用户自定义的音频模块
     *
     * @param callback  音频处理模块
     */
    public void setExternalAudioModuleCallback(ExternalAudioModuleCallback callback)
    {
        mAudioCallback = new WeakReference<ExternalAudioModuleCallback>(callback);
    }

    /**
     * 设置推流状态回调对象
     *
     * @param callback  推流状态接收对象
     */
    public void setExternalRtmpPublishModuleCallback(ExternalRtmpPublishModuleCallback callback)
    {
        mCallback = new WeakReference<ExternalRtmpPublishModuleCallback>(callback);
    }

    /**
     * 开始推流
     *
     * @param sRtmpUrl  推流地址
     */
    public boolean startPublish(String sRtmpUrl)
    {
        if (mVideoCallback == null
                || mVideoCallback.get() == null
                || mAudioCallback == null
                || mAudioCallback.get() == null)
        {
            return false;
        }

        boolean ret = StartPublish(sRtmpUrl);

        if (ret)
        {
            mVideoCallback.get().startCapture();
            mAudioCallback.get().startCapture();
        }

        return ret;

    }

    /**
     * 停止推流
     */
    public boolean stopPublish()
    {
        if (mVideoCallback == null
                || mVideoCallback.get() == null
                || mAudioCallback == null
                || mAudioCallback.get() == null)
        {
            return false;
        }

        mVideoCallback.get().stopCapture();
        mAudioCallback.get().stopCapture();
        StopPublish();

        return true;
    }

    /**
     * 由音频模块调用，发送音频数据
     *
     * @param data  音频数据AAC
     */
    public void pushEncodedAudioData(byte[] data)
    {
        if (mIsPause) {
            return ;
        }

        PushEncodedAudioData(data, data.length);
    }

    @Override
    public void sendSRData(byte[] data, int len) {

    }

    @Override
    public void sendNACKData(byte[] data, int len, long userid) {

    }

    /**
     * 由视频模块调用，发送视频数据
     *
     * @param h264_nal      视频数据h.264 nal数组
     * @param frameType     帧类型
     * @param videoWidth    视频宽
     * @param videoHeight   视频高
     */
    public void pushEncodedVideoData(ArrayList<byte[]> h264_nal, ExternalVideoModuleCallback.VideoFrameType frameType, int videoWidth, int videoHeight)
    {
        if (mIsPause) {
            return ;
        }

        int type;
        if (frameType == ExternalVideoModuleCallback.VideoFrameType.FRAMETYPE_I)
        {
            type = 1;
        }
        else
        {
            type = 0;
        }

        for (byte[] data : h264_nal)
        {
            PushEncodedVideoData(data, data.length, type, videoWidth, videoHeight);
        }

        SendEncodedVideoFrame();
    }

    public void pushDualEncodedVideoData(ArrayList<byte[]> h264_nal, ExternalVideoModuleCallback.VideoFrameType frameType, int videoWidth, int videoHeight)
    {

    }

    private native boolean Initialize(ExternalRtmpPublishModule module);
    private native void Uninitialize();
    private native boolean StartPublish(String sRtmpUrl);
    private native boolean StopPublish();
    private native void PushEncodedVideoData(byte[] nal, int len, int videoType, int width, int height);
    private native void SendEncodedVideoFrame();
    private native void PushEncodedAudioData(byte[] data, int len);

    private boolean StartCapture()
    {
        boolean bRet = false;
        if (mVideoCallback == null
                || mVideoCallback.get() == null
                || mAudioCallback == null
                || mAudioCallback.get() == null)
        {
            return bRet;
        }

        bRet = mVideoCallback.get().startCapture();
        if(bRet)
        {
            bRet = mAudioCallback.get().startCapture();
        }
        return bRet;
    }

    private boolean StopCapture()
    {
        if (mVideoCallback == null
                || mVideoCallback.get() == null
                || mAudioCallback == null
                || mAudioCallback.get() == null)
        {
            return false;
        }

        mVideoCallback.get().stopCapture();
        mAudioCallback.get().stopCapture();

        return true;
    }

    private void ReceiveRtmpStatus( int type)
    {
        if (mCallback != null && mCallback.get() != null)
        {
            ExternalRtmpPublishModuleCallback.RtmpErrorType errortype;
            if (type == 0)
            {
                errortype = ExternalRtmpPublishModuleCallback.RtmpErrorType.RtmpErrorType_InitError;
            }
            else if (type == 1)
            {
                errortype = ExternalRtmpPublishModuleCallback.RtmpErrorType.RtmpErrorType_OpenError;
            }
            else if (type == 2)
            {
                errortype = ExternalRtmpPublishModuleCallback.RtmpErrorType.RtmpErrorType_AudioNoBuf;
            }
            else if (type == 3)
            {
                errortype = ExternalRtmpPublishModuleCallback.RtmpErrorType.RtmpErrorType_VideoNoBuf;
            }
            else
            {
                errortype = ExternalRtmpPublishModuleCallback.RtmpErrorType.RtmpErrorType_LinkFailed;
            }
            mCallback.get().receiveRtmpStatus(errortype);
        }

        GlobalHolder.getInstance().notifyCHRTMPStatus(type);
    }

    //********************************************
    private volatile boolean mIsPause;

    public void setIsPause(boolean mIsPause) {
        this.mIsPause = mIsPause;
    }

    static {
        System.loadLibrary("rtmp");
    }
}
