package com.wushuangtech.library;


public class User {

    private boolean mIsEnableDualVideo;
    private long mUserId;
    private int mUserIdentity = Constants.CLIENT_ROLE_AUDIENCE;
    private int mLastReceiveAudioDatas;
    private int mVideoSteamType = Constants.VIDEO_STEAM_TYPE_BIG;
    private boolean mAudioMuted;
    private boolean mVideoMuted;

    private boolean mIsOpenBigVideo;
    private boolean mIsOpenSmallVideo;

    public User(long mUserId) {
        this.mUserId = mUserId;
    }

    public int getUserIdentity() {
        return mUserIdentity;
    }

    public long getmUserId() {
        return mUserId;
    }

    public int getLastReceiveAudioDatas() {
        return mLastReceiveAudioDatas;
    }

    public void setUserIdentity(int mUserIdentity) {
        this.mUserIdentity = mUserIdentity;
    }

    public void setLastReceiveAudioDatas(int mLastReceiveAudioDatas) {
        this.mLastReceiveAudioDatas = mLastReceiveAudioDatas;
    }

    public boolean isEnableDualVideo() {
        return mIsEnableDualVideo;
    }

    public void setEnableDualVideo(boolean mIsEnableDualVideo) {
        this.mIsEnableDualVideo = mIsEnableDualVideo;
    }

    public boolean isOpenBigVideo() {
        return mIsOpenBigVideo;
    }

    public void setOpenBigVideo(boolean mIsOpenBigVideo) {
        this.mIsOpenBigVideo = mIsOpenBigVideo;
    }

    public boolean isOpenSmallVideo() {
        return mIsOpenSmallVideo;
    }

    public void setOpenSmallVideo(boolean mIsOpenSmallVideo) {
        this.mIsOpenSmallVideo = mIsOpenSmallVideo;
    }

    public int getVideoSteamType() {
        return mVideoSteamType;
    }

    public void setVideoSteamType(int mVideoSteamType) {
        this.mVideoSteamType = mVideoSteamType;
    }

    public void setAudioMuted(boolean muted) {
        mAudioMuted = muted;
    }
    public boolean audioMuted() {
        return mAudioMuted;
    }

    public void setVideoMuted(boolean muted) {
        mVideoMuted = muted;
    }
    public boolean videoMuted() {
        return mVideoMuted;
    }
}
