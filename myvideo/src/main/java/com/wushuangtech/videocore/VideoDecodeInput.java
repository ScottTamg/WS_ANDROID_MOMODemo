package com.wushuangtech.videocore;

import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.view.SurfaceHolder;

import com.wushuangtech.library.Constants;
import com.wushuangtech.videocore.com.wushuangtech.fbo.FBOTextureBinder;

import java.util.concurrent.locks.ReentrantLock;

import javax.microedition.khronos.opengles.GL10;

import project.android.imageprocessing.input.GLTextureOutputRenderer;

class VideoDecodeInput extends GLTextureOutputRenderer implements SurfaceTexture.OnFrameAvailableListener {
    private static final String UNIFORM_DISP_MATRIX = "u_Matrix";

    private GLSurfaceView view;
    private SurfaceTexture dispTex;
    private int matrixHandle;
    private float[] matrix = new float[16];
    private FBOTextureBinder mFBOTextureBinder;

    private ReentrantLock mLock;

    VideoDecodeInput(GLSurfaceView view) {
        super();
        this.view = view;
        view.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mLock = new ReentrantLock();
    }

    /* (non-Javadoc)
     * @see project.android.imageprocessing.input.GLTextureOutputRenderer#destroy()
	 */
    @Override
    public void destroy() {
        super.destroy();
        try {
            mLock.lock();
            if (dispTex != null) {
                dispTex.release();
                dispTex = null;
            }
            if (texture_in != 0) {
                int[] tex = new int[1];
                tex[0] = texture_in;
                GLES20.glDeleteTextures(1, tex, 0);
                texture_in = 0;
            }
        } finally {
            mLock.unlock();
        }
    }

    @Override
    protected void drawFrame() {
        try {
            dispTex.updateTexImage();
            super.drawFrame();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected String getFragmentShader() {
        return
                "#extension GL_OES_EGL_image_external : require\n"
                        + "precision mediump float;\n"
                        + "uniform samplerExternalOES " + UNIFORM_TEXTURE0 + ";\n"
                        + "varying vec2 " + VARYING_TEXCOORD + ";\n"

                        + "void main() {\n"
                        + "   gl_FragColor = texture2D(" + UNIFORM_TEXTURE0 + ", " + VARYING_TEXCOORD + ");\n"
                        + "}\n";
    }

    @Override
    protected String getVertexShader() {
        return
                "uniform mat4 " + UNIFORM_DISP_MATRIX + ";\n"
                        + "attribute vec4 " + ATTRIBUTE_POSITION + ";\n"
                        + "attribute vec2 " + ATTRIBUTE_TEXCOORD + ";\n"
                        + "varying vec2 " + VARYING_TEXCOORD + ";\n"

                        + "void main() {\n"
                        + "   vec4 texPos = " + UNIFORM_DISP_MATRIX + " * vec4(" + ATTRIBUTE_TEXCOORD + ", 1, 1);\n"
                        + "   " + VARYING_TEXCOORD + " = texPos.xy;\n"
                        + "   gl_Position = " + ATTRIBUTE_POSITION + ";\n"
                        + "}\n";
    }

    @Override
    protected void initShaderHandles() {
        super.initShaderHandles();
        matrixHandle = GLES20.glGetUniformLocation(programHandle, UNIFORM_DISP_MATRIX);
    }

    private void initPreviewGLContext() {
        try {
            mLock.lock();
            if (dispTex != null) {
                dispTex.release();
                dispTex = null;
            }
            if (texture_in != 0) {
                int[] tex = new int[1];
                tex[0] = texture_in;
                GLES20.glDeleteTextures(1, tex, 0);
                texture_in = 0;
            }

            int[] textures = new int[1];
            GLES20.glGenTextures(1, textures, 0);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0]);
            GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
            GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
            texture_in = textures[0];
            dispTex = new SurfaceTexture(texture_in);
            dispTex.setOnFrameAvailableListener(this);
            String mDevID = RemotePlayerManger.getInstance().getDevId((RemoteSurfaceView) view);
//            // TODO start decoder here or in ontextureavaliable
            synchronized (this) {
//                boolean isUseHardwareDecoder = GlobalHolder.getInstance().checkHardwareDecoderSize(mDevID);
                boolean isExist = VideoDecoderManager.getInstance().findVideoDecoderByID(mDevID);
                VideoDecoder newVideoDecoder;
                if (!isExist) {
                    newVideoDecoder = VideoDecoderManager.getInstance().createNewVideoDecoder(mDevID);
                } else {
                    newVideoDecoder = VideoDecoderManager.getInstance().getVideoDecoderByID(mDevID);
                }
                // 将SurfaceTexture设置给解码器，用于绘制图形
                if (dispTex != null) {
                    newVideoDecoder.setSurfaceTexture(dispTex);
                }
            }
        } finally {
            mLock.unlock();
        }
    }

    @Override
    protected void initWithGLContext() {
        super.initWithGLContext();
        initPreviewGLContext();
    }

    public void setFBOTextureBinder(FBOTextureBinder fboTextureBinder){
        this.mFBOTextureBinder = fboTextureBinder;
    }

    /* (non-Javadoc)
     * @see android.graphics.SurfaceTexture.OnFrameAvailableListener#onFrameAvailable(android.graphics.SurfaceTexture)
	 */
    @Override
    public void onFrameAvailable(SurfaceTexture arg0) {
        markAsDirty();
        if (Constants.IS_UNITY) {
            if (mFBOTextureBinder != null) {
                mFBOTextureBinder.startGameRender();
            }
        } else {
            view.requestRender();
        }
    }

    @Override
    protected void passShaderValues() {
        renderVertices.position(0);
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 8, renderVertices);
        GLES20.glEnableVertexAttribArray(positionHandle);
        textureVertices[curRotation].position(0);
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 8, textureVertices[curRotation]);
        GLES20.glEnableVertexAttribArray(texCoordHandle);
        bindTexture();
        GLES20.glUniform1i(textureHandle, 0);
        dispTex.getTransformMatrix(matrix);
        GLES20.glUniformMatrix4fv(matrixHandle, 1, false, matrix, 0);
    }

    private void bindTexture() {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture_in);
    }

    @Override
    public void setRenderSize(int width, int height) {
        super.setRenderSize(width, height);
    }
}