package com.wushuangtech.api;


import android.util.LongSparseArray;

import com.wushuangtech.library.GlobalConfig;
import com.wushuangtech.library.GlobalHolder;
import com.wushuangtech.utils.PviewLog;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;


public class ExternalVideoModule implements VideoSender {

    public enum VideoAdjustmentMode
    {
        VIDEO_ADJ_PREFER_CLARITY,   // 画质优先
        VIDEO_ADJ_PREFER_CONTINUITY // 流畅度优先
    }

    public static class VideoRecvLen {
        public int recvLen;
        public int udpRecvLen;
        public int fecVecSize;
    }

    public static class VideoStatistics {
        public int recvSize;
        public int recvFrames;
        public int lostFrames;
        public int recvBitrate;
        public int recvFramerate;
    }

    private static ExternalVideoModule mExternalVideoModule;
    private WeakReference<ExternalVideoModuleCallback> mCallback;
    private LongSparseArray<VideoStatistics> mVideoStatistics = new LongSparseArray<>();
    private LongSparseArray<VideoRecvLen> mVideoRecvLenStatistics = new LongSparseArray<>();

    private int encodedVideoSize = 0;
    private int encodedVideoFrameCount = 0;

    /**
     * 获取api对象实例，singleton
     *
     * @return 对象实例
     */
    public static synchronized ExternalVideoModule getInstance() {
        if (mExternalVideoModule == null) {
            synchronized (ExternalVideoModule.class) {
                if (mExternalVideoModule == null) {
                    mExternalVideoModule = new ExternalVideoModule();
                    mExternalVideoModule.Initialize(mExternalVideoModule);
                }
            }
        }
        return mExternalVideoModule;
    }

    /**
     * 设置call对象实例，与视频处理模块关联
     *
     * @param callback 视频处理模块对象
     */
    public void setExternalVideoModuleCallback(ExternalVideoModuleCallback callback) {
        mCallback = new WeakReference<>(callback);
    }

    /**
     * 设置视频发送缓冲区大小
     *
     * @param duration 缓冲区大小，单位毫秒
     */
    public void setMaxBufferDuration(int duration) {
        SetMaxBufferDuration(duration);
    }

    /**
     * 获取视频发送缓冲区大小
     *
     * @return 缓冲区大小，单位毫秒
     */
    public int getBufferDuration() {
        return GetBufferDuration();
    }

    /**
     * 获取发送总帧数
     *
     * @return 帧数
     */
    public int getSentFrameCount() {
        return GetSentFrameCount();
    }

    /**
     * 获取视频发送大小
     *
     * @return 字节数
     */
    public int getTotalSendBytes() {
        return GetTotalSendBytes();
    }

    /**
     * 获取视频接收大小
     *
     * @return 字节数
     */
    public int getTotalRecvBytes() {
        return GetTotalRecvBytes();
    }

    /**
     * 获取视频流控大小
     *
     * @return 字节数
     */
    public int getflowCtrlBytes() {
        return GetFlowCtrlBytes();
    }

    /**
     * 获取视频流控帧数
     *
     * @return 帧数
     */
    public int getFlowCtrlFrameCount() {
        return GetFlowCtrlFrameCount();
    }

    /**
     * 获取视频编码字节数
     *
     * @return 字节数
     */
    public int getEncodeDataSize() {
        /*
        if (mCallback != null && mCallback.get() != null) {
            return mCallback.get().getEncodeDataSize();
        }
        return 0;
        */
        return encodedVideoSize;
    }

    /**
     * 获取视频编码帧数
     *
     * @return 帧数
     */
    public int getEncodeFrameCount() {
        /*
        if (mCallback != null && mCallback.get() != null) {
            return mCallback.get().getEncodeFrameCount();
        }
        return 0;
        */
        return encodedVideoFrameCount;
    }


    /**
     * 获取视频采集帧数
     *
     * @return 帧数
     */
    public int getCaptureFrameCount() {
        if (mCallback != null && mCallback.get() != null) {
            return mCallback.get().getCaptureFrameCount();
        }
        return 0;
    }

    /**
     * 获取视频解码帧数
     *
     * @return 帧数
     */
    public int getDecodeFrameCount() {
        if (mCallback != null && mCallback.get() != null) {
            return mCallback.get().getDecodeFrameCount();
        }
        return 0;
    }

    /**
     * 获取视频播放帧数
     *
     * @return 帧数
     */
    public int getRenderFrameCount() {
        if (mCallback != null && mCallback.get() != null) {
            return mCallback.get().getRenderFrameCount();
        }
        return 0;
    }

    public int getRTT() {
        return GetRTT();
    }

    public int getRecvDataErrorTimes() {
        return GetRecvDataErrorTimes();
    }

    public LongSparseArray<VideoStatistics> getVideoStatistics() {
        mVideoStatistics.clear();
        GetVideoStatistics();

        return mVideoStatistics;
    }

    public LongSparseArray<VideoRecvLen> getVideoRecvLenStatistics() {
        mVideoRecvLenStatistics.clear();
        GetVideoRecvLenStatistics();

        return mVideoRecvLenStatistics;
    }

    public void setVideoAdjustmentMode(VideoAdjustmentMode mode) {
        PreferVideoQuality(mode==VideoAdjustmentMode.VIDEO_ADJ_PREFER_CLARITY);
    }

    private native boolean Initialize(ExternalVideoModule module);

    private native void Uninitialize();

    private native void SetMaxBufferDuration(int duration);

    private native int GetBufferDuration();

    private native int GetSentFrameCount();

    private native int GetTotalSendBytes();

    private native int GetTotalRecvBytes();

    private native int GetFlowCtrlBytes();

    private native int GetFlowCtrlFrameCount();

    private native void PushEncodedVideoData(byte[] nal, int len, int videoType, int width, int height);

    private native void SendEncodedVideoFrame();

    private native void PushDualEncodedVideoData(byte[] nal, int len, int videoType, int width, int height);
    private native void SendDualEncodedVideoFrame();

    private native int GetRTT();

    private native int GetRecvDataErrorTimes();

    private native void GetVideoStatistics();

    private native void GetVideoRecvLenStatistics();

    private native void PreferVideoQuality(boolean arg);

    public void uninitialize(){
        Uninitialize();
    }

    // for debug
    private int pushDataBegin_count = 0;
    private int pushDataEnd_count = 0;
    public int getPushDataBeginCount() {
        return pushDataBegin_count;
    }
    public int getPushDataEndCount() {
        return pushDataEnd_count;
    }
    public boolean isCapturing() {
        if (mCallback == null || mCallback.get() == null)
        {
            return false;
        }

        return mCallback.get().isCapturing();
    }

    /**
     * 发送编码后的视频数据 @see VideoSender
     */
    @Override
    public void pushEncodedVideoData(ArrayList<byte[]> h264_nal, ExternalVideoModuleCallback.VideoFrameType frameType, int videoWidth, int videoHeight) {
        pushDataBegin_count++;
        encodedVideoFrameCount++;
        int type;
        if (frameType == ExternalVideoModuleCallback.VideoFrameType.FRAMETYPE_I) {
            type = 1;
        } else {
            type = 0;
        }

        for (byte[] data : h264_nal) {
            encodedVideoSize += data.length;
            PushEncodedVideoData(data, data.length, type, videoWidth, videoHeight);
        }

        SendEncodedVideoFrame();
        pushDataEnd_count++;
    }

    @Override
    public void pushDualEncodedVideoData(ArrayList<byte[]> h264_nal, ExternalVideoModuleCallback.VideoFrameType frameType, int videoWidth, int videoHeight) {
        int type;
        if (frameType == ExternalVideoModuleCallback.VideoFrameType.FRAMETYPE_I) {
            type = 1;
        } else {
            type = 0;
        }

        for (byte[] data : h264_nal) {
            PushDualEncodedVideoData(data, data.length, type, videoWidth, videoHeight);
        }

        SendDualEncodedVideoFrame();
    }

    public int[] getEncodeSize() {
        if (mCallback != null && mCallback.get() != null) {
            return mCallback.get().getEncodeSize();
        }
        return null;
    }

    private boolean StartCapture()
    {
        if (mCallback == null || mCallback.get() == null)
        {
            return false;
        }

        GlobalConfig.mIsCapturing.set(true);
        return mCallback.get().startCapture();
    }

    private boolean StopCapture()
    {
        if (mCallback == null || mCallback.get() == null)
        {
            return false;
        }

        GlobalConfig.mIsCapturing.set(false);
        return mCallback.get().stopCapture();
    }

    private boolean StartDualCapture()
    {
        if (mCallback == null || mCallback.get() == null)
        {
            return false;
        }

        return mCallback.get().startDualCapture();
    }

    private boolean StopDualCapture()
    {
        if (mCallback == null || mCallback.get() == null)
        {
            return false;
        }

        return mCallback.get().stopDualCapture();
    }

    private void ReceiveVideoData(byte[] data, String devID, long timeStamp, int width, int height, int frameType) {
        if (mCallback != null && mCallback.get() != null) {
            ExternalVideoModuleCallback.VideoFrameType type;
            if (frameType == 1) {
                type = ExternalVideoModuleCallback.VideoFrameType.FRAMETYPE_SPS_PPS;
            } else if (frameType == 2) {
                type = ExternalVideoModuleCallback.VideoFrameType.FRAMETYPE_I;
            } else if (frameType == 3) {
                type = ExternalVideoModuleCallback.VideoFrameType.FRAMETYPE_P;
            } else {
                type = ExternalVideoModuleCallback.VideoFrameType.FRAMETYPE_INVALID;
            }

            mCallback.get().receiveVideoData(data, devID, timeStamp, width, height, type);
        }
    }

    private void OnReportVideoStatistics(long nUserID, int recvSize, int recvFrames, int lostFrames , int bitrate , int fps) {
        VideoStatistics vs = new VideoStatistics();
        vs.recvSize = recvSize;
        vs.recvFrames = recvFrames;
        vs.lostFrames = lostFrames;
        vs.recvBitrate = bitrate;
        vs.recvFramerate = fps;
        mVideoStatistics.append(nUserID, vs);

    }

    private void OnReportVideoRecvLenStatistics(long nUserID, int recvLen, int udpRecvLen, int fecVecSize) {
        VideoRecvLen vrl = new VideoRecvLen();
        vrl.recvLen = recvLen;
        vrl.udpRecvLen = udpRecvLen;
        vrl.fecVecSize = fecVecSize;
        mVideoRecvLenStatistics.append(nUserID, vrl);
    }

    private void OnSignalDisconnect() {
        encodedVideoSize = 0;
        encodedVideoFrameCount = 0;
    }

    private void ReceiveSeiData(byte[] sei_content, String devID) {
        String s = new String(sei_content , Charset.forName("UTF-8"));
        long uid = GlobalHolder.getInstance().getUserByDeviceID(devID);
        PviewLog.jniCall("ReceiveSeiData", "sei_content : " + s + " | devID : " + devID
                + " | uid : " + uid);
        JniWorkerThread mJniWorkerThread = GlobalHolder.getInstance().getWorkerThread();
        mJniWorkerThread.sendMessage(JniWorkerThread.JNI_CALL_BACK_ON_RECEIVE_SEI_DATA, new Object[]{s, uid});
    }

    private void RequestIFrame() {
        if (mCallback == null || mCallback.get() == null)
        {
            return;
        }

        mCallback.get().requestIFrame();
    }

    private void RequestDualIFrame() {
        if (mCallback == null || mCallback.get() == null)
        {
            return;
        }

        mCallback.get().requestDualIFrame();
    }

    private int MaxFps() {
        if (mCallback == null || mCallback.get() == null)
        {
            return 0;
        }

        return mCallback.get().maxFps();
    }

    private void ChangeFps(int fps) {
        if (mCallback == null || mCallback.get() == null)
        {
            return;
        }

        mCallback.get().changeFps(fps);
    }

    private int MaxBitrate() {
        if (mCallback == null || mCallback.get() == null)
        {
            return 0;
        }

        return mCallback.get().maxBitrate();
    }

    private void ChangeBitrate(int bitrate) {
        if (mCallback == null || mCallback.get() == null)
        {
            return;
        }

        mCallback.get().changeBitrate(bitrate);
    }
}
