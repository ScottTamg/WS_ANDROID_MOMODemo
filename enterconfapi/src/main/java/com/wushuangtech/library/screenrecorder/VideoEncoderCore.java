/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wushuangtech.library.screenrecorder;

import android.annotation.TargetApi;
import android.graphics.PixelFormat;
import android.media.ImageReader;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;

import com.wushuangtech.api.AVRecorderModule;
import com.wushuangtech.api.ExternalVideoModule;
import com.wushuangtech.api.ExternalVideoModuleCallback;
import com.wushuangtech.library.GlobalConfig;
import com.wushuangtech.library.GlobalHolder;
import com.wushuangtech.utils.PviewLog;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class wraps up the core components used for surface-input video encoding.
 * <p>
 * Once created, frames are fed to the input surface.  Remember to provide the presentation
 * time stamp, and always call drainEncoder() before swapBuffers() to ensure that the
 * producer side doesn't get backed up.
 * <p>
 * This class is not thread-safe, with one exception: it is valid to use the input surface
 * on one thread, and drain the output on a different thread.
 */
public class VideoEncoderCore {
    private static final String TAG = "VideoEncoderCore";
    private static final boolean VERBOSE = true;
    private static final int TIMEOUT_USEC = 10000; //def 10000

    private MediaCodec mVideoEncoder;
    private MediaCodec.BufferInfo mVBufferInfo;
    private int sps_pps_len;
    private byte[] sps_pps_byte;
    private boolean mStreamEnded;

    private Surface mInputSurface;
    private long mRecordStartedAt = 0;
    private boolean mMuxerStarted;
    private String mPath;
    private RecordCallback mCallback;
    private Handler mMainHandler;
    private AtomicBoolean mIsWriteAudio = new AtomicBoolean();
    private EncoderConfig mEncoderConfig;
    private int mLastSendTime = -1;
    private boolean mIsSoftEncode;

    private Runnable mRecordProgressChangeRunnable = new Runnable() {

        @Override
        public void run() {
            if (mCallback != null) {
                long speedTime = System.currentTimeMillis() - mRecordStartedAt;
                int times = (int) (speedTime / 1000);
                if (mLastSendTime != times) {
                    mCallback.onRecordedDurationChanged(times);
                    mLastSendTime = times;
                }
            }
        }
    };

    private Timer mProgressTimer;
    private TimerTask mProgressTask = new TimerTask() {
        @Override
        public void run() {
            mMainHandler.post(mRecordProgressChangeRunnable);
        }
    };

    /**
     * Configures encoder and muxer state, and prepares the input Surface.
     */
    VideoEncoderCore(EncoderConfig mEncoderConfig)
            throws IOException {
        this.mEncoderConfig = mEncoderConfig;
        mMainHandler = new Handler(Looper.getMainLooper());
        if (mIsSoftEncode) {
            initSoftEncoder();
        } else {
            initHardwareEncoder();
        }
        /* Save path */
        mPath = mEncoderConfig.mOutputFile.toString();
        Log.d(TAG, "save file path : " + mPath);
        mLastSendTime = -1;
        mMuxerStarted = false;
        GlobalHolder.getInstance().setAudioDataCallBack(new AudioDataCallBack() {
            @Override
            public void pushEncodedAudioData(byte[] data) {
                if (mIsWriteAudio.get()) {
                    PviewLog.wf("pushEncodedAudioData Get Audio Datas : " + data[1]);
                    AVRecorderModule.getInstance().pushEncodedAudioData(data);
                }
            }
        });
        AVRecorderModule.getInstance().startRecorde(mPath);
    }

    private void initHardwareEncoder() {
        try {
            mVBufferInfo = new MediaCodec.BufferInfo();
            sps_pps_len = 0;
            MediaFormat videoFormat = MediaFormat.createVideoFormat(mEncoderConfig.mMinType, mEncoderConfig.mWidth, mEncoderConfig.mHeight);

            // Set some properties.  Failing to specify some of these can cause the MediaCodec
            // configure() call to throw an unhelpful exception.
            videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, mEncoderConfig.mBitRate);
            videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, mEncoderConfig.mFrameRate);
            videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, mEncoderConfig.mIFrameInterval);
            if (VERBOSE) {
                Log.d(TAG, "videoFormat: " + videoFormat);
            }

            // Create a MediaCodec encoder, and configure it with our videoFormat.  Get a Surface
            // we can use for input and wrap it with a class that handles the EGL work.
            mVideoEncoder = MediaCodec.createEncoderByType(mEncoderConfig.mMinType);
            mVideoEncoder.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mInputSurface = mVideoEncoder.createInputSurface();
            mVideoEncoder.start();
            mStreamEnded = false;
        } catch (Exception e) {
            initSoftEncoder();
        }
    }

    private void initSoftEncoder(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            ImageReader imageReader = ImageReader.newInstance(mEncoderConfig.mWidth, mEncoderConfig.mHeight, PixelFormat.RGBA_8888, 1);
            mInputSurface = imageReader.getSurface();
            imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader imageReader) {

                }
            } , null);
        }
    }

    /**
     * Returns the encoder's input surface.
     */
    Surface getInputSurface() {
        return mInputSurface;
    }

    /**
     * Releases encoder resources.
     */
    public void release() {
        if (VERBOSE) {
            Log.d(TAG, "releasing encoder objects");
        }

        try{
            mMuxerStarted = false;
            if (mVideoEncoder != null) {
                mVideoEncoder.stop();
                mVideoEncoder.release();
                mVideoEncoder = null;
            }

            if (mProgressTimer != null) {
                mProgressTimer.cancel();
                mProgressTimer = null;
            }

            if (mCallback != null) {
                mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        AVRecorderModule.getInstance().stopRecorde();
                        File mResultCheck = new File(mPath);
                        if (mResultCheck.exists()) {
                            String name = mResultCheck.getName();
                            PviewLog.i("Record File mResultCheck: " + name);
                            String substring = name.substring(0, name.indexOf("."));
                            PviewLog.i("Record File substring: " + substring);
                            String targetName = mResultCheck.getParent() + File.separator + substring + ".mp4";
                            boolean resultName = mResultCheck.renameTo(new File(targetName));
                            if (resultName) {
                                PviewLog.i("Record File rename success : " + targetName);
                            } else {
                                PviewLog.i("Record File rename failed : " + targetName);
                            }
                        }
                        mCallback.onRecordSuccess(mPath, System.currentTimeMillis() - mRecordStartedAt);
                    }
                });
            }
        } catch (final Exception e){
            if (mCallback != null) {
                mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mCallback.onRecordFailed(e , System.currentTimeMillis() - mRecordStartedAt);
                    }
                });
            }
        }
    }

    public void setRecordCallback(RecordCallback callback) {
        mCallback = callback;
    }

    /**
     * Extracts all pending data from the encoder and forwards it to the muxer.
     * <p>
     * If endOfStream is not set, this returns when there is no more data to drain.  If it
     * is set, we send EOS to the encoder, and then iterate until we see EOS on the output.
     * Calling this with endOfStream set should be done once, right before stopping the muxer.
     * <p>
     * We're just using the muxer to get a .mp4 file (instead of a raw H.264 stream).  We're
     * not recording audio.
     */
    public void drainEncoder(boolean endOfStream) {
        if (VERBOSE) {
            Log.d(TAG, "drainEncoder(" + endOfStream + ")");
        }

        if (endOfStream) {
            if (VERBOSE) {
                Log.d(TAG, "sending EOS to encoder");
            }
            mVideoEncoder.signalEndOfInputStream();
            mStreamEnded = true;
        }
        drainAudio(endOfStream);
        drainVideo(endOfStream);

        if (endOfStream && mMuxerStarted && mCallback != null) {
            mMainHandler.post(mRecordProgressChangeRunnable);
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void drainVideo(boolean endOfStream) {
        while (true) {
            int encoderStatus = mVideoEncoder.dequeueOutputBuffer(mVBufferInfo, TIMEOUT_USEC);
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // no output available yet
                if (!endOfStream) {
                    break;      // out of while
                } else {
                    if (mStreamEnded) {
                        break;
                    }
                    if (VERBOSE) {
                        Log.d(TAG, "no video output available, spinning to await EOS");
                    }
                }
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // should happen before receiving buffers, and should only happen once
                if (mMuxerStarted) {
                    throw new RuntimeException("format changed twice");
                }
                MediaFormat newFormat = mVideoEncoder.getOutputFormat();
                Log.d(TAG, "video encoder output format changed: " + newFormat);

                tryStartMuxer();
            } else if (encoderStatus < 0) {
                Log.w(TAG, "unexpected result from encoder.dequeueOutputBuffer: " +
                        encoderStatus);
                // let's ignore it
            } else {
                if (mMuxerStarted) {
                    // same as mVideoEncoder.getOutputBuffer(encoderStatus)
                    ByteBuffer encodedData = mVideoEncoder.getOutputBuffer(encoderStatus);

                    if (encodedData == null) {
                        throw new RuntimeException("encoderOutputBuffer " + encoderStatus +
                                " was null");
                    }

                    // 这段代码会导致不出sps帧，原Demo有用
//                    if ((mVBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
//                        // The codec config data was pulled out and fed to the muxer when we got
//                        // the INFO_OUTPUT_FORMAT_CHANGED status.  Ignore it.
//                        if (VERBOSE) {
//                            Log.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG");
//                        }
//                        mVBufferInfo.size = 0;
//                    }

                    if (mVBufferInfo.size != 0) {
                        if (!mMuxerStarted) {
                            throw new RuntimeException("muxer hasn't started");
                        }

                        // adjust the ByteBuffer values to match BufferInfo (not needed?)
                        encodedData.position(mVBufferInfo.offset);
                        encodedData.limit(mVBufferInfo.offset + mVBufferInfo.size);

                        byte nelkey = (byte) (encodedData.get(4) & 0x1f);
                        byte[] outData = new byte[mVBufferInfo.size];
                        encodedData.get(outData);
                        byte[] sendData = null;
                        if (nelkey == 5) {
                            sendData = new byte[mVBufferInfo.size + sps_pps_len];
                            System.arraycopy(sps_pps_byte, 0, sendData, 0, sps_pps_len);
                            System.arraycopy(outData, 0, sendData, sps_pps_len, mVBufferInfo.size);
                        } else if (nelkey == 7) {
                            sps_pps_len = mVBufferInfo.size - 4;
                            sps_pps_byte = new byte[sps_pps_len];
                            System.arraycopy(outData, 4, sps_pps_byte, 0, sps_pps_len);
                        } else {
                            sendData = new byte[mVBufferInfo.size - 4];
                            System.arraycopy(outData, 4, sendData, 0, mVBufferInfo.size - 4);
                        }
                        PviewLog.wf("pushEncodedVideoData Get Video Datas : " + sendData + " | nelkey : " + nelkey
                         + " | mIsScreenRecordShare : " + GlobalConfig.mIsScreenRecordShare.get());
                        if (sendData != null) {
                            if (nelkey == 5) {
                                if (GlobalConfig.mIsScreenRecordShare.get()) {
                                    ArrayList<byte[]> list = new ArrayList<>();
                                    list.add(sendData);
                                    ExternalVideoModule.getInstance().pushEncodedVideoData(list,
                                            ExternalVideoModuleCallback.VideoFrameType.FRAMETYPE_I, mEncoderConfig.mWidth, mEncoderConfig.mHeight);
                                } else {
                                    AVRecorderModule.getInstance().pushEncodedVideoData(sendData,
                                            ExternalVideoModuleCallback.VideoFrameType.FRAMETYPE_I, mEncoderConfig.mWidth, mEncoderConfig.mHeight);
                                }
                            } else {
                                if (GlobalConfig.mIsScreenRecordShare.get()) {
                                    ArrayList<byte[]> list = new ArrayList<>();
                                    list.add(sendData);
                                    ExternalVideoModule.getInstance().pushEncodedVideoData(list,
                                            ExternalVideoModuleCallback.VideoFrameType.FRAMETYPE_P, mEncoderConfig.mWidth, mEncoderConfig.mHeight);
                                } else {
                                    AVRecorderModule.getInstance().pushEncodedVideoData(sendData,
                                            ExternalVideoModuleCallback.VideoFrameType.FRAMETYPE_P, mEncoderConfig.mWidth, mEncoderConfig.mHeight);
                                }
                            }
                        } else {
                            Log.e(TAG, "sendData is null!!!");
                        }
                        if (VERBOSE) {
                            Log.d(TAG, "sent " + mVBufferInfo.size + " video bytes to muxer, ts=" +
                                    mVBufferInfo.presentationTimeUs);
                        }
                    }

                    mVideoEncoder.releaseOutputBuffer(encoderStatus, false);

                    if ((mVBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        if (!endOfStream) {
                            Log.w(TAG, "reached end of stream unexpectedly");
                        } else {
                            if (VERBOSE) {
                                Log.d(TAG, "end of video stream reached");
                            }
                        }
                        break;      // out of while
                    }
                } else {
                    Log.w(TAG, "Muxer is not started, just return");
                    // let's ignore it
                    mVideoEncoder.releaseOutputBuffer(encoderStatus, false);
                }
            }
        }
    }

    private void drainAudio(boolean endOfStream) {
        if (endOfStream) {
            mIsWriteAudio.set(false);
        } else {
            mIsWriteAudio.set(true);
        }
    }

    private void tryStartMuxer() {
        if (!mMuxerStarted) { // and muxer not started
            // then start the muxer
            mMuxerStarted = true;
            mRecordStartedAt = System.currentTimeMillis();
            mProgressTimer = new Timer();
            mProgressTimer.schedule(mProgressTask, 0, 16);
        }
    }

    public interface AudioDataCallBack {

        void pushEncodedAudioData(byte[] data);
    }
}
