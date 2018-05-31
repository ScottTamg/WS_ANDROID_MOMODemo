package com.wushuangtech.videocore;

import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.view.Surface;

import com.wushuangtech.library.GlobalHolder;
import com.wushuangtech.utils.PviewLog;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * VideoDecoder 软件和硬件编码都有，可以通过创建对象的时候指定软硬件编码
 * VideoDecoder硬件编码根据DecoderH264写的，软件编码使用gffmpeg
 */

public class VideoDecoder {

    private boolean mEnableSoftwareDecoder = false;
    private long mpdecoder = 0;
    private Surface mSurface = null;
    private MediaCodec mMediaCodec = null;

    private int mWidth = 360;
    private int mHeight = 640;
    private String mBindDevID;
    private boolean isNotifyed;
    private int mDecoderWidth = -1;
    private int mDecoderHeight = -1;

    void SetDecodeSize(int width, int height) {
        mWidth = width;
        mHeight = height;
    }

    public void setSurfaceTexture(SurfaceTexture mSurfaceTexture) {
        mSurfaceTexture.setDefaultBufferSize(mWidth, mHeight);
        mSurface = new Surface(mSurfaceTexture);
        setSurface(mpdecoder , mSurface);
    }

    public static synchronized VideoDecoder createInstance(boolean enablesoftdec) {
        VideoDecoder decoder;
        synchronized (VideoDecoder.class) {
            decoder = new VideoDecoder();
            if (decoder != null) {
                if (enablesoftdec || Build.VERSION.SDK_INT <= 15) {
                    System.loadLibrary("codec");
                    decoder.mpdecoder = decoder.Initialize(decoder);
                    decoder.mEnableSoftwareDecoder = enablesoftdec;
                }
            }
        }
        return decoder;
    }

    private VideoDecoder() {
        mpdecoder = 0;
    }

    public boolean start() {
        useDecodedData(mpdecoder, true);
        isNotifyed = false;
        mDecoderWidth = -1;
        mDecoderHeight = -1;
        if (mEnableSoftwareDecoder || Build.VERSION.SDK_INT <= 15) {
            PviewLog.d("VideoDecoder", "the device id : " + mBindDevID + " , use soft decoder. hardware decoder size : " +
                    GlobalHolder.getInstance().getHardwareDecoderSize());
            //软件编码需要设置buffer的宽和高
            return openSoftDecoder(mpdecoder, mWidth, mHeight);
        } else {
            PviewLog.d("VideoDecoder", "the device id : " + mBindDevID + " , use hardware decoder. hardware decoder size : " +
                    GlobalHolder.getInstance().getHardwareDecoderSize());
            return openHardwareDecoder(mWidth, mHeight);
        }
    }

    public void onGetH264Frame(RemoteSurfaceView.VideoFrame frame) {
        if (mDecoderWidth == -1 || mDecoderHeight == -1 ||
                frame.width != mDecoderWidth || frame.height != mDecoderHeight) {
            mDecoderWidth = frame.width;
            mDecoderHeight = frame.height;
        }
        PviewLog.wf("onGetH264Frame mEnableSoftwareDecoder : " + mEnableSoftwareDecoder
            + " | mDecoderWidth : " + mDecoderWidth + " | mDecoderHeight : " + mDecoderHeight);
        if (mEnableSoftwareDecoder || Build.VERSION.SDK_INT <= 15) {
            decodeYuvFrame(mpdecoder, frame.data, (int) frame.timeStamp);
        } else {
            hardwareDecodeFrame(frame.data, frame.timeStamp);
        }
    }

    public void stop() {
        if (mEnableSoftwareDecoder || Build.VERSION.SDK_INT <= 15) {
            closeSoftDecoder(mpdecoder);
        } else {
            closeHardwareDecoder();
        }
    }

    public void setBindDevID(String mBindDevID) {
        this.mBindDevID = mBindDevID;
    }

    private boolean openHardwareDecoder(int nwidth, int nheigth) {
        try {
            mMediaCodec = MediaCodec.createDecoderByType("video/avc");
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        MediaFormat mMediaFormat = MediaFormat.createVideoFormat("video/avc", nwidth, nheigth);
        mMediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 65536);
        mMediaCodec.configure(mMediaFormat, mSurface, null, 0);
        if (mMediaCodec == null) {
            return false;
        }
        mMediaCodec.setVideoScalingMode(MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
        mMediaCodec.start();
        return true;
    }

    private boolean hardwareDecodeFrame(byte[] buf, long pts) {
        if (mMediaCodec == null) {
            return false;
        }

        try {
            ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
            //ByteBuffer[] outBuffers = mMediaCodec.getOutputBuffers();

            int inputBufferIndex = mMediaCodec.dequeueInputBuffer(-1);
            if (inputBufferIndex >= 0) {
                ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                inputBuffer.clear();
                inputBuffer.put(buf, 0, buf.length);
                mMediaCodec.queueInputBuffer(inputBufferIndex, 0, buf.length, pts, 0);
            } else {
                return false;
            }

            // Get output buffer index
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, 0);
            while (outputBufferIndex >= 0) {

                mMediaCodec.releaseOutputBuffer(outputBufferIndex, true);

                outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, 0);
                if (!isNotifyed) {
                    isNotifyed = true;
                    GlobalHolder.getInstance().notifyCHFirstRemoteVideoDraw(mBindDevID, mDecoderWidth, mDecoderHeight);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    public void closeHardwareDecoder() {
        if (mMediaCodec != null) {
            PviewLog.d("VideoDecoder", "closeHardwareDecoder , the device id : " + mBindDevID);
            mMediaCodec.stop();
            mMediaCodec.release();
            mMediaCodec = null;
        }
    }

    private native long Initialize(VideoDecoder decoder);

    private native void Uninitialize(long pdecoder);

    private native boolean openSoftDecoder(long ldecoder, int nWidth, int nHeight);

    private native void decodeYuvFrame(long ldecoder, byte[] yuvFrame, int pts);

    private native void closeSoftDecoder(long ldecoder);

    private native boolean setSurface(long ldecoder, Surface surface);

    private native boolean useDecodedData(long ldecoder, boolean use);

    //软件解码完成为ARGB
    private void OnFrameDecoded(byte[] decdata, int width, int height) {
        PviewLog.wf("OnYuvFrameDecoded width : " + width + " | height : " + height + " | mBindDevID : " + mBindDevID);
//        if (mIsStop) {
//            return ;
//        }
//
//        // 如果Surface不为null,就绘制图形
//        if (mSurface == null && mSurfaceTexture != null) {
//            PviewLog.w("mSurface is null , create new decoder : " + mBindDevID);
//            VideoDecoderManager.getInstance().userExitRoom(mBindDevID);
//            VideoDecoderManager.getInstance().createNewVideoDecoder(mBindDevID);
//            mIsStop = true;
//        }
//
//        if (mSurface != null) {
//            //数据源是byte[]
//            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
//            ByteBuffer mSoftDecodeBuffer = ByteBuffer.wrap(decdata);
//            bitmap.copyPixelsFromBuffer(mSoftDecodeBuffer);
//            //数据源是int[]
//            Rect rect = new Rect(0, 0, 0x3FFF, 0x3FFF);
//            Canvas canvas = mSurface.lockCanvas(rect);
//            PviewLog.wf("OnYuvFrameDecoded canvas : " + canvas
//                    + " | mSurfaceTexture : " + mSurfaceTexture);
//            if (canvas != null) {
//                Matrix matrix = new Matrix();
//                float f1 = 1;
//                matrix.postScale(canvas.getWidth() * f1 / width, canvas.getHeight() * f1 / height);
//                canvas.drawBitmap(bitmap, matrix, null);
//                mSurface.unlockCanvasAndPost(canvas);
//            }
//        }
        //将数据解码后通知给第三方
        GlobalHolder.getInstance().notifyCHRemoteVideoDecoder(decdata, mBindDevID, mDecoderWidth, mDecoderHeight);
    }

    private void OnFirstFrameDecoded(int width, int height) {
        PviewLog.d("OnFirstFrameDecoded width : " + width + " | height : " + height + " | mBindDevID : " + mBindDevID);
        if (!isNotifyed) {
            isNotifyed = true;
            GlobalHolder.getInstance().notifyCHFirstRemoteVideoDraw(mBindDevID, mDecoderWidth, mDecoderHeight);
        }
    }
}
