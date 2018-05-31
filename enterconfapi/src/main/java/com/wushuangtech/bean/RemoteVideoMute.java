package com.wushuangtech.bean;

/**
 * Created by wangzhiguo on 17/7/4.
 */
public class RemoteVideoMute {

    private long mUserID;
    private boolean mIsMuted;

    public long getUserID() {
        return mUserID;
    }

    public void setUserID(long mUserID) {
        this.mUserID = mUserID;
    }

    public boolean isIsMuted() {
        return mIsMuted;
    }

    public void setIsMuted(boolean mIsMuted) {
        this.mIsMuted = mIsMuted;
    }

}
