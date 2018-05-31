package com.wushuangtech.api;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * 音视频录制.
 */

public class AVRecorderModule implements VideoSender,AudioSender{
    private static AVRecorderModule mAVRecorderModule;
    private WeakReference<ExternalVideoModuleCallback> mVideoCallback;
    private WeakReference<ExternalAudioModuleCallback> mAudioCallback;
    private WeakReference<AVRecorderModuleCallback> mCallback;

    /**
     *  获取api对象实例，singleton
     *
     *  @return 对象实例
     */
    public static synchronized AVRecorderModule getInstance() {
        if (mAVRecorderModule == null) {
            synchronized (ExternalRtmpPublishModule.class) {
                if (mAVRecorderModule == null) {
                    mAVRecorderModule = new AVRecorderModule();
                    mAVRecorderModule.Initialize(mAVRecorderModule);
                }
            }
        }
        return mAVRecorderModule;
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
    public void setmAVRecorderModuleCallback(AVRecorderModuleCallback callback)
    {
        mCallback = new WeakReference<AVRecorderModuleCallback>(callback);
    }
    /**
     * 由音频模块调用，发送音频数据
     *
     * @param data  音频数据AAC
     */
    public void pushEncodedAudioData(byte[] data) {
        WriteEncodedAudioData(data,data.length);
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
    public void pushEncodedVideoData(ArrayList<byte[]> h264_nal, ExternalVideoModuleCallback.VideoFrameType frameType, int videoWidth, int videoHeight) {
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
            WriteEncodedVideoData(data, data.length, type, videoWidth, videoHeight);
        }
    };

    public void pushEncodedVideoData(byte[] data, ExternalVideoModuleCallback.VideoFrameType frameType, int videoWidth, int videoHeight) {
        int type;
        if (frameType == ExternalVideoModuleCallback.VideoFrameType.FRAMETYPE_I) {
            type = 1;
        } else {
            type = 0;
        }
        WriteEncodedVideoData(data, data.length, type, videoWidth, videoHeight);
    }

    public void pushDualEncodedVideoData(ArrayList<byte[]> h264_nal, ExternalVideoModuleCallback.VideoFrameType frameType, int videoWidth, int videoHeight) {
    }

    public boolean startRecorde(String cPathName){
        return StartRecorde(cPathName);
    };

    public boolean stopRecorde(){
        return StopRecorde();
    };

    private native boolean Initialize(AVRecorderModule module);
    private native void Uninitialize();
    private native boolean StartRecorde(String sPathName);
    private native boolean StopRecorde();
    private native void WriteEncodedVideoData(byte[] nal, int len, int videoType, int width, int height);
    private native void WriteEncodedAudioData(byte[] data, int len);
    private void RecordeStatus( int type)
    {

    }
}
