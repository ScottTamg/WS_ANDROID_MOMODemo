package com.wushuangtech.library;

import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.SparseArray;

import com.wushuangtech.inter.InstantRequestCallBack;
import com.wushuangtech.jni.RoomJni;
import com.wushuangtech.utils.PviewLog;

import java.util.Timer;
import java.util.TimerTask;

import static com.wushuangtech.library.LocalSDKConstants.ERROR_FUNCTION_BUSY;
import static com.wushuangtech.library.LocalSDKConstants.FUNCTION_SUCCESS;

/**
 * Created by wangzhiguo on 17/6/1.
 */

public class InstantRequest {

    public static final int REQUEST_JOIN_ROOM = 501;
    private static final int TIME_OUT = 10 * 1000;
    private static InstantRequest mInstantRequest = new InstantRequest();
    private SparseArray<MyTimer> mTasks = new SparseArray<>();

    private InstantRequest() {
    }

    public static InstantRequest getInstance() {
        return mInstantRequest;
    }

    int requestServer(Handler mHandler, int requestType, Object... objs) {
        switch (requestType) {
            case REQUEST_JOIN_ROOM:
                boolean waittingCallBack = isWaittingCallBack(requestType);
                if (waittingCallBack) {
                    return ERROR_FUNCTION_BUSY;
                }
                initTimeOut(REQUEST_JOIN_ROOM, mHandler, objs);
                int role = (int) objs[3];
                PviewLog.d("requestServer -> mTasks size : " + mTasks.size());
                RoomJni.getInstance().RoomQuickEnter((String) objs[0], (long) objs[1],
                        (long) objs[2], role, (String) objs[4], Build.MODEL, false);
                break;
        }
        return FUNCTION_SUCCESS;
    }

    private boolean isWaittingCallBack(int requestType) {
        Timer localTimerTask = mTasks.get(requestType);
        return localTimerTask != null;
    }

    private void initTimeOut(int requestType, Handler mHandler, Object[] objs) {
        LocalConfApiCallBack mLocalConfApiCallBack = new LocalConfApiCallBack(requestType, mHandler, objs);
        RoomJni.getInstance().addCallback(mLocalConfApiCallBack);
        LocalTimerTask mLocalTimerTask = new LocalTimerTask(mLocalConfApiCallBack);
        MyTimer mTimer = new MyTimer(mLocalConfApiCallBack);
        mTimer.schedule(mLocalTimerTask, TIME_OUT);
        mTasks.put(requestType, mTimer);
    }

    private class LocalConfApiCallBack implements InstantRequestCallBack {

        private int mRequestType;
        private Handler mHandler;
        private Object[] objs;

        private LocalConfApiCallBack(int mRequestType, Handler mHandler, Object[] objs) {
            super();
            this.mRequestType = mRequestType;
            this.mHandler = mHandler;
            this.objs = objs;
        }

        void setHandler(Handler mHandler) {
            this.mHandler = mHandler;
        }

        private void sendTimeOutMessage() {
            JNIResponse jniRes = new JNIResponse(JNIResponse.Result.UNKNOWN);
            jniRes.arg1 = Constants.ERROR_ENTER_ROOM_TIMEOUT;
            Message.obtain(mHandler, mRequestType, jniRes).sendToTarget();
            Timer timer = mTasks.get(mRequestType);
            if (timer != null) {
                timer.purge();
            }
            mTasks.delete(mRequestType);

            switch (mRequestType) {
                case REQUEST_JOIN_ROOM:
                    PviewLog.d(PviewLog.TAG, "进房间超时外回调  : " + objs[2]);
                    RoomJni.getInstance().roomExit((Long) objs[2]);
                    break;
            }
        }


        @Override
        public void OnEnterConfCallback(long nConfID, int nResult, int userRole) {
            Log.d("wzg", "InstantRequest OnEnterConfCallback nConfID : " + nConfID + " | nResult : " + nResult
                    + " | userRole : " + userRole);
            MyTimer timer = mTasks.get(mRequestType);
            if (timer != null) {
                RoomJni.getInstance().removeCallback(timer.getLocalConfApiCallBack());
                timer.cancel();
                timer.purge();
            }
            // 在进房间前清理远端用户第一帧回调缓存，最保险
            GlobalHolder.getInstance().getRemoteFirstDecoders().clear();
            mTasks.delete(mRequestType);
            JNIResponse jniRes = new JNIResponse(JNIResponse.Result.fromInt(nResult));
            jniRes.resObj = new Conference(nConfID, userRole);
            jniRes.arg1 = nResult;
            Message.obtain(mHandler, mRequestType, jniRes).sendToTarget();
        }
    }

    private class LocalTimerTask extends TimerTask {


        private LocalConfApiCallBack mLocalConfApiCallBack;

        private LocalTimerTask(LocalConfApiCallBack mLocalConfApiCallBack) {
            this.mLocalConfApiCallBack = mLocalConfApiCallBack;
        }

        @Override
        public void run() {
            RoomJni.getInstance().removeCallback(mLocalConfApiCallBack);
            mLocalConfApiCallBack.sendTimeOutMessage();
            mLocalConfApiCallBack.setHandler(null);
            mLocalConfApiCallBack = null;
        }
    }

    private class MyTimer extends Timer {

        private LocalConfApiCallBack mLocalConfApiCallBack;

        private MyTimer(LocalConfApiCallBack mLocalConfApiCallBack) {
            this.mLocalConfApiCallBack = mLocalConfApiCallBack;
        }

        public LocalConfApiCallBack getLocalConfApiCallBack() {
            return mLocalConfApiCallBack;
        }
    }
}
