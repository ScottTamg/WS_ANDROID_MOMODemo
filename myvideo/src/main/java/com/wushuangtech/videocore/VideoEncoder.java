package com.wushuangtech.videocore;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;

import com.wushuangtech.api.ExternalVideoModuleCallback;
import com.wushuangtech.bean.ConfVideoFrame;
import com.wushuangtech.utils.PviewLog;

import java.nio.ByteBuffer;
import java.util.Vector;

/**
 * Created by Administrator on 2017/8/29.
 */

public class VideoEncoder {
    /**
     * I420图像格式g
     */
    public static final int FORMAT_I420 = 1;
    /**
     * NV12图像格式
     */
    public static final int FORMAT_NV12 = 2;
    /**
     * NV21图像格式
     */
    public static final int FORMAT_NV21 = 3;
    /**
     * RGBA图像格式
     */
    public static final int FORMAT_RGBA = 4;

    public static synchronized VideoEncoder getInstance() {
        VideoEncoder mencoder = null;
        synchronized (VideoEncoder.class) {
            mencoder = new VideoEncoder();
            if (mencoder != null) {
                mencoder.mlencoder = mencoder.Initialize(mencoder);
            }
        }
        return mencoder;
    }

    private boolean mIsDualVideo=false;
    private boolean mEnableSoftEncoder = false;
    private long mlencoder = 0;
    private String mMime = "video/avc";
    private MediaCodecInfo mMediaCodecInfo;
    private int mVideoColorFormat;
    private int mDstColorFormat;
    private int mSrcColorFormat;
    private MediaCodec mMediaEncoder = null;
    private MediaFormat mMediaFormat = null;
    private byte[] sps_pps_byte;
    private int sps_pps_len = 0;
    private long mPresentTimeUs;
    private int mWidth = 480;
    private int mHeight = 640;

    private byte[] sei_byte;
    private Object  sei_sync=new Object();

    public void setEnableSoftEncoder(boolean mEnableSoftEncoder) {
        this.mEnableSoftEncoder = mEnableSoftEncoder;
    }

    public void setDualVideo(boolean isDualVideo){
        this.mIsDualVideo=isDualVideo;
    }

    public void insertH264SeiContent(byte[] sei_content){
        synchronized (sei_sync) {
            if(sei_content==null){
                sei_byte=null;
            }else {
                int sei_len=SeiPacket.get_sei_packet_size(sei_content.length);
                sei_byte=new byte[sei_len];
                SeiPacket.fill_sei_packet(sei_byte,1,sei_content,sei_content.length);
            }
        }
    }

    private VideoEncoder() {
        mVideoColorFormat = chooseVideoEncoder();
        if (mVideoColorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar) {
            mDstColorFormat = FORMAT_NV12;
        } else {
            mDstColorFormat = FORMAT_I420;
        }
        if (mEnableSoftEncoder || Build.VERSION.SDK_INT <= 15) {
            mDstColorFormat = FORMAT_NV12;
        }
        mSrcColorFormat = FORMAT_RGBA;
    }


    public void setResolution(int width, int height) {
        mWidth = width;
        mHeight = height;
    }

    public boolean start() throws Exception {
        MyVideoApi.VideoConfig config = MyVideoApi.getInstance().getVideoConfig();
        int videoFrameRate=0,videoBitRate=0,videoMaxKeyframeInterval=0;
        if(mIsDualVideo){
            videoBitRate=config.videoBitRate/4;
            videoFrameRate=config.videoFrameRate;
            videoMaxKeyframeInterval=config.videoMaxKeyframeInterval;
        }else{
            videoBitRate=config.videoBitRate;
            videoFrameRate=config.videoFrameRate;
            videoMaxKeyframeInterval=config.videoMaxKeyframeInterval;
        }

        setEncoderResolution(mlencoder, mWidth, mHeight);
        mPresentTimeUs = System.nanoTime() / 1000;
        if (mEnableSoftEncoder || Build.VERSION.SDK_INT <= 15) {
            if (MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar == mVideoColorFormat) {
                return openSoftEncoder(mlencoder, mWidth, mHeight, videoFrameRate, videoBitRate, videoMaxKeyframeInterval * 15, FORMAT_NV12);
            } else {
                return openSoftEncoder(mlencoder, mWidth, mHeight, videoFrameRate, videoBitRate, videoMaxKeyframeInterval * 15, FORMAT_I420);
            }
        } else {
            return openHardwareEncoder(mWidth, mHeight, videoFrameRate, videoBitRate, videoMaxKeyframeInterval);
        }
    }

    public boolean externalEncodeStart() throws Exception {
        MyVideoApi.VideoConfig config = MyVideoApi.getInstance().getVideoConfig();
        setEncoderResolution(mlencoder, mWidth, mHeight);
        mPresentTimeUs = System.nanoTime() / 1000;
        if (mEnableSoftEncoder || Build.VERSION.SDK_INT <= 15) {
            return openSoftEncoder(mlencoder, mWidth, mHeight, config.videoFrameRate, config.videoBitRate, config.videoMaxKeyframeInterval * 15, FORMAT_I420);
        } else {
            return openHardwareEncoder(mWidth, mHeight, config.videoFrameRate, config.videoBitRate, config.videoMaxKeyframeInterval);
        }
    }

    public void onGetRgbaFrame(byte[] data, int width, int height) {
        long pts = System.nanoTime() / 1000 - mPresentTimeUs;
        byte[] yuvData;
        if (mVideoColorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar) {
            yuvData = RGBAToNV12(mlencoder, data, width, height, false, 180);
        } else {
            yuvData = RGBAToI420(mlencoder, data, width, height, false, 180);
        }
        PviewLog.wf("VideoEncoder onGetRgbaFrame yuvData : " + yuvData + " | data : " + data);
        if (yuvData != null) {
            PviewLog.wf("VideoEncoder onGetRgbaFrame yuvData : " + yuvData.length);
        }

        if (data != null) {
            PviewLog.wf("VideoEncoder onGetRgbaFrame data length : " + data.length);
        }

        if (yuvData != null) {
            if (mEnableSoftEncoder || Build.VERSION.SDK_INT <= 15) {
                encodeYuvFrame(mlencoder, yuvData, (int) pts);
            } else {
                HardwareEncodeYuvFrame(yuvData, pts);
            }
        }
    }

    public void externalGetRgbaFrame(byte[] yuvData) {
        long pts = System.nanoTime() / 1000 - mPresentTimeUs;
        PviewLog.wf("VideoEncoder onGetRgbaFrame yuvData : " + yuvData);
        if (yuvData != null) {
            PviewLog.wf("VideoEncoder onGetRgbaFrame yuvData : " + yuvData.length);
        }

        if (yuvData != null) {
            if (mEnableSoftEncoder || Build.VERSION.SDK_INT <= 15) {
                encodeYuvFrame(mlencoder, yuvData, (int) pts);
            } else {
                HardwareEncodeYuvFrame(yuvData, pts);
            }
        }
    }

    public void stop() {
        if (mEnableSoftEncoder || Build.VERSION.SDK_INT <= 15) {
            closeSoftEncoder(mlencoder);
        } else {
            closeHardwareEncoder();
        }
    }

    public boolean isMtk() {
        if (mMediaCodecInfo == null) {
            mMediaCodecInfo = chooseVideoEncoder(null);
        }
        if (mMediaCodecInfo.getName().contains("MTK")) {
            return true;
        }
        return false;
    }

    private MediaCodecInfo chooseVideoEncoder(String name) {
        int nbCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < nbCodecs; i++) {
            MediaCodecInfo mci = MediaCodecList.getCodecInfoAt(i);
            if (!mci.isEncoder()) {
                continue;
            }
            String[] types = mci.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mMime)) {
                    // Log.i("-----", String.format("vencoder %s types: %s", mci.getName(), types[j]));
                    if (name == null) {
                        return mci;
                    }
                    if (mci.getName().contains(name)) {
                        return mci;
                    }
                }
            }
        }
        return null;
    }

    // choose the right supported color format. @see below:
    private int chooseVideoEncoder() {
        mMediaCodecInfo = chooseVideoEncoder(null);
        int matchedColorFormat = 0;
        MediaCodecInfo.CodecCapabilities cc = mMediaCodecInfo.getCapabilitiesForType(mMime);
        for (int i = 0; i < cc.colorFormats.length; i++) {
            int cf = cc.colorFormats[i];
            Log.i("----", String.format("vencoder %s supports color fomart 0x%x(%d)", mMediaCodecInfo.getName(), cf, cf));
            // choose YUV for h.264, prefer the bigger one.
            // corresponding to the color space transform in onPreviewFrame
            if (cf >= cc.COLOR_FormatYUV420Planar && cf <= cc.COLOR_FormatYUV420SemiPlanar) {
                if (cf > matchedColorFormat) {
                    matchedColorFormat = cf;
                }
            }
        }

        return matchedColorFormat;
    }

    boolean openHardwareEncoder(int nWidth, int nHeight, int nFs, int nBitRate, int nGop) throws Exception {
        sps_pps_len = 0;
        mMediaEncoder = MediaCodec.createByCodecName(mMediaCodecInfo.getName());
        mMediaFormat = MediaFormat.createVideoFormat(mMime, nWidth, nHeight);
        mMediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, mVideoColorFormat);
        mMediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, nBitRate);
        mMediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, nFs);
        mMediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, nGop);
        PviewLog.d("openHardwareEncoder width : " + nWidth + " | height : " + nHeight
                + " | nFs : " + nFs + " | nBitRate : " + nBitRate + " | nGop : " + nGop);
        mMediaEncoder.configure(mMediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mMediaEncoder.start();
        return true;
    }

    public void HardwareEncodeYuvFrame(byte[] yuvFrame, long pts) {

        int inputBufferIndex = mMediaEncoder.dequeueInputBuffer(-1);
        if (inputBufferIndex >= 0) {
            ByteBuffer[] inputBuffers = mMediaEncoder.getInputBuffers();
            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
            inputBuffer.clear();
            inputBuffer.put(yuvFrame, 0, yuvFrame.length);
            //pts单位ms，硬件编码为us所以x1000
            mMediaEncoder.queueInputBuffer(inputBufferIndex, 0, yuvFrame.length, pts, 0);
        }
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex = mMediaEncoder.dequeueOutputBuffer(bufferInfo, 0);
        while (outputBufferIndex >= 0) {
            ByteBuffer[] outputBuffers = mMediaEncoder.getOutputBuffers();
            ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
            outputBuffer.position(bufferInfo.offset);

            byte[] outData = new byte[bufferInfo.size];
            outputBuffer.get(outData);

            byte[] sendData=null;
            byte nelkey=(byte)(outputBuffer.get(4)&0x1f);
            if(nelkey==5){
//                sendData= new byte[bufferInfo.size+sps_pps_len];
//                System.arraycopy(sps_pps_byte,0,sendData,0,sps_pps_len);
//                System.arraycopy(outData,0,sendData,sps_pps_len,bufferInfo.size);

                int indexs=0;
                synchronized (sei_sync) {
                    if(sei_byte!=null) {
                        sendData = new byte[bufferInfo.size + sps_pps_len+sei_byte.length];
                        System.arraycopy(sps_pps_byte, 0, sendData, 0, sps_pps_len);
                        indexs=sps_pps_len;
                        System.arraycopy(sei_byte, 0,sendData, indexs,sei_byte.length);
                        indexs+=sei_byte.length;
                    }else{
                        sendData = new byte[bufferInfo.size + sps_pps_len];
                        System.arraycopy(sps_pps_byte, 0, sendData, 0, sps_pps_len);
                        indexs=sps_pps_len;
                    }
                }
                System.arraycopy(outData, 0, sendData, indexs, bufferInfo.size);
            }else if(nelkey==7){
                sps_pps_len=bufferInfo.size-4;
                sps_pps_byte= new byte[sps_pps_len];
                System.arraycopy(outData,4,sps_pps_byte,0,sps_pps_len);
            }else{
                sendData= new byte[bufferInfo.size-4];
                System.arraycopy(outData,4,sendData,0,bufferInfo.size-4);
            }

            if(sendData!=null){
                if(nelkey==5) {
                    MyVideoApi.getInstance().encodedDualVideoFrame(sendData, ExternalVideoModuleCallback.VideoFrameType.FRAMETYPE_I, mWidth, mHeight,mIsDualVideo);
                }else{
                    MyVideoApi.getInstance().encodedDualVideoFrame(sendData, ExternalVideoModuleCallback.VideoFrameType.FRAMETYPE_P, mWidth, mHeight,mIsDualVideo);
                }
            }

            mMediaEncoder.releaseOutputBuffer(outputBufferIndex, false);
            outputBufferIndex = mMediaEncoder.dequeueOutputBuffer(bufferInfo, 0);
        }
    }

    public void closeHardwareEncoder() {
        if (mMediaEncoder != null) {
            mMediaEncoder.stop();
            mMediaEncoder.release();
            mMediaEncoder = null;
        }
    }

    public byte[] changeFormatToTarget(ConfVideoFrame frame) {
        byte[] mFinallyDatas = frame.buf;
        if (frame.format == ConfVideoFrame.FORMAT_NV21) {
            mFinallyDatas = NV21ToNV12(mlencoder, frame.buf, frame.stride, frame.height, true, frame.rotation);
        } else if (frame.format == ConfVideoFrame.FORMAT_RGBA) {
            mFinallyDatas = RGBAToNV12(mlencoder, frame.buf, frame.stride, frame.height, true, frame.rotation);
        } else if (frame.format == ConfVideoFrame.FORMAT_I420) {
            mFinallyDatas = I420ToI420(mlencoder, frame.buf, frame.stride, frame.height, true, frame.rotation);
        }
        return mFinallyDatas;
    }

    private native long Initialize(VideoEncoder encoder);

    private native void Uninitialize(long lencoder);

    private native boolean openSoftEncoder(long lencoder, int nWidth, int nHeight, int nFs, int nBitRate, int nGop, int YuvType);

    private native void encodeYuvFrame(long lencoder, byte[] yuvFrame, int pts);//I420格式

    private native void closeSoftEncoder(long lencoder);

    private native void setBitRate(long lencoder, int bitRate);

    private native void setEncoderResolution(long lencoder, int outWidth, int outHeight);

    private native byte[] I420ToI420(long lencoder, byte[] yuvFrame, int width, int height, boolean flip, int rotate);

    private native byte[] NV12ToNV12(long lencoder, byte[] yuvFrame, int width, int height, boolean flip, int rotate);

    private native byte[] I420ToNV12(long lencoder, byte[] yuvFrame, int width, int height, boolean flip, int rotate);

    private native byte[] NV12ToI420(long lencoder, byte[] yuvFrame, int width, int height, boolean flip, int rotate);

    private native byte[] NV21ToI420(long lencoder, byte[] yuvFrame, int width, int height, boolean flip, int rotate);

    private native byte[] NV21ToNV12(long lencoder, byte[] yuvFrame, int width, int height, boolean flip, int rotate);

    private native byte[] RGBAToI420(long lencoder, byte[] yuvFrame, int width, int height, boolean flip, int rotate);

    private native byte[] RGBAToNV12(long lencoder, byte[] yuvFrame, int width, int height, boolean flip, int rotate);


    public static byte[] CheckFrame(byte[] frame,int offset,int len,byte[] sei) {
        if (offset + len > frame.length) {
            //打印错误
            return null;
        }

//        int naluType = (frame[offset + 4] & 0x1f);
//        boolean iframe = false;
//        if (naluType == 0x07 || naluType == 0x05) {
//            iframe = true;
//            //判断是否为关键帧
//        }

        int max = offset + len - 3;
        int outlen = len - 4;
        Vector<Integer> vIndx = new Vector<>();
        for (int i = offset; i < max; i++) {
            if (frame[i] == 0 && frame[i + 1] == 0 && frame[i + 2] == 1) {
                if (i > 0 && frame[i - 1] == 0) {
                    vIndx.add(i - 1);
                } else {
                    vIndx.add(i);
                    outlen++;
                }
            }
        }
        vIndx.add(offset + len);
        if(sei!=null){
            outlen+=sei.length;
        }
        byte[] outframe = new byte[outlen];
        int index = 0;
        for (int i = 0; i < vIndx.size() - 1; i++) {
            int nL = vIndx.get(i + 1) - vIndx.get(i);
            if (i == 0) {
                System.arraycopy(frame, offset + vIndx.get(i) + 4, outframe, index, nL - 4);
                index += nL - 4;
            } else {
                if(i==2) {
                    if(sei!=null){
                        System.arraycopy(sei, 0, outframe, index, sei.length);
                        index+=sei.length;
                    }
                }
                if (frame[offset + vIndx.get(i) + 2] == 1) {
                    outframe[index++] = 0;//补零
                    System.arraycopy(frame, offset + vIndx.get(i), outframe, index, nL);
                }else{
                    System.arraycopy(frame, offset + vIndx.get(i), outframe, index, nL);
                }

                //Log.i("-------",outframe[index-1]+" "+outframe[index]+" "+outframe[index+1]+" "+outframe[index+2]+" "+outframe[index+3]+" "+outframe[index+4]);
                index+=nL;
            }
        }
        //Log.i("-------","nalu="+naluType+"  rawlen="+len+"  outlen="+outframe.length);
        return outframe;
    }

    //软件编码完成后回调函数
    private void OnYuvFrameEncoded(byte[] encdata, int length, int frameType) {
        PviewLog.wf("OnYuvFrameEncoded encdata : " + length + " | frameType : " + frameType);
        byte nelkey = (byte) (encdata[4] & 0x1f);
        //去掉编码前面00 00 00 01
        byte[] sendData=null;

        synchronized (sei_sync) {
            if (sei_byte != null&&nelkey==7) {
                sendData = CheckFrame(encdata,0,encdata.length,sei_byte);
            } else {
                sendData = CheckFrame(encdata,0,encdata.length,sei_byte);
            }
        }

        if (nelkey == 7) {
            MyVideoApi.getInstance().encodedDualVideoFrame(sendData, ExternalVideoModuleCallback.VideoFrameType.FRAMETYPE_I, mWidth, mHeight , mIsDualVideo);
        } else {
            MyVideoApi.getInstance().encodedDualVideoFrame(sendData, ExternalVideoModuleCallback.VideoFrameType.FRAMETYPE_P, mWidth, mHeight , mIsDualVideo);
        }

    }

    static {
        System.loadLibrary("yuv");
        System.loadLibrary("codec");
    }
}
