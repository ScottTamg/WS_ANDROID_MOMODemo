/*
 *
 * FBORender.java
 *
 * Created by Wuwang on 2016/12/24
 * Copyright © 2016年 深圳哎吖科技. All rights reserved.
 */
package com.wushuangtech.videocore.com.wushuangtech.fbo;

import android.content.Context;
import android.content.res.Resources;
import android.opengl.EGLSurface;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.wushuangtech.bean.ConfVideoFrame;
import com.wushuangtech.library.GlobalConfig;
import com.wushuangtech.videocore.EncoderEngine;

import project.android.imageprocessing.FastImageProcessingPipeline;

import static android.content.ContentValues.TAG;

/**
 * Description:
 */
public class FBOTextureBinder {

    private static final int START_ENCODE = 2;
    private static final int INIT_ENCODE_11 = 3;
    private static final int INIT_ENCODE_14 = 4;
    private static final int START_DRAW_FRAME = 5;

    private HandlerThread mLocalHandlerThread;
    private LocalHandlerThreadHandler mLocalHandlerThreadHandler;

    private int mWidth = 240;
    private int mHeight = 320;
    private int mTextureID;
    private javax.microedition.khronos.egl.EGLContext eglContext11;
    private android.opengl.EGLContext eglContext14;

    private String mThreadOwner;
    private EGLHelper mEGLHelper;
    private EglCore mEglCore;
    private AFilter mFilter;
    private Resources mRes;
    private float[] mTransform;
    private int mTextureType;
    private FastImageProcessingPipeline mPipeline;

    public FBOTextureBinder(Context mContext) {
        mRes = mContext.getResources();
    }

    public boolean initEGLContext11(javax.microedition.khronos.egl.EGLContext eglContext11
            , int mWidth, int mHeight, int mTextureID, float[] mTransform, int mTextureType) {
        if (mEGLHelper == null) {
            mEGLHelper = new EGLHelper();
        }
        this.mWidth = mWidth;
        this.mHeight = mHeight;
        this.mTextureID = mTextureID;
        this.eglContext11 = eglContext11;
        this.mTransform = mTransform;
        this.mTextureType = mTextureType;
        mLocalHandlerThreadHandler.sendEmptyMessage(INIT_ENCODE_11);
        return true;
    }

    public boolean initEGLContext14(android.opengl.EGLContext eglContext14
            , int mWidth, int mHeight, int mTextureID, float[] mTransform, int mTextureType) {
        if (mEglCore == null) {
            mEglCore = new EglCore();
        }
        this.mWidth = mWidth;
        this.mHeight = mHeight;
        this.mTextureID = mTextureID;
        this.eglContext14 = eglContext14;
        this.mTransform = mTransform;
        this.mTextureType = mTextureType;
        mLocalHandlerThreadHandler.sendEmptyMessage(INIT_ENCODE_14);
        return true;
    }

    public boolean initLocalThread() {
        if (mLocalHandlerThread == null) {
            mLocalHandlerThread = new HandlerThread("loopers");
            mLocalHandlerThread.start();
            mLocalHandlerThreadHandler = new LocalHandlerThreadHandler(
                    mLocalHandlerThread.getLooper());
            this.mThreadOwner = mLocalHandlerThread.getName();
            return true;
        }
        return false;
    }

    public void initGameRenderGLES(FastImageProcessingPipeline mPipeline,
                                   javax.microedition.khronos.egl.EGLContext eglContext11) {
        this.eglContext11 = eglContext11;
        this.mPipeline = mPipeline;
        mLocalHandlerThreadHandler.sendEmptyMessage(INIT_ENCODE_11);
    }

    public void initEGLHelper() {
        mEGLHelper = new EGLHelper();
    }

    public void startGameRender() {
        mLocalHandlerThreadHandler.sendEmptyMessage(START_DRAW_FRAME);
    }

    public void startEGLTextureEncode() {
        mLocalHandlerThreadHandler.sendEmptyMessage(START_ENCODE);
    }

    public void drawFrame(int textureType, int mTextureID,
                          float[] transform, int mWidth, int mHeight) {
        // 如果传入的texture类型与当前的类型不一样，需要重新构建
        if (mFilter != null && mFilter.getFormatType() != textureType) {
            mFilter = null;
        }

        if (mFilter == null) {
            if (textureType == ConfVideoFrame.FORMAT_TEXTURE_2D) {
                mFilter = new NoFilter(mRes);
            } else if (textureType == ConfVideoFrame.FORMAT_TEXTURE_OES) {
                mFilter = new OesFilter(mRes);
            }

            if (mFilter == null) {
                Log.e(TAG, "getBitmap: Renderer was not set.");
                return;
            }
            mFilter.create();
        }

        mFilter.setSize(mWidth, mHeight);
        if (transform != null) {
            mFilter.setMatrix(transform);
        }
        mFilter.setTextureId(mTextureID);
        mFilter.draw();
    }


    public void destoryFBO() {
        mFilter = null;
        mEGLHelper.destroy();
        if (mEglCore != null) {
            mEglCore.release();
        }
        if (mLocalHandlerThread != null) {
            mLocalHandlerThread.quit();
        }
        mLocalHandlerThreadHandler = null;
    }

    private class LocalHandlerThreadHandler extends Handler {

        LocalHandlerThreadHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case INIT_ENCODE_11:
                    mEGLHelper.destroy();
                    mEGLHelper.shareContext = eglContext11;
                    mEGLHelper.eglInit(mWidth, mHeight);
                    mEGLHelper.makeCurrent();
                    break;
                case INIT_ENCODE_14:
                    mEglCore.release();
                    mEglCore.mShareEGLContext = eglContext14;
                    mEglCore.eglInit();
                    EGLSurface offscreenSurface = mEglCore.createOffscreenSurface(mWidth, mHeight);
                    mEglCore.makeCurrent(offscreenSurface);
                    break;
                case START_ENCODE:
                    if (!Thread.currentThread().getName().equals(mThreadOwner)) {
                        Log.e(TAG, "getBitmap: This thread does not own the OpenGL context.");
                        return;
                    }

                    if (GlobalConfig.mIsCapturing.get()) {
                        drawFrame(mTextureType
                                , mTextureID, mTransform, mWidth
                                , mHeight);
                        EncoderEngine.getInstance().putIntBuffer();
                    }
                    break;
                case START_DRAW_FRAME:
                    mPipeline.onDrawFrame(null);
                    break;
            }
        }
    }
}