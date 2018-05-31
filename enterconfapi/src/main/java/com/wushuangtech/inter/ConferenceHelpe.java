package com.wushuangtech.inter;

public interface ConferenceHelpe {

    void onMemberEnter(long nConfId, long nUserId, String deviceInfo, int userRole, int speakStatus);

    void onMemberQuit(long nConfId, long nUserId , int reason);

    void onKickConference(long nConfId, long nSrcUserId, long nDstUserId, int reason , String uuid);

    void onConfDisconnected(String uuid);

    void onSetSei(long nUserId, String sei);

    void onUpdateDevParam(String devParam);

    void onUpdateRtmpStatus(long nConfId, String rtmpUrl, boolean status);

    void onAnchorEnter(long nConfId, long nUserId, String devID, int error);

    void onAnchorExit(long nConfId, long nUserId);

    void onAnchorLinkResponse(long nConfId, long nUserId);

    void onAnchorUnlinkResponse(long nConfId, long nUserId);

    void onApplyPermission(long nUserId, int type);

    void onGrantPermission(long nUserId, int type, int status);

    void onUpdateReportLogConfig(boolean reportData, boolean reportEvent, int reportInterval);

    void onConfChairChanged(long nConfId, long nUserId);

    void onRecvCmdMsg(long nConfId, long nUserId, String msg);

    void onReportMediaAddr(String aIp, String vIp);
    void onMediaReconnect(int type);
    void onAudioLevelReport(long nUserId, int audioLevel, int audioLevelFullRange);
    void onUpdateVideoDev(long nUserId, String xml);
    void onRecvAudioMsg(String msg);
    void onRecvVideoMsg(String msg);

    void onStartSendVideo(boolean bMute, boolean bOpen);
    void onStopSendVideo(int reason);
    void onStartSendAudio();
    void onStopSendAudio();

    void onUpdateAudioStatus(long userID, boolean speak, boolean server_mix);

    void OnRemoteAudioMuted(long userID, boolean muted);
}
