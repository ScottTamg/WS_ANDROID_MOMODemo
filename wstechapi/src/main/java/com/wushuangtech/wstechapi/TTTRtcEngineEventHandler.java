package com.wushuangtech.wstechapi;

import com.wushuangtech.bean.ChatInfo;
import com.wushuangtech.bean.ConfVideoFrame;
import com.wushuangtech.bean.LocalAudioStats;
import com.wushuangtech.bean.LocalVideoStats;
import com.wushuangtech.bean.RemoteAudioStats;
import com.wushuangtech.bean.RemoteVideoStats;
import com.wushuangtech.bean.RtcStats;
import com.wushuangtech.inter.TTTRtcEngineEventInter;

/**
 * 主体SDK的回调接收基类，用户可自定义类继承此类，实现SDK回调的接收
 */
public abstract class TTTRtcEngineEventHandler implements TTTRtcEngineEventInter{
    @Override
    public void onError(int err) {

    }

    @Override
    public void onConnectionLost() {

    }

    @Override
    public void onJoinChannelSuccess(String channel, long uid) {

    }

    @Override
    public void onUserJoined(long nUserId, int identity) {

    }

    @Override
    public void onFirstLocalVideoFrame(int width, int height) {

    }

    @Override
    public void onFirstRemoteVideoFrame(long uid, int width, int height) {

    }

    @Override
    public void onFirstRemoteVideoDecoded(long uid, int width, int height) {

    }

    @Override
    public void onRemoteVideoDecoded(long uid, ConfVideoFrame mFrame) {

    }

    @Override
    public void onUserOffline(long nUserId, int reason) {

    }

    @Override
    public void onLocalVideoStats(LocalVideoStats stats) {

    }

    @Override
    public void onRemoteVideoStats(RemoteVideoStats stats) {

    }

    @Override
    public void onLocalAudioStats(LocalAudioStats stats) {

    }

    @Override
    public void onRemoteAudioStats(RemoteAudioStats stats) {

    }

    @Override
    public void onCameraReady() {

    }

    @Override
    public void onLeaveChannel(RtcStats stats) {

    }

    @Override
    public void onAudioVolumeIndication(long nUserID, int audioLevel, int audioLevelFullRange) {

    }

    @Override
    public void onRtcStats(RtcStats stats) {

    }

    @Override
    public void onAudioRouteChanged(int routing) {

    }

    @Override
    public void onSetSEI(String sei) {

    }

    @Override
    public void onVideoStopped() {

    }

    @Override
    public void OnChatMessageSent(ChatInfo chatInfo, int error) {

    }

    @Override
    public void OnSignalSent(String sSeqID, int error) {

    }

    @Override
    public void OnChatMessageRecived(long nSrcUserID, ChatInfo chatInfo) {

    }

    @Override
    public void OnSignalRecived(long nSrcUserID, String sSeqID, String strData) {

    }

    @Override
    public void onPlayChatAudioCompletion(String filePath) {

    }

    @Override
    public void onUserEnableVideo(long uid, boolean muted) {

    }

    @Override
    public void onRequestChannelKey() {

    }

    @Override
    public void onUserRoleChanged(long userID, int userRole) {

    }

    @Override
    public void onScreenRecordTime(int mRecordTime) {

    }

    @Override
    public void onUserMuteAudio(long uid, boolean muted) {

    }

    @Override
    public void onSpeechRecognized(String str) {

    }

    @Override
    public void onAnchorEnter(long sessionId, long userId, String devID, int error) {

    }

    @Override
    public void onAnchorExit(long sessionId, long userId) {

    }

    @Override
    public void onAnchorLinkResponse(long sessionId, long userId) {

    }

    @Override
    public void onAnchorUnlinkResponse(long sessionId, long userId) {

    }

    @Override
    public void reportH264SeiContent(String sei_content, long uid) {

    }

    @Override
    public void onStatusOfRtmpPublish(int errorType) {

    }
}
