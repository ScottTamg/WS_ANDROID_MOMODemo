package com.wushuangtech.videocore;

import android.content.Context;

import com.faceunity.wrapper.faceunity;
import com.wushuangtech.api.ExternalVideoModuleCallback;
import com.wushuangtech.api.VideoSender;
import com.wushuangtech.utils.CheckCameraUtils;
import com.wushuangtech.utils.PviewLog;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class MyVideoApi implements ExternalVideoModuleCallback {

    private static MyVideoApi mVideoApi;
    private List<WeakReference<VideoSender>> mVideoSenders = new ArrayList<>();

    private WeakReference<VideoStatReportCallback> videoStatReportCallback;
    private VideoConfig mConfig = new VideoConfig();
    private int encodeDataSize = 0;
    private int encodeFrameCount = 0;
    private int captureFrameCount = 0;
    private int enc_width = 0;
    private int enc_height = 0;
    public boolean isCanSwitchCamera;//是否能切换摄像头

    private boolean isCapturing = false;

    public class VideoConfig implements Cloneable {
        /*
         * 视频宽
         */
        public int videoWidth;
        /*
         * 视频高
         */
        public int videoHeight;
        /*
         * 视频帧率
         */
        public int videoFrameRate;
        /*
         * I帧间隔，决定一个gop的大小，单位秒
         */
        public int videoMaxKeyframeInterval;
        /*
         * 视频码率
         */
        public int videoBitRate;
        /*
         * 是否启用前置摄像头
         */
        public boolean enabeleFrontCam;
        public boolean openflash;

        public VideoConfig() {
            videoMaxKeyframeInterval = 1;
            enabeleFrontCam = true;
            videoFrameRate = 15;
            videoBitRate = 400 * 1000;
            videoWidth = 360;
            videoHeight = 640;
            openflash = false;
            if (CheckCameraUtils.hasFrontFacingCamera()) {
                enabeleFrontCam = true;
                isCanSwitchCamera = true;
            } else {
                enabeleFrontCam = false;
                isCanSwitchCamera = false;
            }

        }

        public Object clone() {
            Object o = null;
            try {
                o = super.clone();
            } catch (CloneNotSupportedException e) {
                System.out.println(e.toString());
            }
            return o;
        }
    }

    /*
     * 获取视频模块单例
     *
     * @return MyVideoApi单例
     */
    public static synchronized MyVideoApi getInstance() {
        if (mVideoApi == null) {
            synchronized (MyVideoApi.class) {
                if (mVideoApi == null) {
                    mVideoApi = new MyVideoApi();
                }
            }
        }
        return mVideoApi;
    }

    public void initFUBeautify(Context context) {

        try {
            InputStream v3 = context.getAssets().open("v3.bundle");
            byte[] v3Data = new byte[v3.available()];
            v3.read(v3Data);
            v3.close();
            faceunity.fuSetup(v3Data, null, authpack.A());

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /*
     * 设置视频参数
     *
     * @param config 视频参数
     */
    public void setVideoConfig(VideoConfig config) {
        boolean bNeedReset = false;
        if (mConfig.videoWidth != config.videoWidth
                || mConfig.videoHeight != config.videoHeight
                || mConfig.videoBitRate != config.videoBitRate
                || mConfig.videoFrameRate != config.videoFrameRate
                || mConfig.videoMaxKeyframeInterval != mConfig.videoMaxKeyframeInterval) {
            bNeedReset = true;
        }
        mConfig = config;
        PviewLog.i("Set Video Config , width : " + config.videoWidth + " | height : " + config.videoHeight);
        if (bNeedReset) {
            LocaSurfaceView.getInstance().Reset();
        }
    }

    public VideoConfig getVideoConfig() {
        Object o = mConfig.clone();
        if (o != null) {
            return (VideoConfig) o;
        } else {
            PviewLog.funEmptyError("getVideoConfig", "clone getVideoConfig", "clone failed!");
            return null;
        }
    }

    /**
     * @param bswitch true-front false-back
     */
    public boolean switchCarmera(boolean bswitch) {
        mConfig.enabeleFrontCam = bswitch;
        if (bswitch) {
            mConfig.openflash = false;
        }
        return LocaSurfaceView.getInstance().switchCamera(bswitch);
    }

    public void encodedVideoFrame(byte[] videoFrame, ExternalVideoModuleCallback.VideoFrameType frameType, int width, int height) {
        encodeFrameCount++;
        captureFrameCount++;
        encodeDataSize += videoFrame.length;
        // Log.e("推送","流媒体推送==================1");
        ArrayList<byte[]> h264_nals = new ArrayList<>();
        h264_nals.add(videoFrame);
        for (WeakReference<VideoSender> videoSender : mVideoSenders) {
            if (videoSender.get() != null) {
                videoSender.get().pushEncodedVideoData(h264_nals, frameType, width, height);
            }
        }
    }

    public void encodedDualVideoFrame(byte[] videoFrame, ExternalVideoModuleCallback.VideoFrameType frameType, int width, int height,boolean isDualVideo)
    {
        encodeFrameCount++;
        captureFrameCount++;
        encodeDataSize += videoFrame.length;
        ArrayList<byte[]> h264_nals = new ArrayList<>();
        h264_nals.add(videoFrame);
        for(WeakReference<VideoSender> videoSender : mVideoSenders)
        {
            if (videoSender.get() != null)
            {
                if(isDualVideo) {
                    videoSender.get().pushDualEncodedVideoData(h264_nals, frameType, width, height);
                }else{
                    videoSender.get().pushEncodedVideoData(h264_nals, frameType, width, height);
                }
            }
        }
    }

    /**
     * 开启本地预览
     *
     * @return always true
     */
    public boolean startPreview() {
        return LocaSurfaceView.getInstance().setPreview(true);
    }

    /**
     * 停止本地预览
     *
     * @return always true
     */
    public boolean stopPreview() {
        return LocaSurfaceView.getInstance().setPreview(false);
    }

    //////////// implementation of ExternalVideoModuleCallback //////////

    /**
     * @return always true
     */
    public boolean startCapture() {
        isCapturing = true;
        LocaSurfaceView.getInstance().setBmEncode(true);
        return true;
    }

    /**
     * @return always true
     */
    public boolean stopCapture() {
        isCapturing = false;
        LocaSurfaceView.getInstance().setBmEncode(false);
        return true;
    }

    public boolean startDualCapture()
    {
        isCapturing = true;
        LocaSurfaceView.getInstance().setDualBmEncode(true);
        return true;
    }

    public boolean stopDualCapture()
    {
        isCapturing = false;
        LocaSurfaceView.getInstance().setDualBmEncode(false);
        return true;
    }

    public void receiveVideoData(byte[] data, String devID, long timeStamp, int width, int height, VideoFrameType frameType) {
        //operaleData(data);
        // 上报远端解码第一帧
        VideoNewControl.firstDecoder(data, devID, timeStamp, width, height, frameType);
    }

    /**
     * @param sender 编码数据接收对象
     */
    public void addVideoSender(VideoSender sender) {
        for (WeakReference<VideoSender> videoSender : mVideoSenders) {
            if (videoSender.get() == sender) {
                return;
            }
        }

        WeakReference<VideoSender> videoSender = new WeakReference<>(sender);
        mVideoSenders.add(videoSender);
    }

    /**
     * @param sender 编码数据接收对象
     */
    public void removeVideoSender(VideoSender sender) {
        for (WeakReference<VideoSender> videoSender : mVideoSenders) {
            if (videoSender.get() == sender) {
                mVideoSenders.remove(videoSender);
                break;
            }
        }
    }

    /**
     * 获取编码视频字节数
     *
     * @return 字节数
     */
    public int getEncodeDataSize() {
        return encodeDataSize;
    }

    /**
     * 编码的帧数
     *
     * @return 帧数
     */
    public int getEncodeFrameCount() {
        return encodeFrameCount;
    }

    /**
     * 采集的帧数
     *
     * @return 帧数
     */
    public int getCaptureFrameCount() {
        return captureFrameCount;
    }

    /**
     * 解码的帧数
     *
     * @return 帧数
     */
    public int getDecodeFrameCount() {
        return RemotePlayerManger.getInstance().getDecFrameCount();
    }

    /**
     * 显示的帧数
     *
     * @return 帧数
     */
    public int getRenderFrameCount() {
        return RemotePlayerManger.getInstance().getRenFrameCount();
    }

    /**
     * 获取编码视频尺寸
     *
     * @return width at index of 0，height at index of 1
     */
    public int[] getEncodeSize() {
        int[] size = new int[2];
        size[0] = enc_width;
        size[1] = enc_height;

        return size;
    }

    public void updateEncodeSize(int encWidth, int encHeight) {
        enc_width = encWidth;
        enc_height = encHeight;
    }

    void updateDecodeSize(int decWidth, int decHeight, RemoteSurfaceView surface) {
        String devId = RemotePlayerManger.getInstance().getDevId(surface);
        if (devId != null) {
            if (videoStatReportCallback != null && videoStatReportCallback.get() != null) {
                videoStatReportCallback.get().UpdateRemoteVideoSize(devId, decWidth, decHeight);
            }
        }
    }

    public byte[] getLocalBuffer() {
        return LocaSurfaceView.getInstance().getLocalBuffer();
    }

    public int getRenderWidth() {
        return LocaSurfaceView.getInstance().getRenderWidth();
    }

    public int getRenderHeight() {
        return LocaSurfaceView.getInstance().getRenderHeight();
    }

    public boolean isCapturing() {
        return isCapturing;
    }

    public void requestIFrame() {
        // TODO
    }

    public void requestDualIFrame() {
        // TODO
    }

    public int maxFps() {
        return getVideoConfig().videoFrameRate;
    }

    public void changeFps(int fps) {
//        LocaSurfaceView.getInstance().changeFps(fps);
    }

    public int maxBitrate() {
        return getVideoConfig().videoBitRate;
    }

    public void changeBitrate(int bitrate) {
//        LocaSurfaceView.getInstance().changeBitrate(bitrate);
    }
}
