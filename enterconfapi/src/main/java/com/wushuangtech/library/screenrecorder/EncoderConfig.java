package com.wushuangtech.library.screenrecorder;

import android.media.MediaFormat;
import android.opengl.EGL14;
import android.opengl.EGLContext;

import java.io.File;

/**
 * Encoder configuration.
 * <p>
 * Object is immutable, which means we can safely pass it between threads without
 * explicit synchronization (and don't need to worry about it getting tweaked out from
 * under us).
 * <p>
 * TODO: make frame rate and iframe interval configurable?  Maybe use builder pattern
 * with reasonable defaults for those and bit rate.
 */
public class EncoderConfig {
    final File mOutputFile;
    final int mWidth;
    final int mHeight;
    final float mTopCropped;
    final float mBottomCropped;
    final int mFrameRate;
    final int mBitRate;
    final int mIFrameInterval;
    final String mMinType;
    final EGLContext mEglContext;

    public EncoderConfig(File outputFile, int width, int height,
                         int bitRate, int frameRate, int iFrameInterval) {
        this(outputFile, width, height, 0f, 0f, bitRate, frameRate, iFrameInterval, MediaFormat.MIMETYPE_VIDEO_AVC, EGL14.eglGetCurrentContext());
    }

    public EncoderConfig(File outputFile, int width, int height,
                         int bitRate, int frameRate, int iFrameInterval,
                         EGLContext sharedEglContext) {
        this(outputFile, width, height, 0f, 0f, bitRate, frameRate, iFrameInterval, MediaFormat.MIMETYPE_VIDEO_AVC, sharedEglContext);
    }

    public EncoderConfig(File outputFile, int width, int height,
                         int bitRate, int frameRate, int iFrameInterval, String minType,
                         EGLContext sharedEglContext) {
        this(outputFile, width, height, 0f, 0f, bitRate, frameRate, iFrameInterval, minType, sharedEglContext);
    }

    public EncoderConfig(File outputFile, int width, int height,
                         float topCropped, float bottomCropped,
                         int bitRate, int frameRate, int iFrameInterval, String minType,
                         EGLContext sharedEglContext) {
        mOutputFile = outputFile;
        mWidth = width;
        mHeight = height;
        mTopCropped = topCropped;
        mBottomCropped = bottomCropped;
        mBitRate = bitRate;
        mFrameRate = frameRate;
        mIFrameInterval = iFrameInterval;
        mMinType = minType;
        mEglContext = sharedEglContext;
    }

    @Override
    public String toString() {
        return "EncoderConfig: " + mWidth + "x" + mHeight
                + ", Crop with: " + mTopCropped + " and " + mBottomCropped
                + "@ mBitRate : " + mBitRate
                + " | mFrameRate : " + mFrameRate
                + " | mIFrameInterval : " + mIFrameInterval
                + " | mMinType : " + mMinType
                + " to '" + mOutputFile.toString() + "' ctxt=" + mEglContext;
    }
}

