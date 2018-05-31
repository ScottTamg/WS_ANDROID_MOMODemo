package com.wushuangtech.videocore;

import android.content.Context;
import android.hardware.Camera;
import android.opengl.GLES20;

import com.faceunity.wrapper.faceunity;
import com.wushuangtech.library.Constants;
import com.wushuangtech.library.GlobalConfig;
import com.wushuangtech.utils.PviewLog;
import com.wushuangtech.videocore.com.wushuangtech.fbo.FBOTextureBinder;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.microedition.khronos.egl.EGL10;

import project.android.imageprocessing.FastImageProcessingPipeline;
import project.android.imageprocessing.beauty.BeautifyFilter;
import project.android.imageprocessing.beauty.FUBeautifyFilter;
import project.android.imageprocessing.filter.MultiInputFilter;
import project.android.imageprocessing.filter.blend.WaterMarklBlendFilter;
import project.android.imageprocessing.input.CameraPreviewInput;
import project.android.imageprocessing.input.GLTextureOutputRenderer;
import project.android.imageprocessing.input.ImageResourceInput;
import project.android.imageprocessing.output.ScreenEndpoint;


class LocaSurfaceView implements GLTextureOutputRenderer.FrameAvaliableListener {
    private static LocaSurfaceView locaSurfaceView = null;

    private int mIndex = 0;
    RemoteSurfaceView mfastImageProcessingView = null;
    FastImageProcessingPipeline mPipeline = null;
    Context mContext = null;
    private MultiInputFilter filter = null;
    ScreenEndpoint mScreen = null;
    private FUBeautifyFilter mBeautifyFilter = null;
    CameraPreviewInput mPreviewInput = null;
    private GLTextureOutputRenderer mWatermark = null;
    WaterMarkPosition mWaterMarkPos = null;

    private boolean bPreview = false;
    private VideoEncoder mEncoder = null;
    private VideoEncoder mDualEncoder = null;
    private boolean mIsEncoding = false;
    private boolean mIsDualEncoding = false;
    private boolean bsartEncoding = false;
    private boolean bAllocatebuf = false;

    private final ConcurrentLinkedQueue<IntBuffer> mGLIntBufferCache = new ConcurrentLinkedQueue<>();
    private int starX = 0;
    private int startY = 0;
    private int mOutWidth = 0;
    private int mOutHeight = 0;
    private int mCount = 10;

    private IntBuffer[] mArrayGLFboBuffer;
    private ByteBuffer mGlPreviewBuffer;
    private final Object writeLock = new Object();

    int mActivityDirector;
    boolean bcreate = false;

    private int capturedFrameCount = 0;
    private double real_fps;
    private long last_time = 0;
    private Thread worker;
    private boolean realocate;

    public static LocaSurfaceView getInstance() {
        if (locaSurfaceView == null) {
            synchronized (MyVideoApi.class) {
                if (locaSurfaceView == null) {
                    locaSurfaceView = new LocaSurfaceView();
                }
            }
        }
        return locaSurfaceView;
    }

    boolean setPreview(boolean bPreview) {
        this.bPreview = bPreview;
        if (mScreen != null) {
            mScreen.setPreView(bPreview);
            return true;
        }
        PviewLog.funEmptyError("setPreview", "ScreenEndpoint", "bcreate : " + bcreate + " | bPreview : " + bPreview);
        return false;
    }

    boolean setDisplayMode(int mode) {
        if (mScreen != null) {
            mScreen.setScaleMode(mode);
            mScreen.reInitialize();
            return true;
        }
        PviewLog.funEmptyError("setDisplayMode", "ScreenEndpoint", "bcreate : " + bcreate + " | mode : " + mode);
        return false;
    }

    public byte[] getLocalBuffer() {
        if (ib != null) {
            synchronized (ib) {
                return ib.array();
            }
        }
        return null;
    }

    public int getRenderWidth() {
        return mOutWidth;
    }

    public int getRenderHeight() {
        return mOutHeight;
    }

    void CreateLocalSurfaceView(final WaterMarkPosition waterMarkPosition) {
        if (bcreate) {
            return;
        }
        bcreate = true;

        real_fps = MyVideoApi.getInstance().getVideoConfig().videoFrameRate;
        last_time = 0;

        mPreviewInput = new CameraPreviewInput(mContext, mfastImageProcessingView);
        mPreviewInput.setActivityOrientation(mActivityDirector);
//        mBeautifyFilter = new BeautifyFilter();
        mBeautifyFilter = new FUBeautifyFilter();
        mScreen = new ScreenEndpoint(mPipeline);
        mPreviewInput.addTarget(mBeautifyFilter);
        mBeautifyFilter.addTarget(mScreen);
        mBeautifyFilter.SetFrameAvaliableListener(this);
        /*mBeautifyFilter.setAmount(0.30f);

        if (waterMarkPosition != null) {
            filter = new WaterMarklBlendFilter(waterMarkPosition);
            filter.addTarget(mScreen);
            filter.registerFilterLocation(mBeautifyFilter);
            filter.SetFrameAvaliableListener(this);
            mBeautifyFilter.addTarget(filter);
        } else {
            mBeautifyFilter.SetFrameAvaliableListener(this);
            mBeautifyFilter.addTarget(mScreen);
        }*/

        mPipeline.addRootRenderer(mPreviewInput);
        mPreviewInput.setCameraCbObj(new CameraPreviewInput.CameraSizeCb() {
            @Override
            public void startPrieview() {
                mPreviewInput.StartCamera();
                mScreen.setPreView(bPreview);

                Camera.Size size = mPreviewInput.getClsSize();
                if (mPreviewInput.getPreviewRotation() == 90
                        || mPreviewInput.getPreviewRotation() == 270) {
                    mScreen.SetRawSize(size.height, size.width);
                } else {
                    mScreen.SetRawSize(size.width, size.height);
                }
                mScreen.SetEncodeSize(mPreviewInput.getOutWidth(), mPreviewInput.getOutHeight());

                if (waterMarkPosition != null) {
                    if (mPreviewInput.getPreviewRotation() == 90
                            || mPreviewInput.getPreviewRotation() == 270) {
                        filter.setRenderSize(size.height, size.width);
                    } else {
                        filter.setRenderSize(size.width, size.height);
                    }
                    mWatermark = new ImageResourceInput(mfastImageProcessingView,
                            waterMarkPosition.activity, waterMarkPosition.resid);
                    filter.registerFilterLocation(mWatermark);
                    mWatermark.addTarget(filter);
                    mPipeline.addRootRenderer(mWatermark);
                }
            }
        });

        PviewLog.d("LocalCamera LocaSurfaceView startRendering ....");
        startRendering();
    }

    private void startRendering() {
        if (mPipeline == null) {
            return;
        }
        mPipeline.startRendering();
    }

    private void AllocateBuffer() {
        if (mPreviewInput == null) {
            return;
        }
        Camera.Size size = mPreviewInput.getClsSize();
        if (size == null) {
            return;
        }
        mOutWidth = mPreviewInput.getOutWidth();
        mOutHeight = mPreviewInput.getOutHeight();
        mArrayGLFboBuffer = new IntBuffer[mCount];
        for (int i = 0; i < mCount; i++) {
            mArrayGLFboBuffer[i] = IntBuffer.allocate(mOutWidth * mOutHeight);
        }
        if (mPreviewInput.getPreviewRotation() == 90) {
            starX = (size.height - mOutWidth) / 2;
            startY = (size.width - mOutHeight) / 2;
        } else {
            starX = (size.width - mOutWidth) / 2;
            startY = (size.height - mOutHeight) / 2;
        }
        mGlPreviewBuffer = ByteBuffer.allocate(mOutWidth * mOutHeight * 4);
        bAllocatebuf = true;
    }

    void setBmEncode(boolean bmEncode) {
        this.mIsEncoding = bmEncode;
    }

    void setDualBmEncode(boolean bmEncode) {
        this.mIsDualEncoding = bmEncode;
    }

    private void StartEncoder() {
        if (!bsartEncoding) {
            //初始化双路视频公用部分，
            Camera.Size size = mPreviewInput.getClsSize();
            if (size == null) {
                return;
            }
            bsartEncoding = true;
            AllocateBuffer();
            enableEncoding();
            PviewLog.i(PviewLog.TAG, "Camera取值 camera size width : " + size.width + " | height : " + size.height);
            PviewLog.i(PviewLog.TAG, "视频编码尺寸 encode video mOutWidth : " + mOutWidth + " | mOutHeight : " + mOutHeight);
        }
        if (mIsEncoding) {//需要编码第一路
            synchronized (this) {
                if (mEncoder == null) {//如果编码器为空，初始编码器。
                    try {
                        mEncoder = VideoEncoder.getInstance();
                        mEncoder.setResolution(mOutWidth, mOutHeight);
                        mEncoder.setEnableSoftEncoder(false);
                        mEncoder.start();
                        PviewLog.i(PviewLog.TAG, "第一路视频硬编成功");
                    } catch (Exception e) {
                        PviewLog.i(PviewLog.TAG, "第一路视频硬编失败，转软编尝试");
                        try {
                            mEncoder.setEnableSoftEncoder(true);
                            mEncoder.start();
                        } catch (Exception e1) {
                            e1.printStackTrace();
                            PviewLog.i(PviewLog.TAG, "第一路视频硬编软编都失败");
                            if (mEncoder != null) {//关闭编码器
                                mEncoder.stop();
                            }
                            mEncoder = null;
                        }
                    }
                }
            }
        } else {//不需要编第一路
            synchronized (this) {
                if (mEncoder != null) {//关闭编码器
                    mEncoder.stop();
                }
                mEncoder = null;
            }
        }

        if (mIsDualEncoding) {//需要编码第二路
            synchronized (this) {
                if (mDualEncoder == null) {//如果编码器为空，初始编码器。
                    try {
                        mDualEncoder = VideoEncoder.getInstance();
                        //指定为第二路编码，编码后回调函数不一样。
                        mDualEncoder.setDualVideo(true);
                        //第二路视频为软编码
                        mDualEncoder.setEnableSoftEncoder(true);
                        //第二路编码器宽高暂时定为原来的一半，以后根据需求改变。
                        mDualEncoder.setResolution(mOutWidth / 2, mOutHeight / 2);
                        mDualEncoder.start();
                        PviewLog.i(PviewLog.TAG, "第二路软编成功");
                    } catch (Exception e) {
                        PviewLog.i(PviewLog.TAG, "第二路软编失败");
                        if (mDualEncoder != null) {//关闭编码器
                            mDualEncoder.stop();
                        }
                        mDualEncoder = null;
                    }
                }
            }
        } else {//不需要编第二路
            synchronized (this) {
                if (mDualEncoder != null) {//关闭编码器
                    mDualEncoder.stop();
                }
                mDualEncoder = null;
            }
        }
    }

    private void FreeEncoder() { // EncoderEngine
        if (!bsartEncoding) {
            return;
        }

        disableEncoding();

        bsartEncoding = false;
        //关闭第一路解码器
        if (mEncoder != null) {
            mEncoder.stop();
        }
        mEncoder = null;
        //关闭第二路解码器
        if (mDualEncoder != null) {
            mDualEncoder.stop();
        }
        mDualEncoder = null;
    }

    public void FreeAll() {
        if (!bcreate) {
            return;
        }
        bcreate = false;
        mPipeline.pauseRendering();
        mPreviewInput.StopCamera();
        mPreviewInput.removeTarget(mBeautifyFilter);
        if (filter != null) {
            filter.clearRegisteredFilterLocations();
            filter.removeTarget(mScreen);
            mBeautifyFilter.removeTarget(filter);
            mWatermark.removeTarget(filter);
            mPipeline.removeRootRenderer(mWatermark);
        } else {
            mBeautifyFilter.removeTarget(mScreen);
        }
        mPipeline.removeRootRenderer(mPreviewInput);

        FreeEncoder();

        mScreen.destroy();
        if (filter != null) {
            mWatermark.destroy();
            filter.destroy();
        }
        mBeautifyFilter.destroy();
        mPreviewInput.destroy();

        mScreen = null;
        mBeautifyFilter = null;
        mPreviewInput = null;
        mArrayGLFboBuffer = null;
        mGlPreviewBuffer = null;
        bAllocatebuf = false;
    }

    private void enableEncoding() { // EncoderEngine
        worker = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.interrupted()) {
                    synchronized (mGLIntBufferCache) {
                        while (!mGLIntBufferCache.isEmpty()) {
                            IntBuffer picture = mGLIntBufferCache.poll();
                            mGlPreviewBuffer.asIntBuffer().put(picture.array());
                            synchronized (this) {
                                if (mEncoder != null) {
                                    mEncoder.onGetRgbaFrame(mGlPreviewBuffer.array(), mOutWidth, mOutHeight);
                                }
                                if (mDualEncoder != null) {
                                    mDualEncoder.onGetRgbaFrame(mGlPreviewBuffer.array(), mOutWidth, mOutHeight);
                                }
                            }
                            mGlPreviewBuffer.clear();
                            picture.clear();
                        }
                    }

                    // Waiting for next frame
                    synchronized (writeLock) {
                        try {
                            writeLock.wait(30);
                        } catch (InterruptedException ie) {
                            worker.interrupt();
                        }
                    }
                }
            }
        });
        worker.start();
    }

    private void disableEncoding() { // EncoderEngine
        if (worker != null) {
            worker.interrupt();
            try {
                worker.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
                worker.interrupt();
            }
            worker = null;
            mGLIntBufferCache.clear();
        }
    }

    private IntBuffer getIntBuffer() {
        if (mIndex > mCount - 1) {
            mIndex = 0;
        }
        return mArrayGLFboBuffer[mIndex++];
    }

    ByteBuffer ib;

    private void putIntBuffer() {

        capturedFrameCount++;

        if (needDropThisFrame()) {
            return;
        }

        if (mIsEncoding || mIsDualEncoding) {
            StartEncoder();
            if (bAllocatebuf) {
                if (Constants.IS_UNITY) {
                    if (ib == null)
                        ib = ByteBuffer.allocate(mOutWidth * mOutHeight * 4);
                    synchronized (ib) {
                        GLES20.glReadPixels(0, 0, mOutWidth, mOutHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, ib);
                    }
                }
                synchronized (mGLIntBufferCache) {
                    if (realocate) {
                        AllocateBuffer();
                    }

                    IntBuffer mGLFboBuffer = getIntBuffer();
                    GLES20.glReadPixels(starX, startY, mOutWidth, mOutHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, mGLFboBuffer);
                    if (mGLIntBufferCache.size() >= mCount) {
                        IntBuffer picture = mGLIntBufferCache.poll();
                        picture.clear();
                    }
                    mGLIntBufferCache.add(mGLFboBuffer);
                }
            }
        } else {
            FreeEncoder();
        }
    }

    boolean switchCamera(boolean bfront) {
        if (mPreviewInput == null) {
            PviewLog.funEmptyError("switchCamera->switchCamera", "mPreviewInput", String.valueOf(bfront));
            return false;
        }
        int camerid = mPreviewInput.getmCamId();
        if (bfront && camerid == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            return true;
        }
        if (bfront) {
            camerid = 1;
        } else {
            camerid = 0;
        }
        mPreviewInput.switchCarmera(camerid);
        mScreen.reInitialize();
        realocate = true;
        return true;
    }

    void Reset() {
        real_fps = MyVideoApi.getInstance().getVideoConfig().videoFrameRate;;
        last_time = 0;

        if (mfastImageProcessingView != null && mfastImageProcessingView.getHolder().getSurface().isValid()) {
            FreeAll();
            CreateLocalSurfaceView(mWaterMarkPos);
        }
    }


    public synchronized void createFBOTextureBinder() {
        FBOTextureBinder mFBOTextureBinder = new FBOTextureBinder(mfastImageProcessingView.getContext());
        mPreviewInput.setFBOTextureBinder(mFBOTextureBinder);
        mFBOTextureBinder.initEGLHelper();

        mFBOTextureBinder.initLocalThread();

        mFBOTextureBinder.initGameRenderGLES(mPipeline, EGL10.EGL_NO_CONTEXT);
        PviewLog.d("LocalCamera LocaSurfaceView startGameRender ....");
        mFBOTextureBinder.startGameRender();
    }

    @Override
    public void OnFrameAvaliable(int width, int height) {
        if (GlobalConfig.mIsScreenRecordShare != null && !GlobalConfig.mIsScreenRecordShare.get()) {
            PviewLog.wf("VideoEncoder OnFrameAvaliable width : " + width + " | height : " + height);
            putIntBuffer();
        }
    }

    public void changeBitrate(int bitrate) {
        synchronized (this) {
            if (mEncoder != null) {
//                mEncoder.changeBitrate(bitrate);
            }
        }
    }

    public void changeFps(int fps) {
        real_fps = fps;
    }

    private boolean needDropThisFrame() {

        int videoFrameRate = MyVideoApi.getInstance().getVideoConfig().videoFrameRate;

        if ((int)real_fps == videoFrameRate) {
            return false;
        }

        if (real_fps/videoFrameRate > 0.5)
        {
            int drop_interval = videoFrameRate/(videoFrameRate-(int)real_fps);
            if (capturedFrameCount % drop_interval == 0)
            {
                //printf("------ drop_interval is [%d] [%d] -------\n", drop_interval, capturedFrameCount);
                return true;
            }
        }
        else
        {
            int frame_period = 1000/(int)real_fps;

            int nTickErr;
            long timediff = System.nanoTime() / 1000 - last_time;
            //printf("------ real_fps is [%f] [%d] [%lld] -------\n", real_fps, capturedFrameCount, timediff);
            if (timediff < frame_period)
            {
                return true;
            }
            last_time = System.nanoTime() / 1000;
        }

        return false;
    }
}
