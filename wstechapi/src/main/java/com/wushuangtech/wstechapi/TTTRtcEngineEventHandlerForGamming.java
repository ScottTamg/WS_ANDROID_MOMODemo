package com.wushuangtech.wstechapi;

import com.wushuangtech.bean.RtcStats;

/**
 * 游戏SDK的回调接收基类，用户可自定义类继承此类，实现SDK回调的接收
 */
public abstract class TTTRtcEngineEventHandlerForGamming extends TTTRtcEngineEventHandler {

    @Override
    public void onJoinChannelSuccess(String channel, long uid) {
    }

    @Override
    public void onError(int err) {
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
    public void onUserJoined(long nUserId, int identity) {
    }

    @Override
    public void onUserOffline(long nUserId, int reason) {
    }

    @Override
    public void onConnectionLost() {
    }
}
