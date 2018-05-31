package com.wushuangtech.bean;

import java.io.Serializable;

/**
 * 配置外接视频源的信息类
 */
public class ConfVideoFrame implements Serializable{

    /**
     * Texture 2D图像格式
     */
    public static final int FORMAT_TEXTURE_2D = 10;
    /**
     * Texture OES图像格式
     */
    public static final int FORMAT_TEXTURE_OES = 11;
    /**
     * I420图像格式
     */
    public static final int FORMAT_I420 = 1;
    /**
     * NV21图像格式
     */
    public static final int FORMAT_NV21 = 3;
    /**
     * RGBA图像格式
     */
    public static final int FORMAT_RGBA = 4;

    /**
     * 外部提供的视频格式
     */
    public int format;
    /**
     * 外部提供的视频源宽度
     */
    public int stride;
    /**
     * 外部提供的视频源高度
     */
    public int height;
    /**
     * 外部提供的OPENGL的上下文，11版本
     */
    public javax.microedition.khronos.egl.EGLContext eglContext11;
    /**
     * 外部提供的OPENGL的上下文，14版本
     */
    public android.opengl.EGLContext eglContext14;
    /**
     * 外部提供的OPENGL的texture ID
     */
    public int textureID;

    /**
     * 设置是否等待前一帧编码完成。true等待,false不等待
     */
    public boolean syncMode;
    /**
     * 传入一个 4x4 的变换矩阵，典型值是传入一个单位矩阵
     */
    public float[] transform;

    /**
     * 传入视频帧的内容数据
     */
    public byte[] buf;
    /**
     * 指定是否对传入的视频组做旋转操作，可选值为 0， 90， 180， 270。默认为 180
     */
    public int rotation;

    public ConfVideoFrame() {
        this.format = 10;
        this.stride = 0;
        this.height = 0;
        this.textureID = 0;
        this.syncMode = false;
        this.transform = null;
        this.eglContext11 = null;
        this.eglContext14 = null;
        this.buf = null;
        this.rotation = 180;
    }
}
