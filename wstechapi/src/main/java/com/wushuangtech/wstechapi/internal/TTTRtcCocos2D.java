package com.wushuangtech.wstechapi.internal;

import android.text.TextUtils;

import com.wushuangtech.bean.ChatInfo;
import com.wushuangtech.bean.RtcStats;
import com.wushuangtech.library.Constants;
import com.wushuangtech.utils.PviewLog;
import com.wushuangtech.wstechapi.TTTRtcEngineEventHandler;
import com.wushuangtech.wstechapi.TTTRtcEngineForGamming;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.wushuangtech.utils.PviewLog.CHAT_SEND;

/**
 * 这里面用到的Constants里面的常量，这里要改一下跟C++层保持一致
 */
public class TTTRtcCocos2D extends TTTRtcEngineEventHandler {

    private ConcurrentLinkedQueue<String> mReceiveMeesage = new ConcurrentLinkedQueue<>();
    private Lock mLock = new ReentrantLock();

    public TTTRtcCocos2D() {
        PviewLog.i("wzg TTTRtcCocos2D build... ");
        TTTRtcEngineForGamming.getInstance().setTTTRtcEngineForGammingEventHandler(this);
    }

    @Override
    public void onJoinChannelSuccess(String channel, long uid) {
        buildAndSaveMeesage("onJoinChannelSuccess", channel, uid, 0);
    }

    @Override
    public void onError(int err) {
        int mCode;
        switch (err) {
            case Constants.ERROR_ENTER_ROOM_INVALIDCHANNELNAME:
                mCode = 9000;
                break;
            case Constants.ERROR_ENTER_ROOM_TIMEOUT:
                mCode = 9001;
                break;
            case Constants.ERROR_ENTER_ROOM_FAILED:
                mCode = 9002;
                break;
            case Constants.ERROR_ENTER_ROOM_VERIFY_FAILED:
                mCode = 9003;
                break;
            case Constants.ERROR_ENTER_ROOM_BAD_VERSION:
                mCode = 9004;
                break;
            default:
                mCode = 9005;
                break;
        }
        buildAndSaveMeesage("onError", mCode);
    }

    @Override
    public void onConnectionLost() {
        buildAndSaveMeesage("onConnectionLost");
    }

    @Override
    public void onLeaveChannel(RtcStats stats) {
        buildAndSaveMeesage("onLeaveChannel",
                stats.getTotalDuration(), stats.getTxBytes(), stats.getRxBytes(), stats.getTxAudioKBitRate(),
                stats.getRxAudioKBitRate(), 0);
    }

    @Override
    public void onAudioVolumeIndication(long nUserID, int audioLevel, int audioLevelFullRange) {
        buildAndSaveMeesage("onReportAudioLevel", nUserID, audioLevel, audioLevelFullRange);
    }

    @Override
    public void onUserJoined(long nUserId, int identity) {
        buildAndSaveMeesage("onUserJoined", nUserId, 0);
    }

    @Override
    public void onUserOffline(long nUserId, int reason) {
        int mCode;
        switch (reason) {
            case Constants.USER_OFFLINE_QUIT:
                mCode = 0;
                break;
            case Constants.USER_OFFLINE_DROPPED:
                mCode = 1;
                break;
            case Constants.USER_OFFLINE_BECOMEAUDIENCE:
                mCode = 2;
                break;
            default:
                mCode = 0;
                break;
        }
        buildAndSaveMeesage("onUserOffline", nUserId, mCode);
    }

    @Override
    public void onAudioRouteChanged(int routing) {
        int mCode;
        switch (routing) {
            case Constants.AUDIO_ROUTE_HEADSET:
                mCode = 0;
                break;
            case Constants.AUDIO_ROUTE_SPEAKER:
                mCode = 1;
                break;
            case Constants.AUDIO_ROUTE_HEADPHONE:
                mCode = 2;
                break;
            default:
                mCode = 1;
                break;
        }
        buildAndSaveMeesage("onAudioRouteChanged", mCode);
    }

    @Override
    public void OnChatMessageSent(ChatInfo chatInfo, int error) {
        PviewLog.d(CHAT_SEND, "OnChatMessageSent -> chatInfo : " + chatInfo + " | error : " + error);
        buildAndSaveMeesage("OnChatMessageSent", error , chatInfo.chatType, chatInfo.seqID, chatInfo.chatData , chatInfo.audioDuration);
    }

    @Override
    public void OnSignalSent(String sSeqID, int error) {
        PviewLog.d(CHAT_SEND, "OnSignalSent -> sSeqID : " + sSeqID + " | error : " + error);
        buildAndSaveMeesage("OnSignalSent", sSeqID, error);
    }

    @Override
    public void OnChatMessageRecived(long nSrcUserID, ChatInfo chatInfo) {
        PviewLog.d(CHAT_SEND, "OnChatMessageRecived -> nSrcUserID : " + nSrcUserID + " | chatInfo : " + chatInfo);
        buildAndSaveMeesage("OnChatMessageRecived", nSrcUserID, chatInfo.chatType, chatInfo.seqID, chatInfo.chatData , chatInfo.audioDuration);
    }

    @Override
    public void OnSignalRecived(long nSrcUserID, String sSeqID, String strData) {
        PviewLog.d(CHAT_SEND, "OnSignalRecived -> nSrcUserID : " + nSrcUserID + " | sSeqID : " + sSeqID
                + " | strData : " + strData);
        buildAndSaveMeesage("OnChatMessageRecived", nSrcUserID, sSeqID, strData);
    }

    @Override
    public void onPlayChatAudioCompletion(String filePath) {
        PviewLog.d(CHAT_SEND, "onPlayChatAudioCompletion -> filePath : " + filePath);
        buildAndSaveMeesage("OnChatMessageRecived");
    }

    @Override
    public void onSpeechRecognized(String str) {
        PviewLog.d(CHAT_SEND, "onSpeechRecognized -> str : " + str);
        buildAndSaveMeesage("onSpeechRecognized", str);
    }

    private void buildAndSaveMeesage(Object... objs) {
        mLock.lock();
        try {
            StringBuilder sb = new StringBuilder();
            for (Object obj : objs) {
                sb.append(obj).append("\t");
            }
            String result = sb.toString();
            PviewLog.wf("wzg buildMeesage... message : " + result);
            mReceiveMeesage.add(result);
        } finally {
            mLock.unlock();
        }
    }

    private String getJavaMessage() {
        mLock.lock();
        try {
            String s = mReceiveMeesage.poll();
            PviewLog.wf("wzg java getMessage... message : " + s + " | message size : " +
                    mReceiveMeesage.size());
            if (!TextUtils.isEmpty(s)) {
                return s;
            }
            return null;
        } finally {
            mLock.unlock();
        }
    }
}
