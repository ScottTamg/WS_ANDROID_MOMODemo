package com.wushuangtech.library;


import android.text.TextUtils;
import android.util.LongSparseArray;

import com.wushuangtech.api.ExternalChatModule;
import com.wushuangtech.api.JniWorkerThread;
import com.wushuangtech.bean.ConfVideoFrame;
import com.wushuangtech.bean.RtcStats;
import com.wushuangtech.bean.TTTVideoCanvas;
import com.wushuangtech.inter.TTTInterfaceTestCallBack;
import com.wushuangtech.inter.TTTRtcEngineEventInter;
import com.wushuangtech.library.screenrecorder.VideoEncoderCore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class GlobalHolder {

    private volatile static GlobalHolder holder;
    private TTTRtcEngineEventInter mCommunicationHelper;
    private VideoEncoderCore.AudioDataCallBack mAudioDataCallBack;
    private TTTInterfaceTestCallBack mTTTInterfaceTestCallBack;

    private LongSparseArray<User> mUserHolder = new LongSparseArray<>();
    private LongSparseArray<UserDeviceConfig> mUserDeviceList = new LongSparseArray<>();
    private HashMap<String, Boolean> mRemoteFirstDecoders = new HashMap<>();
    private List<String> mHardwareDecoders = new ArrayList<>();
    private Vector<TTTVideoCanvas> mWaitOpenDevices = new Vector<>();

    private Lock mUserLock = new ReentrantLock();
    private Lock mDecoderLock = new ReentrantLock();
    private Lock mMuteLock = new ReentrantLock();
    private RtcStats mRtcStats = new RtcStats();
    private JniWorkerThread mWorkerThread;
    private int mCurrentAudioRoute = -1;

    public static GlobalHolder getInstance() {
        if (holder == null) {
            synchronized (GlobalHolder.class) {
                if (holder == null) {
                    holder = new GlobalHolder();
                }
            }
        }
        return holder;
    }

    public void clearDatas() {
        mUserDeviceList.clear();
        mUserHolder.clear();
        mHardwareDecoders.clear();
        mRemoteFirstDecoders.clear();
        mWaitOpenDevices.clear();
    }

    public User putOrUpdateUser(long nUserID) {
        User user;
        try {
            mUserLock.lock();
            boolean isContained = true;
            user = mUserHolder.get(nUserID);
            if (user == null) {
                isContained = false;
                user = new User(nUserID);
            }

            if (!isContained) {
                mUserHolder.put(user.getmUserId(), user);
            }
        } finally {
            mUserLock.unlock();
        }
        return user;
    }

    public User getUser(long userID) {
        if (userID <= 0) {
            return null;
        }

        try {
            mUserLock.lock();
            User tmp = mUserHolder.get(userID);
            if (tmp != null) {
                return tmp;
            }
        } finally {
            mUserLock.unlock();
        }
        return null;
    }

    public List<User> getUsers() {
        ArrayList<User> result = new ArrayList<>();
        try {
            mUserLock.lock();
            for (int i = 0; i < mUserHolder.size(); i++) {
                User user = mUserHolder.valueAt(i);
                result.add(user);
            }
        } finally {
            mUserLock.unlock();
        }
        return result;
    }

    public RtcStats getRtcStats() {
        return mRtcStats;
    }

    public HashMap<String, Boolean> getRemoteFirstDecoders() {
        return mRemoteFirstDecoders;
    }

    public void removeRemoteFirstDecoders(String devID) {
        mRemoteFirstDecoders.remove(devID);
    }

    void delUser(long nUserID) {
        try {
            mUserLock.lock();
            mUserHolder.delete(nUserID);
        } finally {
            mUserLock.unlock();
        }
    }

    public JniWorkerThread getWorkerThread() {
        return mWorkerThread;
    }

    public int getHardwareDecoderSize() {
        return mHardwareDecoders.size();
    }

    public List<TTTVideoCanvas> getWaitOpenDevices() {
        return mWaitOpenDevices;
    }

    public void setWorkerThread(JniWorkerThread mWorkerThread) {
        this.mWorkerThread = mWorkerThread;
    }

    public void setRtcStats(RtcStats mRtcStats) {
        this.mRtcStats = mRtcStats;
    }

    public TTTRtcEngineEventInter getCommunicationHelper() {
        return mCommunicationHelper;
    }

    public UserDeviceConfig getUserDefaultDevice(long uid) {
        return mUserDeviceList.get(uid);
    }

    public long getUserByDeviceID(String deviceID) {
        if (TextUtils.isEmpty(deviceID)) {
            return -1;
        }

        for (int i = 0; i < mUserDeviceList.size(); i++) {
            UserDeviceConfig userDeviceConfig = mUserDeviceList.valueAt(i);
            String tempDeviceID = userDeviceConfig.getDeviceID();
            if (deviceID.equals(tempDeviceID)) {
                return mUserDeviceList.keyAt(i);
            }
        }
        return -1;
    }

    public void updateUserDevice(long id, UserDeviceConfig udc) {
        mUserDeviceList.remove(id);
        mUserDeviceList.put(id, udc);
    }

    public void setCommunicationHelper(TTTRtcEngineEventInter mCommunicationHelper) {
        this.mCommunicationHelper = mCommunicationHelper;
    }

    public void setAudioDataCallBack(VideoEncoderCore.AudioDataCallBack mAudioDataCallBack) {
        this.mAudioDataCallBack = mAudioDataCallBack;
    }

    public void setTTTInterfaceTestCallBack(TTTInterfaceTestCallBack mTTTInterfaceTestCallBack) {
        this.mTTTInterfaceTestCallBack = mTTTInterfaceTestCallBack;
    }

    public void notifyCHFirstRemoteVideoDraw(String deviceID, int width, int height) {
        if (mUserDeviceList.size() > 0) {
            for (int i = 0; i < mUserDeviceList.size(); i++) {
                UserDeviceConfig udc = mUserDeviceList.valueAt(i);
                if (deviceID.equals(udc.getDeviceID())) {
                    long uid = mUserDeviceList.keyAt(i);
                    mWorkerThread.sendMessage(JniWorkerThread.JNI_CALL_BACK_REMOTE_VIDEO_FIRST_FRAME,
                            new Object[]{uid, width, height});
                    break;
                }
            }
        }
    }

    public void notifyCHFirstRemoteVideoDecoder(String deviceID, int width, int height, int elapsed) {
        if (mUserDeviceList.size() > 0) {
            for (int i = 0; i < mUserDeviceList.size(); i++) {
                UserDeviceConfig udc = mUserDeviceList.valueAt(i);
                if (deviceID.equals(udc.getDeviceID())) {
                    long uid = mUserDeviceList.keyAt(i);
                    mWorkerThread.sendMessage(JniWorkerThread.JNI_CALL_BACK_REMOTE_VIDEO_FIRST_DECODE,
                            new Object[]{uid, width, height, elapsed});
                    break;
                }
            }
        }
    }

    public void notifyCHRemoteVideoDecoder(byte[] buf, String deviceID, int width, int height) {
        if (mUserDeviceList.size() > 0) {
            for (int i = 0; i < mUserDeviceList.size(); i++) {
                UserDeviceConfig udc = mUserDeviceList.valueAt(i);
                if (deviceID.equals(udc.getDeviceID())) {
                    long uid = mUserDeviceList.keyAt(i);
                    ConfVideoFrame mConfVideoFrame = new ConfVideoFrame();
                    mConfVideoFrame.format = ConfVideoFrame.FORMAT_RGBA;
                    mConfVideoFrame.buf = buf;
                    mConfVideoFrame.stride = width;
                    mConfVideoFrame.height = height;
                    mWorkerThread.sendMessage(JniWorkerThread.JNI_CALL_BACK_REMOTE_VIDEO_DECODE,
                            new Object[]{uid, mConfVideoFrame});
                    break;
                }
            }
        }
    }

    public void notifyCHAudioRouteChanged(int routing) {
        if (mWorkerThread != null) {
            if (mCurrentAudioRoute == routing) {
                return;
            }
            mCurrentAudioRoute = routing;
            mWorkerThread.sendMessage(JniWorkerThread.JNI_CALL_BACK_AUDIO_ROUTE_CHANGE,
                    new Object[]{routing});
        }
    }

    public void notifyCHChannelNameError() {
        if (mWorkerThread != null) {
            mWorkerThread.sendMessage(JniWorkerThread.JNI_CALL_BACK_ON_ERROR,
                    new Object[]{Constants.ERROR_ENTER_ROOM_INVALIDCHANNELNAME});
        }
    }

    public void notifyCHChannelKeyExpire() {
        if (mWorkerThread != null) {
            mWorkerThread.sendMessage(JniWorkerThread.JNI_CALL_BACK_ON_ERROR,
                    new Object[]{Constants.ERROR_KICK_BY_CHANNELKEYEXPIRED});
        }
    }

    public void notifyCHRequestChannelKey() {
        if (mWorkerThread != null) {
            mWorkerThread.sendMessage(JniWorkerThread.JNI_CALL_BACK_REQUEST_CHANNEL_KEY,
                    new Object[]{});
        }
    }

    public void notifyCHVideoStop() {
        if (mWorkerThread != null) {
            mWorkerThread.sendMessage(JniWorkerThread.JNI_CALL_BACK_VIDEO_STOPPED,
                    new Object[]{});
        }
    }

    public void notifyAudioDataToWrite(byte[] datas) {
        if (mAudioDataCallBack != null) {
            mAudioDataCallBack.pushEncodedAudioData(datas);
        }
    }

    public void notifyCHRTMPStatus(int type) {
        if (mWorkerThread != null) {
            mWorkerThread.sendMessage(JniWorkerThread.JNI_CALL_BACK_ON_RTMP_STATUS,
                    new Object[]{type});
        }
    }

    public void notifyCHIJKSei(String sei) {
        if (mWorkerThread != null) {
            mWorkerThread.sendMessage(JniWorkerThread.JNI_CALL_BACK_ON_IJK_H264_SEI,
                    new Object[]{sei});
        }
    }

    public void notifyCHTestString(String testString) {
        if (mTTTInterfaceTestCallBack != null) {
            mTTTInterfaceTestCallBack.reportTestString(testString);
        }
    }

    public boolean checkHardwareDecoderSize(String mDeviceID) {
        try {
            mDecoderLock.lock();
            if (mHardwareDecoders.size() >= 1) {
                return false;
            } else {
                mHardwareDecoders.add(mDeviceID);
                return true;
            }
        } finally {
            mDecoderLock.unlock();
        }
    }

    public void closeHardwareDecoder(String mDeviceID) {
        try {
            mDecoderLock.lock();
            mHardwareDecoders.remove(mDeviceID);
        } finally {
            mDecoderLock.unlock();
        }
    }

    public void updateAudioMuted(long userid, boolean muted) {
        try {
            mMuteLock.lock();
            User user = mUserHolder.get(userid);
            if (user != null) {
                user.setAudioMuted(muted);
            }
        } finally {
            mMuteLock.unlock();
        }
    }

    public boolean isAudioMuted(long userid) {
        try {
            mMuteLock.lock();
            User user = mUserHolder.get(userid);
            return user != null && user.audioMuted();
        } finally {
            mMuteLock.unlock();
        }
    }

    public void updateVideoMuted(long userid, boolean muted) {
        try {
            mMuteLock.lock();
            User user = mUserHolder.get(userid);
            if (user != null) {
                user.setVideoMuted(muted);
            }
        } finally {
            mMuteLock.unlock();
        }
    }

    public boolean isVideoMuted(long userid) {
        try {
            mMuteLock.lock();
            User user = mUserHolder.get(userid);
            return user != null && user.videoMuted();
        } finally {
            mMuteLock.unlock();
        }
    }


    public Object handleChatModule(int actionType, Object... objs) {
        ExternalChatModule instance = ExternalChatModule.getInstance();
        if (instance != null) {
            return ExternalChatModule.getInstance().handleActionEvent(actionType, (Object[]) objs);
        }
        return null;
    }

}
