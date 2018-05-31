package com.wushuangtech.wstechapi.internal;

import android.app.Service;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.LongSparseArray;

import com.wushuangtech.api.ExternalAudioModule;
import com.wushuangtech.api.ExternalVideoModule;
import com.wushuangtech.api.JniWorkerThread;
import com.wushuangtech.bean.LocalAudioStats;
import com.wushuangtech.bean.LocalVideoStats;
import com.wushuangtech.bean.RemoteAudioStats;
import com.wushuangtech.bean.RtcStats;
import com.wushuangtech.library.GlobalConfig;
import com.wushuangtech.library.GlobalHolder;
import com.wushuangtech.library.LocalSDKConstants;
import com.wushuangtech.library.User;
import com.wushuangtech.wstechapi.model.TTTLocalModuleConfig;
import com.wushuangtech.wstechapi.model.TTTModuleConstants;
import com.wushuangtech.wstechapi.utils.DeviceUtils;

import java.util.List;

/**
 * SDK的工作线程类
 */
public class WorkerThread extends Thread {
    private static final int ACTION_WORKER_THREAD_QUIT = 0X1010; // quit this thread

    private static final int ACTION_WORKER_VIDEO_STATUS = 0X1011;

    private static final int ACTION_WORKER_DATA_STATUS = 0X1012;

    private static final int INTERVAL_TIME = 2000;
    public static final int SPEED_TIME = 1000;
    private static final String TAG = "WorkerThread";
    private WorkerThreadHandler mWorkerHandler;
    private Context mContext;
    private boolean mReady;
    private static int mLastLocalEncodeFrameCount;
    private static int mLastLocalVideoSendDataSize;
    private static int mLastLocalVideoRecvDataSize;
    private static int mLastLocalAudioSendDataSize;
    private static int mLastLocalAudioRecvDataSize;
    private TTTRtcHeadsetListener mHeadsetListener;
    private static TTTRtcEngineImpl mTTTRtcEngine;

    public Context getContext() {
        return mContext;
    }

    final void waitForReady() {
        while (!mReady) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Log.i(TAG, "wait for " + WorkerThread.class.getSimpleName());
        }
    }

    public void setContext(Context mContext) {
        this.mContext = mContext;
    }

    void setTTTRtcEngine(TTTRtcEngineImpl mTTTRtcEngine) {
        this.mTTTRtcEngine = mTTTRtcEngine;
    }

    @Override
    public void run() {
        Log.i(TAG, "start to run");
        Looper.prepare();
        mWorkerHandler = new WorkerThreadHandler(this);
        mReady = true;
        mHeadsetListener = new TTTRtcHeadsetListener(mContext, mTTTRtcEngine);
        mHeadsetListener.registerReceiver();
        DeviceUtils.getInstance().init();
        reset();
        GlobalConfig.mStartRoomTime = System.currentTimeMillis();
        mWorkerHandler.sendEmptyMessageDelayed(ACTION_WORKER_DATA_STATUS, SPEED_TIME);
        mWorkerHandler.sendEmptyMessageDelayed(ACTION_WORKER_VIDEO_STATUS, INTERVAL_TIME);
        // 电话监听
        TelephonyManager mTelephonyManager = (TelephonyManager) mContext.getSystemService(Service.TELEPHONY_SERVICE);
        TTTRtcPhoneListener mTTTRtcPhoneListener = new TTTRtcPhoneListener(mContext);
        if (mTelephonyManager != null) {
            mTelephonyManager.listen(mTTTRtcPhoneListener, PhoneStateListener.LISTEN_CALL_STATE);
        }
        // enter thread looper
        Looper.loop();
    }

    void reset() {
        // 开始回调本地和远端视频的状态信息
        mLastLocalEncodeFrameCount = 0;
        mLastLocalVideoSendDataSize = 0;
        mLastLocalVideoRecvDataSize = 0;
        mLastLocalAudioSendDataSize = 0;
        mLastLocalAudioRecvDataSize = 0;
        mWorkerHandler.clearDatas();
    }

    /**
     * call this method to exit
     * should ONLY call this method when this thread is running
     */
    final void exit() {
        mHeadsetListener.unregisterReceiver();
        if (Thread.currentThread() != this) {
            Log.w(TAG, "exit() - exit app thread asynchronously");
            mWorkerHandler.sendEmptyMessage(ACTION_WORKER_THREAD_QUIT);
            return;
        }
        mReady = false;
        mContext = null;
        // TODO should remove all pending(read) messages
        mWorkerHandler.removeMessages(ACTION_WORKER_DATA_STATUS);
        mWorkerHandler.removeMessages(ACTION_WORKER_VIDEO_STATUS);
        Log.i(TAG, "exit() > start");
        // exit thread looper
        Looper looper = Looper.myLooper();
        if (looper != null) {
            looper.quit();
        }
        mWorkerHandler.release();
        Log.i(TAG, "exit() > end");
    }

    public static float formatedSpeedKS(long bytes, long elapsed_milli) {
        float bytes_per_sec = ((float) bytes) * 1000.f / elapsed_milli;
        return (bytes_per_sec / (float) 1024);
    }

    public static float formatedSpeedKbps(long bytes, long elapsed_milli) {
        float bytes_per_sec = ((float) bytes) * 1000.f / elapsed_milli;
        return (bytes_per_sec / (float) 1024) * 8;
    }

    void checkHeadsetListener() {
        mHeadsetListener.headsetAndBluetoothHeadsetHandle();
    }

    int joinChannel(String channelKey,
                    String channelName,
                    long optionalUid){
        mWorkerHandler.post(new Runnable() {
            @Override
            public void run() {
                mTTTRtcEngine.joinRealChannel(channelKey, channelName, optionalUid);
            }
        });
        return LocalSDKConstants.FUNCTION_SUCCESS;
    }

    int leaveChannel(){
        mWorkerHandler.post(new Runnable() {
            @Override
            public void run() {
                mTTTRtcEngine.leaveRealChannel();
            }
        });
        return LocalSDKConstants.FUNCTION_SUCCESS;
    }

    private static final class WorkerThreadHandler extends Handler {

        private WorkerThread mWorkerThread;
        private int mLocalFpsPS;
        private int mLocalVideoSendPS;
        private int mLocalAudioSendPS;
        private int mLocalVideoRecvPS;
        private int mLocalAudioRecvPS;
        private LongSparseArray<RemoteUserVideoWorkStats> mRemoteUserDataStats;
        private LongSparseArray<RemoteUserAudioWorkStats> mRemoteUserAudioDataStats;

        WorkerThreadHandler(WorkerThread thread) {
            this.mWorkerThread = thread;
            mRemoteUserDataStats = new LongSparseArray<>();
            mRemoteUserAudioDataStats = new LongSparseArray<>();
        }

        private void release() {
            mWorkerThread = null;
        }

        public void clearDatas() {
            mRemoteUserDataStats.clear();
            mRemoteUserAudioDataStats.clear();
        }

        @Override
        public void handleMessage(Message msg) {
            if (this.mWorkerThread == null) {
                Log.w(TAG, "handler is already released! " + msg.what);
                return;
            }

            switch (msg.what) {
                case ACTION_WORKER_THREAD_QUIT:
                    mRemoteUserDataStats.clear();
                    mRemoteUserAudioDataStats.clear();
                    mRemoteUserDataStats = null;
                    mRemoteUserAudioDataStats = null;
                    mWorkerThread.exit();
                    break;
                case ACTION_WORKER_DATA_STATUS:
                    if (GlobalConfig.mIsInRoom.get()) {
                        if (GlobalConfig.mIsEnableVideoMode) {
                            // 本地视频统计回调
                            ExternalVideoModule videoModule = ExternalVideoModule.getInstance();
                            if (mLastLocalEncodeFrameCount == 0) {
                                mLastLocalEncodeFrameCount = videoModule.getEncodeFrameCount();
                            }
                            if (mLastLocalVideoSendDataSize == 0) {
                                mLastLocalVideoSendDataSize = videoModule.getTotalSendBytes();
                            }
                            if (mLastLocalVideoRecvDataSize == 0) {
                                mLastLocalVideoRecvDataSize = videoModule.getTotalRecvBytes();
                            }

                            int encodeFrameCount = videoModule.getEncodeFrameCount();
                            mLocalFpsPS = (encodeFrameCount - mLastLocalEncodeFrameCount) * 1000 / SPEED_TIME;

                            int sendDataSize = videoModule.getTotalSendBytes();
                            int sendDatasValue = sendDataSize - mLastLocalVideoSendDataSize;
                            mLocalVideoSendPS = (int) formatedSpeedKbps(sendDatasValue, SPEED_TIME);

                            int videoTotalRecvBytes = videoModule.getTotalRecvBytes();
                            int videoRecvDataSize = videoTotalRecvBytes - mLastLocalVideoRecvDataSize;
                            mLocalVideoRecvPS = (int) formatedSpeedKbps(videoRecvDataSize, SPEED_TIME);

                            mLastLocalEncodeFrameCount = encodeFrameCount;
                            mLastLocalVideoSendDataSize = sendDataSize;
                            mLastLocalVideoRecvDataSize = videoTotalRecvBytes;

                            // 远端视频统计回调
                            Object o = mTTTRtcEngine.handleVideoModule(TTTModuleConstants.VIDEO_REMOTE_STUTS);
                            if (o != null) {
                                RemoteUserVideoWorkStats[] status = (RemoteUserVideoWorkStats[]) o;
                                for (RemoteUserVideoWorkStats tempStats : status) {
                                    if (tempStats != null) {
                                        mRemoteUserDataStats.put(tempStats.uid, tempStats);
                                    }
                                }
                            }
                        }

                        ExternalAudioModule audioModule = ExternalAudioModule.getInstance();
                        // 本地音频统计回调
                        if (mLastLocalAudioSendDataSize == 0) {
                            mLastLocalAudioSendDataSize = audioModule.getTotalSendBytes();
                        }
                        if (mLastLocalAudioRecvDataSize == 0) {
                            mLastLocalAudioRecvDataSize = audioModule.getTotalRecvBytes();
                        }
                        int audioTotalSendBytes = audioModule.getTotalSendBytes();
                        int audioSendDataSize = audioTotalSendBytes - mLastLocalAudioSendDataSize;
                        mLocalAudioSendPS = (int) formatedSpeedKbps(audioSendDataSize, SPEED_TIME);

                        int audioTotalRecvBytes = audioModule.getTotalRecvBytes();
                        int audioRecvDataSize = audioTotalRecvBytes - mLastLocalAudioRecvDataSize;
                        mLocalAudioRecvPS = (int) formatedSpeedKbps(audioRecvDataSize, SPEED_TIME);
                        mLastLocalAudioSendDataSize = audioTotalSendBytes;
                        mLastLocalAudioRecvDataSize = audioTotalRecvBytes;

                        // 远端音频统计回调
                        List<User> users = GlobalHolder.getInstance().getUsers();
                        for (int i = 0; i < users.size(); i++) {
                            User user = users.get(i);
                            long uid = user.getmUserId();
                            if (uid != GlobalConfig.mLocalUserID) {
                                int recvAudioDatas = ExternalAudioModule.getInstance().getRecvBytes(uid);
                                float mTempAudioRecvDatas = formatedSpeedKbps(recvAudioDatas - user.getLastReceiveAudioDatas(), SPEED_TIME);
                                user.setLastReceiveAudioDatas(recvAudioDatas);
                                RemoteUserAudioWorkStats tempAudioStats = new RemoteUserAudioWorkStats(uid, (int) mTempAudioRecvDatas);
                                mRemoteUserAudioDataStats.put(uid, tempAudioStats);
                            }
                        }
                    }
                    sendEmptyMessageDelayed(ACTION_WORKER_DATA_STATUS, SPEED_TIME);
                    break;
                case ACTION_WORKER_VIDEO_STATUS:
                    JniWorkerThread mJniWorkerThread = GlobalHolder.getInstance().getWorkerThread();
                    if (GlobalConfig.mIsInRoom.get()) {
                        LocalVideoStats stats = new LocalVideoStats(mLocalVideoSendPS, mLocalFpsPS);
                        mJniWorkerThread.sendMessage(JniWorkerThread.JNI_CALL_BACK_LOCAL_VIDEO_SATAUS, new Object[]{stats});

                        LocalAudioStats audioStats = new LocalAudioStats(mLocalAudioSendPS);
                        mJniWorkerThread.sendMessage(JniWorkerThread.JNI_CALL_BACK_LOCAL_AUDIO_SATAUS, new Object[]{audioStats});

                        mTTTRtcEngine.handleVideoModule(new TTTLocalModuleConfig(TTTModuleConstants.VIDEO_REMOTE_RTC_STUTS
                                , new Object[]{mRemoteUserDataStats}));

                        List<User> users = GlobalHolder.getInstance().getUsers();
                        for (int i = 0; i < users.size(); i++) {
                            User user = users.get(i);
                            long uid = user.getmUserId();
                            if (uid != GlobalConfig.mLocalUserID) {
                                RemoteUserAudioWorkStats remoteUserAudioWorkStats = mRemoteUserAudioDataStats.get(uid);
                                if (remoteUserAudioWorkStats != null) {
                                    RemoteAudioStats tempAudioStats = new RemoteAudioStats(uid, remoteUserAudioWorkStats.mBitrateRate);
                                    mJniWorkerThread.sendMessage(JniWorkerThread.JNI_CALL_BACK_REMOTE_AUDIO_SATAUS, new Object[]{tempAudioStats});
                                }
                            }
                        }
                    }

                    ExternalVideoModule videoModuleRtc = ExternalVideoModule.getInstance();
                    ExternalAudioModule audioModuleRtc = ExternalAudioModule.getInstance();
                    // 统计数据回调
                    int sendTotalVideoDataSize = videoModuleRtc.getTotalSendBytes();
                    int sendTotalAudioDataSize = audioModuleRtc.getTotalSendBytes();
                    int recvTotalVideoDataSize = videoModuleRtc.getTotalRecvBytes();
                    int recvTotalAudioDataSize = audioModuleRtc.getTotalRecvBytes();

                    RtcStats rtcStats = new RtcStats();
                    //通话时长
                    long mEndTime = System.currentTimeMillis();
                    long spendTime = mEndTime - GlobalConfig.mStartRoomTime;
                    rtcStats.setTotalDuration((int) (spendTime / 1000));
                    //发送字节数
                    rtcStats.setTxBytes(sendTotalVideoDataSize + sendTotalAudioDataSize);
                    //接收字节数
                    rtcStats.setRxBytes(recvTotalVideoDataSize + recvTotalAudioDataSize);
                    //发送音频码率
                    rtcStats.setTxAudioKBitRate(mLocalAudioSendPS);
                    //发送视频码率
                    rtcStats.setTxVideoKBitRate(mLocalVideoSendPS);
                    //接收音频码率
                    rtcStats.setRxAudioKBitRate(mLocalAudioRecvPS);
                    //接收视频码率
                    rtcStats.setRxVideoKBitRate(mLocalVideoRecvPS);
//                    //当前系统的CPU使用率（%）
//                    float totalCpuRate = DeviceUtils.getInstance().getTotalCpuRate();
//                    rtcStats.setCpuTotalUsage((int) totalCpuRate);
//                    //当前应用程序的CPU使用率（%）
//                    float processTotalCpuRate = DeviceUtils.getInstance().getCurProcessCpuRate();
//                    rtcStats.setCpuAppUsage((int) processTotalCpuRate);
                    mJniWorkerThread.sendMessage(JniWorkerThread.JNI_CALL_BACK_RTC_STATUS, new Object[]{rtcStats});
                    GlobalHolder.getInstance().setRtcStats(rtcStats);
                    sendEmptyMessageDelayed(ACTION_WORKER_VIDEO_STATUS, INTERVAL_TIME);
                    break;
            }
        }
    }

    static class RemoteUserVideoWorkStats {

        public long uid;
        public int mBitrateRate;
        public int mFrameRate;

        public RemoteUserVideoWorkStats(long uid, int mBitrateRate, int mFrameRate) {
            this.uid = uid;
            this.mBitrateRate = mBitrateRate;
            this.mFrameRate = mFrameRate;
        }
    }

    static class RemoteUserAudioWorkStats {

        public long uid;
        public int mBitrateRate;

        public RemoteUserAudioWorkStats(long uid, int mBitrateRate) {
            this.uid = uid;
            this.mBitrateRate = mBitrateRate;
        }
    }
}
