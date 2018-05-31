package com.wushuangtech.videocore;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import com.wushuangtech.api.EnterConfApi;
import com.wushuangtech.api.ExternalVideoModuleCallback;
import com.wushuangtech.library.Constants;
import com.wushuangtech.utils.PviewLog;
import com.wushuangtech.videocore.com.wushuangtech.fbo.FBOTextureBinder;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.opengles.GL10;

import project.android.imageprocessing.FastImageProcessingPipeline;
import project.android.imageprocessing.output.ScreenEndpoint;

import static android.opengl.EGL14.EGL_CONTEXT_CLIENT_VERSION;

public class RemoteSurfaceView extends GLSurfaceView {

    private Context mContext;
    private FastImageProcessingPipeline mPipeline = null;
    private ScreenEndpoint mScreen = null;
    private VideoDecodeInput mDecodeInput = null;
    private boolean bcreate = false;
    private int scale_mode = Constants.RENDER_MODE_HIDDEN;
    private int decFrames = 0;
    private int mDecDatas;
    private int mDecDatasKbps;

    private boolean mIsLocalCameraView;
    private String mDeviceID;
    private long mBindUserID;
    private int mLastDecFrameCount;
    private int mLastDecDataCount;

    private boolean mRealInitRemote;
    private boolean mPipleCreated;

    static class VideoFrame {
        public byte[] data;
        public ExternalVideoModuleCallback.VideoFrameType frameType;
        public long timeStamp;
        public int width;
        public int height;
    }

    public RemoteSurfaceView(Context context) {
        this(context, null, Constants.RENDER_MODE_HIDDEN);
    }

    /**
     * Creates a new view which can be used for fast image processing.
     *
     * @param context The activity context that this view belongs to.
     */
    public RemoteSurfaceView(Context context, int mode) {
        this(context, null, mode);
    }

    /**
     * Creates a new view which can be used for fast image processing.
     *
     * @param context The activity context that this view belongs to.
     * @param attr    The activity attribute set.
     */
    public RemoteSurfaceView(Context context, AttributeSet attr, int mode) {
        super(context, attr);
        mContext = context;
        LocaSurfaceView.getInstance().mContext = context;
        setDebugFlags(GLSurfaceView.DEBUG_CHECK_GL_ERROR | GLSurfaceView.DEBUG_LOG_GL_CALLS);
        MyEGLContextFactory mMyEGLContextFactory = new MyEGLContextFactory();
        setEGLContextFactory(mMyEGLContextFactory);
        setEGLContextClientVersion(2);
        mPipeline = new FastImageProcessingPipeline();
        this.setPipeline(mPipeline);
        if (mIsLocalCameraView) {
            PviewLog.d("LocalCamera LocaSurfaceView start create ....");
            LocaSurfaceView.getInstance().mPipeline = mPipeline;
        }
        mPipeline.setListen(new FastImageProcessingPipeline.SurfaceListen() {

            @Override
            public void onSurfaceCreated(GL10 unused, EGLConfig config) {
                mPipleCreated = true;
                if (mIsLocalCameraView && LocaSurfaceView.getInstance().mPipeline != null) {
                    PviewLog.d("LocalCamera LocaSurfaceView onSurfaceCreated ....");
                    LocaSurfaceView.getInstance().CreateLocalSurfaceView(LocaSurfaceView.getInstance().mWaterMarkPos);
                } else {
                    if (mRealInitRemote && !bcreate) {
                        PviewLog.d("FastImageProcessingPipeline onSurfaceCreated.. mBindUserID : " + mBindUserID);
                        unused.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
                        unused.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
                        FreeAll();
                        CreateRemoteSurfaceView();
                    }
                }
            }


            @Override
            public void onSurfaceChanged(GL10 unused, int width, int height) {
                if (mIsLocalCameraView) {
                    PviewLog.d("LocalCamera LocaSurfaceView onSurfaceChanged ....");
                    if (LocaSurfaceView.getInstance().mPipeline != null) {
                        if (!LocaSurfaceView.getInstance().bcreate) {
                            LocaSurfaceView.getInstance().CreateLocalSurfaceView(LocaSurfaceView.getInstance().mWaterMarkPos);
                        }
                        LocaSurfaceView.getInstance().mScreen.reInitialize();
                    }
                } else {
                    if (mRealInitRemote) {
                        PviewLog.d("FastImageProcessingPipeline onSurfaceChanged.. mBindUserID : " + mBindUserID);
                        //CreateRemoteSurfaceView();
                        mScreen.reInitialize();
                    }
                }
            }
        });
    }

    public void initRemote(int mode) {
        PviewLog.d("LocalCamera LocaSurfaceView initRemote ....");
        scale_mode = mode;
        if (mPipleCreated && !bcreate) {
            PviewLog.d("initRemote onSurfaceCreated.. mBindUserID : " + mBindUserID);
            FreeAll();
            CreateRemoteSurfaceView();
            requestRender();
        }
        mRealInitRemote = true;

        if (Constants.IS_UNITY) {
            CreateRemoteSurfaceView();
            createFBOTextureBinder();
        }
    }

    public FastImageProcessingPipeline getPipeline() {
        return mPipeline;
    }

    public VideoDecodeInput getDecodeInput() {
        return mDecodeInput;
    }

    public byte[] getRemoteBuffer() {
        if (mDecodeInput != null) {
            return mDecodeInput.getFBOBuffer();
        }
        return null;
    }

    public int getRemoteWidth() {
        if (mDecodeInput != null) {
            return mDecodeInput.getWidth();
        }
        return 0;
    }

    public int getRemoteHeight() {
        if (mDecodeInput != null) {
            return mDecodeInput.getHeight();
        }
        return 0;
    }

    private synchronized void createFBOTextureBinder() {
        FBOTextureBinder mFBOTextureBinder = new FBOTextureBinder(mContext);
        mDecodeInput.setFBOTextureBinder(mFBOTextureBinder);
        mFBOTextureBinder.initEGLHelper();
        mFBOTextureBinder.initLocalThread();
        mFBOTextureBinder.initGameRenderGLES(mPipeline, EGL10.EGL_NO_CONTEXT);
        mFBOTextureBinder.startGameRender();
    }

    public void CreateRemoteSurfaceView() {
        if (bcreate) {
            return;
        }
        bcreate = true;
        synchronized (this) {
            mDecodeInput = new VideoDecodeInput(this);
            mScreen = new ScreenEndpoint(mPipeline);
            if (!mIsLocalCameraView) {
                mScreen.setPreView(true);
            }
            mScreen.setScaleMode(scale_mode);
            mDecodeInput.addTarget(mScreen);

            mPipeline.addRootRenderer(mDecodeInput);
        }
        startRendering();
    }

    private void startRendering() {
        if (mPipeline == null) {
            return;
        }
        mPipeline.startRendering();
    }

    public void setIsLocalCameraView(boolean mIsLocalCameraView) {
        this.mIsLocalCameraView = mIsLocalCameraView;
    }

    /**
     * Sets the FastImageProcessingPipeline that will do the rendering for this view.
     *
     * @param pipeline The FastImageProcessingPipeline that will do the rendering for this view.
     */
    public void setPipeline(FastImageProcessingPipeline pipeline) {
        setRenderer(pipeline);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
    }

    public void AddVideoData(VideoFrame frame) {
        synchronized (this) {
            if (mDecodeInput != null) {
                if (mDecodeInput.getWidth() != frame.width || mDecodeInput.getHeight() != frame.height) {
                    MyVideoApi.getInstance().updateDecodeSize(frame.width, frame.height, this);
                    mDecodeInput.setRenderSize(frame.width, frame.height);
                    mScreen.SetRawSize(frame.width, frame.height);
                }
                decFrames++;
                mDecDatas += frame.data.length;
                mDecDatasKbps = frame.data.length;
                PviewLog.wf("AddVideoData mDecodeInput : " + mDecodeInput);
            }
        }
    }

    private void FreeAll() {
        bcreate = false;
        mPipeline.pauseRendering();
        synchronized (this) {
            if (mDecodeInput != null) {
                mLastDecFrameCount = 0;
                mLastDecDataCount = 0;
                mDecDatas = 0;
                mDecDatasKbps = 0;
                mPipeline.removeRootRenderer(mDecodeInput);
                mDecodeInput.removeTarget(mScreen);
                mDecodeInput.destroy();
                mScreen.destroy();
                mDecodeInput = null;
                mScreen = null;
            }
        }
    }

    public int getDecFrames() {
        return decFrames;
    }

    public int getRenFrames() {
        return decFrames;
    }

    public int getDecDatas() {
        return mDecDatas;
    }

    public int getDecDatasKbps() {
        return mDecDatasKbps;
    }

    public int getLastDecFrameCount() {
        return mLastDecFrameCount;
    }

    public int getLastDecDataCount() {
        return mLastDecDataCount;
    }

    public long getBindUserID() {
        return mBindUserID;
    }

    public int getDisplayMode() {
        if (mScreen != null) {
            return mScreen.getScaleMode();
        }
        return -1;
    }

    public void setDisplayMode(int mode) {
        if (mScreen != null) {
            mScreen.setScaleMode(mode);
            mScreen.reInitialize();
        }
    }

    public void setDeviceID(String mDeviceID) {
        this.mDeviceID = mDeviceID;
    }

    public void setBindUserID(long mBindUserID) {
        this.mBindUserID = mBindUserID;
    }


    public void setLastDecFrameCount(int mLastDecFrameCount) {
        this.mLastDecFrameCount = mLastDecFrameCount;
    }

    public void setLastDecDataCount(int mLastDecDataCount) {
        this.mLastDecDataCount = mLastDecDataCount;
    }

    public void openOrCloseVideoDevice(boolean mIsOpen) {
        if (mIsOpen) {
            EnterConfApi.getInstance().openDeviceVideo(mBindUserID, mDeviceID);
        } else {
            EnterConfApi.getInstance().closeDeviceVideo(mBindUserID, mDeviceID);
        }
    }

    private class MyEGLContextFactory implements GLSurfaceView.EGLContextFactory {

        @Override
        public EGLContext createContext(EGL10 egl10, EGLDisplay eglDisplay, EGLConfig eglConfig) {
            int[] contextAttr = new int[]{
                    EGL_CONTEXT_CLIENT_VERSION, 2,
                    EGL10.EGL_NONE
            };

            return egl10.eglCreateContext(eglDisplay, eglConfig, EGL10.EGL_NO_CONTEXT, contextAttr);
        }

        @Override
        public void destroyContext(EGL10 egl10, EGLDisplay eglDisplay, EGLContext eglContext) {

        }
    }
}