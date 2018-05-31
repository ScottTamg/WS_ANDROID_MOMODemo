package com.wushuangtech.videocore;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

import com.wushuangtech.bean.ConfVideoFrame;
import com.wushuangtech.videocore.com.wushuangtech.fbo.FBOTextureBinder;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;

public class EncoderEngine {

    private static volatile EncoderEngine mInstance = null;

    private EncoderEngine() {
    }

    private int mIndex;
    private int mCount = 10;
    private int mReceiveWidth;
    private int mReceiveHeight;
    private int mEncodeWidth = 240;
    private int mEncodeHeight = 320;


    private int mTextureStartX;
    private int mTextureStartY;

    private boolean bAllocatebuf;
    private boolean bsartEncoding;

    private IntBuffer[] mArrayGLFboBuffer;
    private ByteBuffer mGlPreviewBuffer;

    private VideoEncoder mEncoder;
    private Thread worker;
    private final Object writeLock = new Object();
    private final ConcurrentLinkedQueue<IntBuffer> mGLIntBufferCache = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<ConfVideoFrame> mGLByteByfferCache = new ConcurrentLinkedQueue<>();
    private FBOTextureBinder mRenderer;

    public static EncoderEngine getInstance() {
        if (mInstance == null) {
            synchronized (EncoderEngine.class) {
                if (mInstance == null) {
                    mInstance = new EncoderEngine();
                }
            }
        }
        return mInstance;
    }

    public void setEncodeWH(int mEncodeWidth, int mEncodeHeight) {
        this.mEncodeWidth = mEncodeWidth;
        this.mEncodeHeight = mEncodeHeight;
    }

    private void calcTextureStartXY() {
        float preRate = (float) mReceiveWidth / (float) mReceiveHeight; // 屏幕宽高
        float capRate = (float) mEncodeWidth / (float) mEncodeHeight; //预览宽高

        int renderWidth;
        int renderHeight;

        if (preRate >= capRate) {
            renderHeight = (int) (mReceiveWidth / capRate);//调整render高度，使比例相同，宽度填满view，高度延伸到view外面。
            mTextureStartY = (mReceiveHeight - renderHeight) / 2;
            mTextureStartX = 0;
        } else {
            renderWidth = (int) (mReceiveHeight * capRate);//调整render宽度，使比例相同，高度填满view，宽度延伸到view外面。
            mTextureStartX = (mReceiveWidth - renderWidth) / 2;
            mTextureStartY = 0;
        }
    }

    public boolean encodVideoFrame(ConfVideoFrame mConfVideoFrame) {
        // 如果宽高的值发生变化，需要重新初始化编码器
        boolean isFrameChanged = checkFrameSizeChanged(mConfVideoFrame);
        if (isFrameChanged) {
            FreeEncoder();
        }
        mReceiveWidth = mConfVideoFrame.stride;
        mReceiveHeight = mConfVideoFrame.height;
        initYUVEncoder();
        synchronized (mGLByteByfferCache) {
            if (mGLByteByfferCache.size() >= mCount) {
                mGLByteByfferCache.poll();
            }
            mGLByteByfferCache.add(mConfVideoFrame);
        }
        return true;
    }

    private void initYUVEncoder() {
        if (bsartEncoding) {
            return;
        }

        bsartEncoding = true;
        mEncoder = VideoEncoder.getInstance();
        mEncoder.setResolution(mEncodeWidth, mEncodeHeight);
        try {
            mEncoder.externalEncodeStart();
        } catch (Exception e) {
            e.printStackTrace();
        }
        encodingYUV();
    }

    private void encodingYUV() {
        worker = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.interrupted()) {
                    synchronized (mGLByteByfferCache) {
                        while (!mGLByteByfferCache.isEmpty()) {
                            ConfVideoFrame frame = mGLByteByfferCache.poll();
                            byte[] bytes = mEncoder.changeFormatToTarget(frame);
                            mEncoder.externalGetRgbaFrame(bytes);
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

    // ************************** texture id **************************
    public boolean startDecodeVideoFrame(Context mContext, ConfVideoFrame mConfVideoFrame, boolean isEGL14) {
        // 如果宽高的值发生变化，需要重新初始化编码器
        boolean isFrameChanged = checkFrameSizeChanged(mConfVideoFrame);
        if (isFrameChanged) {
            FreeEncoder();
        }

        mReceiveWidth = mConfVideoFrame.stride;
        mReceiveHeight = mConfVideoFrame.height;
        calcTextureStartXY();

        if (mRenderer == null) {
            mRenderer = new FBOTextureBinder(mContext);
        }

        if (mConfVideoFrame.syncMode) {
            mRenderer.drawFrame(mConfVideoFrame.format
                    , mConfVideoFrame.textureID, mConfVideoFrame.transform, mConfVideoFrame.stride
                    , mConfVideoFrame.height);
            putIntBuffer();
        } else {
            boolean b = mRenderer.initLocalThread();
            if (b) {
                boolean result;
                if (isEGL14) {
                    result = mRenderer.initEGLContext14(mConfVideoFrame.eglContext14,
                            mConfVideoFrame.stride, mConfVideoFrame.height, mConfVideoFrame.textureID
                            , mConfVideoFrame.transform, mConfVideoFrame.format);
                } else {
                    result = mRenderer.initEGLContext11(mConfVideoFrame.eglContext11,
                            mConfVideoFrame.stride, mConfVideoFrame.height, mConfVideoFrame.textureID
                            , mConfVideoFrame.transform, mConfVideoFrame.format);
                }

                if (!result) {
                    return false;
                }
            }
            mRenderer.startEGLTextureEncode();
        }
        return true;
    }

    public void putIntBuffer() {
        StartEncoder();
        if (bAllocatebuf) {
            synchronized (mGLIntBufferCache) {
                IntBuffer mGLFboBuffer = getIntBuffer();
                GLES20.glReadPixels(mTextureStartX, mTextureStartY, mReceiveWidth, mReceiveHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, mGLFboBuffer);
                if (mGLIntBufferCache.size() >= mCount) {
                    IntBuffer picture = mGLIntBufferCache.poll();
                    picture.clear();
                }

                mGLIntBufferCache.add(mGLFboBuffer);
            }
        }
    }

    private void StartEncoder() {
        if (bsartEncoding) {
            return;
        }

        bsartEncoding = true;
        AllocateBuffer();
        mEncoder = VideoEncoder.getInstance();
        mEncoder.setResolution(mEncodeWidth, mEncodeHeight);
        try {
            mEncoder.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        enableEncoding();
    }

    private void AllocateBuffer() {
        mArrayGLFboBuffer = new IntBuffer[mCount];
        for (int i = 0; i < mCount; i++) {
            mArrayGLFboBuffer[i] = IntBuffer.allocate(mReceiveWidth * mReceiveHeight);
        }
        mGlPreviewBuffer = ByteBuffer.allocate(mReceiveWidth * mReceiveHeight * 4);
        bAllocatebuf = true;
    }

    private IntBuffer getIntBuffer() {
        if (mIndex > mCount - 1) {
            mIndex = 0;
        }
        return mArrayGLFboBuffer[mIndex++];
    }

    private void enableEncoding() {
        worker = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.interrupted()) {
                    synchronized (mGLIntBufferCache) {
                        while (!mGLIntBufferCache.isEmpty()) {
                            IntBuffer picture = mGLIntBufferCache.poll();
                            mGlPreviewBuffer.asIntBuffer().put(picture.array());
                            mEncoder.onGetRgbaFrame(mGlPreviewBuffer.array(), mReceiveWidth, mReceiveHeight);
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

    private void disableEncoding() {
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

    public void FreeEncoder() {
        if (!bsartEncoding) {
            return;
        }
        bsartEncoding = false;
        disableEncoding();
        if (mEncoder != null) {
            mEncoder.stop();
        }
        mEncoder = null;
        mRenderer.destoryFBO();
        mRenderer = null;
    }

    private boolean checkFrameSizeChanged(ConfVideoFrame mConfVideoFrame) {
        if (mConfVideoFrame.stride != mReceiveWidth
                || mConfVideoFrame.height != mReceiveHeight) {
            return true;
        }
        return false;
    }
}
