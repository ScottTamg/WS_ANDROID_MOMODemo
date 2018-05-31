package com.wushuangtech.api;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.wushuangtech.bean.ChatInfo;
import com.wushuangtech.bean.ConfVideoFrame;
import com.wushuangtech.bean.LocalAudioStats;
import com.wushuangtech.bean.LocalVideoStats;
import com.wushuangtech.bean.RemoteAudioStats;
import com.wushuangtech.bean.RemoteVideoStats;
import com.wushuangtech.bean.RtcStats;
import com.wushuangtech.inter.TTTRtcEngineEventInter;
import com.wushuangtech.jni.ChatJni;
import com.wushuangtech.library.GlobalHolder;
import com.wushuangtech.utils.PviewLog;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wangzhiguo on 17/10/24.
 */


public class JniWorkerThread extends Thread {

    public static final int JNI_CALL_BACK_ENTER_ROOM = 5;
    public static final int JNI_CALL_BACK_ON_ERROR = 6;
    public static final int JNI_CALL_BACK_USER_JOIN = 7;
    public static final int JNI_CALL_BACK_USER_EXIT = 8;
    public static final int JNI_CALL_BACK_REMOTE_VIDEO_FIRST_FRAME = 9;
    public static final int JNI_CALL_BACK_LOCAL_VIDEO_FIRST_FRAME = 10;
    public static final int JNI_CALL_BACK_CONNECT_LOST = 11;
    public static final int JNI_CALL_BACK_RECONNECT_TIMEOUT = 42;
    public static final int JNI_CALL_BACK_LOCAL_VIDEO_SATAUS = 12;
    public static final int JNI_CALL_BACK_REMOTE_VIDEO_SATAUS = 13;
    public static final int JNI_CALL_BACK_CAMERA_READY = 14;
    public static final int JNI_CALL_BACK_USER_MUTE_VIDEO = 15;
    public static final int JNI_CALL_BACK_LEAVE_CHANNEL = 16;
    public static final int JNI_CALL_BACK_AUDIO_VOLUME_INDICATION = 17;
    public static final int JNI_CALL_BACK_RTC_STATUS = 18;
    public static final int JNI_CALL_BACK_AUDIO_ROUTE_CHANGE = 19;
    public static final int JNI_CALL_BACK_ON_SEI = 20;
    public static final int JNI_CALL_BACK_LOCAL_AUDIO_SATAUS = 21;
    public static final int JNI_CALL_BACK_REMOTE_AUDIO_SATAUS = 22;
    public static final int JNI_CALL_BACK_REMOTE_VIDEO_FIRST_DECODE = 23;
    public static final int JNI_CALL_BACK_REQUEST_CHANNEL_KEY = 24;
    public static final int JNI_CALL_BACK_VIDEO_STOPPED = 25;
    public static final int JNI_CALL_BACK_REMOTE_VIDEO_DECODE = 26;
    public static final int JNI_CALL_BACK_ON_CHAT_SEND = 27;
    public static final int JNI_CALL_BACK_ON_CHAT_RECV = 28;
    public static final int JNI_CALL_BACK_ON_CHAT_AUDIO_PLAY_COMPLETION = 29;
    public static final int JNI_CALL_BACK_ON_USER_ROLE_CHANGED = 30;
    public static final int JNI_CALL_BACK_ON_RECORD_TIME = 31;
    public static final int JNI_CALL_BACK_ON_AUDIO_MUTE = 32;
    public static final int JNI_CALL_BACK_ON_CHAT_AUDIO_RECOGNIZED = 33;
    public static final int JNI_CALL_BACK_ON_RECEIVE_SEI_DATA = 34;
    public static final int JNI_CALL_BACK_ON_RTMP_STATUS = 35;
    public static final int JNI_CALL_BACK_ON_ANCHOR_LINKED = 36;
    public static final int JNI_CALL_BACK_ON_ANCHOR_UNLINKED = 37;
    public static final int JNI_CALL_BACK_ON_ANCHOR_LINK_RESPONSE = 38;
    public static final int JNI_CALL_BACK_ON_ANCHOR_UNLINK_RESPONSE = 39;

    public static final int JNI_CALL_BACK_ENTER_PULL_ROOM = 40;
    public static final int JNI_CALL_BACK_ON_IJK_H264_SEI = 41;
    // 最大42

    private static final int ACTION_WORKER_THREAD_QUIT = 0X1010; // quit this thread

    private WorkerThreadHandler mWorkerHandler;

    private boolean mReady;

    private boolean mIsInRoom;

    private List<DelayMessageObj> mDelayMessage = new ArrayList<>();

    public final void waitForReady() {
        while (!mReady) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            PviewLog.i("wait for " + JniWorkerThread.class.getSimpleName());
        }
    }

    @Override
    public void run() {
        PviewLog.i("JniWorkerThread start to run");
        Looper.prepare();

        mWorkerHandler = new WorkerThreadHandler(this);

        mReady = true;

        // enter thread looper
        Looper.loop();
    }

    private static final class WorkerThreadHandler extends Handler {

        private JniWorkerThread mWorkerThread;

        WorkerThreadHandler(JniWorkerThread thread) {
            this.mWorkerThread = thread;
        }

        public void release() {
            mWorkerThread = null;
        }

        @Override
        public void handleMessage(Message msg) {
            if (this.mWorkerThread == null) {
                PviewLog.w("handler is already released! " + msg.what);
                return;
            }

            if (msg.what == JNI_CALL_BACK_USER_JOIN
                    || msg.what == JNI_CALL_BACK_USER_EXIT
                    || msg.what == JNI_CALL_BACK_REMOTE_VIDEO_FIRST_FRAME
                    || msg.what == JNI_CALL_BACK_USER_MUTE_VIDEO
                    || msg.what == JNI_CALL_BACK_ON_SEI
                    || msg.what == JNI_CALL_BACK_REMOTE_VIDEO_FIRST_DECODE
                    ) {
                if (!mWorkerThread.mIsInRoom) {
                    PviewLog.i(PviewLog.TAG, "添加缓存信息处理... what : " + msg.what);
                    mWorkerThread.mDelayMessage.add(new DelayMessageObj(msg.what, (Object[]) msg.obj));
                    PviewLog.i(PviewLog.TAG, "添加缓存信息处理-... size : " + mWorkerThread.mDelayMessage.size());
                } else {
                    handleDelayMessage(msg.what, (Object[]) msg.obj);
                }
            } else {
                TTTRtcEngineEventInter communicationHelper = GlobalHolder.getInstance().getCommunicationHelper();
                if (communicationHelper != null) {
                    Object[] objs = (Object[]) msg.obj;
                    switch (msg.what) {
                        case JNI_CALL_BACK_ENTER_PULL_ROOM:
                            communicationHelper.onJoinChannelSuccess((String) objs[0], -1);
                            break;
                        case JNI_CALL_BACK_ENTER_ROOM:
                            communicationHelper.onJoinChannelSuccess((String) objs[0], (long) objs[1]);
                            for (int i = 0; i < mWorkerThread.mDelayMessage.size(); i++) {
                                DelayMessageObj message = mWorkerThread.mDelayMessage.get(i);
                                PviewLog.i(PviewLog.TAG, "缓存信息处理... what : " + message.messageType);
                                handleDelayMessage(message.messageType, message.objs);
                            }
                            mWorkerThread.mDelayMessage.clear();
                            mWorkerThread.mIsInRoom = true;
                            break;
                        case JNI_CALL_BACK_ON_ERROR:
                            communicationHelper.onError((int) objs[0]);
                            break;
                        case JNI_CALL_BACK_CONNECT_LOST:
                            communicationHelper.onConnectionLost();
                            break;
                        case JNI_CALL_BACK_RTC_STATUS:
                            communicationHelper.onRtcStats((RtcStats) objs[0]);
                            break;
                        case JNI_CALL_BACK_LOCAL_VIDEO_SATAUS:
                            communicationHelper.onLocalVideoStats((LocalVideoStats) objs[0]);
                            break;
                        case JNI_CALL_BACK_AUDIO_ROUTE_CHANGE:
                            communicationHelper.onAudioRouteChanged((int) objs[0]);
                            break;
                        case JNI_CALL_BACK_LEAVE_CHANNEL:
                            mWorkerThread.mIsInRoom = false;
                            communicationHelper.onLeaveChannel((RtcStats) objs[0]);
                            break;
                        case JNI_CALL_BACK_REMOTE_VIDEO_SATAUS:
                            communicationHelper.onRemoteVideoStats((RemoteVideoStats) objs[0]);
                            break;
                        case JNI_CALL_BACK_AUDIO_VOLUME_INDICATION:
                            communicationHelper.onAudioVolumeIndication((long) objs[0], (int) objs[1], (int) objs[2]);
                            break;
                        case JNI_CALL_BACK_LOCAL_VIDEO_FIRST_FRAME:
                            communicationHelper.onFirstLocalVideoFrame((int) objs[0], (int) objs[1]);
                            break;
                        case JNI_CALL_BACK_CAMERA_READY:
                            communicationHelper.onCameraReady();
                            break;
                        case JNI_CALL_BACK_LOCAL_AUDIO_SATAUS:
                            communicationHelper.onLocalAudioStats((LocalAudioStats) objs[0]);
                            break;
                        case JNI_CALL_BACK_REMOTE_AUDIO_SATAUS:
                            communicationHelper.onRemoteAudioStats((RemoteAudioStats) objs[0]);
                            break;
                        case JNI_CALL_BACK_REQUEST_CHANNEL_KEY:
                            communicationHelper.onRequestChannelKey();
                            break;
                        case JNI_CALL_BACK_VIDEO_STOPPED:
                            communicationHelper.onVideoStopped();
                            break;
                        case JNI_CALL_BACK_REMOTE_VIDEO_DECODE:
                            communicationHelper.onRemoteVideoDecoded((long) objs[0], (ConfVideoFrame) objs[1]);
                            break;
                        case JNI_CALL_BACK_ON_CHAT_SEND:
                            if (((ChatInfo)objs[0]).chatType == ChatJni.CHATDATATYPE_SIGNAL) {
                                communicationHelper.OnSignalSent(((ChatInfo)objs[0]).seqID, (int) objs[1]);
                            } else {
                                communicationHelper.OnChatMessageSent((ChatInfo) objs[0], (int) objs[1]);
                            }
                            break;
                        case JNI_CALL_BACK_ON_CHAT_RECV:
                            if (((ChatInfo)objs[1]).chatType == ChatJni.CHATDATATYPE_SIGNAL) {
                                communicationHelper.OnSignalRecived((long) objs[0], ((ChatInfo)objs[1]).seqID, ((ChatInfo)objs[1]).chatData);
                            } else {
                                communicationHelper.OnChatMessageRecived((long) objs[0], (ChatInfo) objs[1]);
                            }
                            break;
                        case JNI_CALL_BACK_ON_CHAT_AUDIO_PLAY_COMPLETION:
                            communicationHelper.onPlayChatAudioCompletion((String) objs[0]);
                            break;
                        case JNI_CALL_BACK_ON_CHAT_AUDIO_RECOGNIZED:
                            communicationHelper.onSpeechRecognized((String)objs[0]);
                            break;
                        case JNI_CALL_BACK_ON_ANCHOR_LINKED:
                            communicationHelper.onAnchorEnter((long)objs[0], (long)objs[1], (String)objs[2], (int)objs[3]);
                            break;
                        case JNI_CALL_BACK_ON_ANCHOR_UNLINKED:
                            communicationHelper.onAnchorExit((long)objs[0], (long)objs[1]);
                            break;
                        case JNI_CALL_BACK_ON_ANCHOR_LINK_RESPONSE:
                            communicationHelper.onAnchorLinkResponse((long)objs[0], (long)objs[1]);
                            break;
                        case JNI_CALL_BACK_ON_ANCHOR_UNLINK_RESPONSE:
                            communicationHelper.onAnchorUnlinkResponse((long)objs[0], (long)objs[1]);
                            break;
                        case JNI_CALL_BACK_ON_USER_ROLE_CHANGED:
                            communicationHelper.onUserRoleChanged((long)objs[0],(int)objs[1]);
                            break;
                        case JNI_CALL_BACK_ON_RECORD_TIME:
                            communicationHelper.onScreenRecordTime((int)objs[0]);
                            break;
                        case JNI_CALL_BACK_ON_AUDIO_MUTE:
                            communicationHelper.onUserMuteAudio((long)objs[0] , (boolean)objs[1]);
                            break;
                        case JNI_CALL_BACK_ON_RECEIVE_SEI_DATA:
                            communicationHelper.reportH264SeiContent((String)objs[0] , (long)objs[1]);
                            break;
                        case JNI_CALL_BACK_ON_RTMP_STATUS:
                            communicationHelper.onStatusOfRtmpPublish((int)objs[0]);
                            break;
                        case JNI_CALL_BACK_ON_IJK_H264_SEI:
                            communicationHelper.onSetSEI((String) objs[0]);
                            break;
                        case JNI_CALL_BACK_RECONNECT_TIMEOUT:
                            communicationHelper.onConnectionLost();
                            break;
                    }
                }
            }

        }

        private void handleDelayMessage(int msgType, Object[] objs) {
            TTTRtcEngineEventInter communicationHelper = GlobalHolder.getInstance().getCommunicationHelper();
            if (communicationHelper != null) {
                switch (msgType) {
                    case JNI_CALL_BACK_USER_JOIN:
                        communicationHelper.onUserJoined((long) objs[0], (int) objs[1]);
                        break;
                    case JNI_CALL_BACK_USER_EXIT:
                        communicationHelper.onUserOffline((long) objs[0], (int) objs[1]);
                        break;
                    case JNI_CALL_BACK_REMOTE_VIDEO_FIRST_FRAME:
                        communicationHelper.onFirstRemoteVideoFrame((long) objs[0], (int) objs[1], (int) objs[2]);
                        break;
                    case JNI_CALL_BACK_REMOTE_VIDEO_FIRST_DECODE:
                        communicationHelper.onFirstRemoteVideoDecoded((long) objs[0], (int) objs[1], (int) objs[2]);
                        break;
                    case JNI_CALL_BACK_USER_MUTE_VIDEO:
                        communicationHelper.onUserEnableVideo((long) objs[0], (boolean) objs[1]);
                        break;
                    case JNI_CALL_BACK_ON_SEI:
                        communicationHelper.onSetSEI((String) objs[0]);
                        break;
                }
            }
        }
    }

    public WorkerThreadHandler getWorkerHandler() {
        return mWorkerHandler;
    }

    public void sendMessage(int what, Object[] objs) {
        Message msg = new Message();
        msg.what = what;
        msg.obj = objs;
        msg.arg1 = what;
        msg.setTarget(mWorkerHandler);
        msg.sendToTarget();
    }

    public void sendDelayMessage(int what, Object[] objs) {
        Message msg = new Message();
        msg.what = what;
        msg.obj = objs;
        msg.arg1 = what;
        mWorkerHandler.sendMessageDelayed(msg , 100);
    }

    public void clearDelayMessages() {
        mDelayMessage.clear();
    }

    /**
     * call this method to exit
     * should ONLY call this method when this thread is running
     */
    public final void exit() {
        if (Thread.currentThread() != this) {
            PviewLog.w("exit() - exit app thread asynchronously");
            mWorkerHandler.sendEmptyMessage(ACTION_WORKER_THREAD_QUIT);
            return;
        }

        mReady = false;

        // TODO should remove all pending(read) messages

        PviewLog.i("exit() > start");

        // exit thread looper
        Looper.myLooper().quit();

        mWorkerHandler.release();

        PviewLog.i("exit() > end");
    }

    static class DelayMessageObj {
        public int messageType;
        public Object[] objs;

        public DelayMessageObj(int messageType, Object[] objs) {
            this.messageType = messageType;
            this.objs = objs;
        }
    }
}
