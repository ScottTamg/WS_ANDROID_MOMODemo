package com.wushuangtech.wstechapi.model;

import android.view.SurfaceView;

import com.wushuangtech.library.Constants;

/**
 * 设置视频属性的类
 */
public class VideoCanvas {

    /**
     * 该视频属性类所属用户ID
     */
    private long mUserID;

    /**
     * 设置视频显示的模式
     */
    private int mShowMode;

    /**
     * 视频显示视窗
     */
    private SurfaceView mSurface;


    /**
     * 视频属性类的构造函数
     *
     * @param mUserID   该视频属性类所属用户ID
     * @param mShowMode 设置视频显示的模式 see {@link Constants#RENDER_MODE_HIDDEN}
     * @param mSurface  视频显示视窗
     */
    public VideoCanvas(long mUserID, int mShowMode, SurfaceView mSurface) {
        this.mUserID = mUserID;
        this.mShowMode = mShowMode;
        this.mSurface = mSurface;
    }

    public long getUserID() {
        return mUserID;
    }

    public int getShowMode() {
        return mShowMode;
    }

    public SurfaceView getSurface() {
        return mSurface;
    }

    public void setUserID(long mUserID) {
        this.mUserID = mUserID;
    }

    public void setShowMode(int mShowMode) {
        this.mShowMode = mShowMode;
    }

    public void setSurface(SurfaceView mSurface) {
        this.mSurface = mSurface;
    }

}
