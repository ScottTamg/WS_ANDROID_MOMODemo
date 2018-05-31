package com.wushuangtech.library;

import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import com.wushuangtech.api.EnterConfApi;
import com.wushuangtech.api.JniWorkerThread;
import com.wushuangtech.bean.ChatInfo;
import com.wushuangtech.bean.RemoteVideoMute;
import com.wushuangtech.bean.UserDeviceInfos;
import com.wushuangtech.inter.ConferenceHelpe;
import com.wushuangtech.inter.UserExitNotifyCallBack;
import com.wushuangtech.jni.ChatJni;
import com.wushuangtech.jni.ReportLogJni;
import com.wushuangtech.jni.RoomJni;
import com.wushuangtech.jni.VideoJni;
import com.wushuangtech.utils.PviewLog;
import com.wushuangtech.utils.XMLParser;

import static com.wushuangtech.library.Constants.CLIENT_ROLE_BROADCASTER;
import static com.wushuangtech.library.Constants.CLIENT_ROLE_AUDIENCE;
import static com.wushuangtech.library.Constants.CLIENT_ROLE_ANCHOR;
import static com.wushuangtech.library.InstantRequest.REQUEST_JOIN_ROOM;


public class PviewConferenceRequest extends PviewAbstractHandler {

    private static final int JNI_REQUEST_ENTER_CONF = 1;
    private static final java.lang.String TAG = PviewConferenceRequest.class.getSimpleName();

    private ConferenceHelpe help;
    private Handler mCallbackHandler;
    // 这个回调用于，当某个用户退出，若显示了该用户的视频，需要销毁该用户对应的解码器
    private UserExitNotifyCallBack mUserExitNotifyCallBack;
//    private boolean mFirstAudioSent = false;
//    private boolean mFirstVideoSent = false;

    public PviewConferenceRequest(ConferenceHelpe confhelp, HandlerThread handlerThread) {
        this(handlerThread);
        help = confhelp;
    }

    private PviewConferenceRequest(HandlerThread handlerThread) {
        super(handlerThread.getLooper());
        mCallbackHandler = this;
        VideoJni.getInstance().addCallback(this);
        RoomJni.getInstance().addCallback(this);
        ReportLogJni.getInstance().addCallback(this);
        ChatJni.getInstance().addCallback(this);
    }

    public void requestEnterRoom(String appId, long confId, long userId, int userRole, String rtmpUrl, boolean recordMp4, HandlerWrap caller) {
        initTimeoutMessage(JNI_REQUEST_ENTER_CONF, DEFAULT_TIME_OUT_SECS, caller);
        RoomJni.getInstance().RoomQuickEnter(appId, userId,
                confId, userRole, rtmpUrl, Build.MODEL, recordMp4);
    }

    public int requestEnterRoom(String appId, long confId, long userId, int userRole, String rtmpUrl, boolean recordMp4, Handler caller) {
//        mFirstAudioSent = false;
//        mFirstVideoSent = false;
        return InstantRequest.getInstance().requestServer(caller, REQUEST_JOIN_ROOM, appId, userId,
                confId, userRole, rtmpUrl, recordMp4);
    }

    public void requestEnterConference(String appId, long confId, long userId, boolean mixVideo, String rtmpUrl, boolean recordMp4, String password, HandlerWrap caller) {
        initTimeoutMessage(JNI_REQUEST_ENTER_CONF, DEFAULT_TIME_OUT_SECS, caller);
        RoomJni.getInstance().RoomNormalEnter(appId, userId,
                confId, password, mixVideo, rtmpUrl, Build.MODEL, recordMp4);
    }

    @Override
    public void clearCalledBack() {
        VideoJni.getInstance().removeCallback(this);
        RoomJni.getInstance().removeCallback(this);
        ReportLogJni.getInstance().removeCallback(this);
        ChatJni.getInstance().removeCallback(this);
        mCallbackHandler.removeCallbacksAndMessages(null);
    }

    public void OnUpdateVideoDev(long uid, String szXmlData) {
        help.onUpdateVideoDev(uid, szXmlData);
        UserDeviceInfos remoteUserDeviceInfos = XMLParser.getRemoteUserDeviceInfos(uid, szXmlData);
        if (remoteUserDeviceInfos != null) {
            RemoteVideoMute remoteVideoMute = new RemoteVideoMute();
            remoteVideoMute.setIsMuted(!remoteUserDeviceInfos.mInUse);
            remoteVideoMute.setUserID(remoteUserDeviceInfos.mUserID);

            if (remoteUserDeviceInfos.mInUse) {
                EnterConfApi.getInstance().openDeviceVideo(uid, remoteUserDeviceInfos.mUserDeviceID);
            }

            PviewLog.d("OnUpdateVideoDev -> update device, id : " + remoteUserDeviceInfos.mUserID + " | devID : " + remoteUserDeviceInfos.mUserDeviceID
                    + " | inuse : " + remoteUserDeviceInfos.mInUse);
            UserDeviceConfig udc = new UserDeviceConfig(remoteUserDeviceInfos.mUserID,
                    remoteUserDeviceInfos.mUserDeviceID, remoteUserDeviceInfos.mInUse);
            GlobalHolder.getInstance().updateUserDevice(remoteUserDeviceInfos.mUserID, udc);
            JniWorkerThread mJniWorkerThread = GlobalHolder.getInstance().getWorkerThread();
            mJniWorkerThread.sendMessage(JniWorkerThread.JNI_CALL_BACK_USER_MUTE_VIDEO, new Object[]{remoteVideoMute.getUserID(), remoteVideoMute.isIsMuted()});
        } else {
            PviewLog.e(TAG, "OnUpdateVideoDev parse xml get null , xml : " + "szXmlData");
        }
    }

    public void OnEnterConfCallback(long nConfID, int nJoinResult, int userRole) {
        JNIResponse jniRes = new JNIResponse(JNIResponse.Result.fromInt(nJoinResult));
        jniRes.resObj = new Conference(nConfID, userRole);
        Message.obtain(mCallbackHandler, JNI_REQUEST_ENTER_CONF, jniRes).sendToTarget();
    }

    public void OnConfDisconnected(String uuid) {
        help.onConfDisconnected(uuid);
    }

    public void OnConfMemberEnter(long nConfID, long nUserID, String szDeviceInfos,
                                  int userRole, int speakStatus) {

        UserDeviceInfos remoteUserDeviceInfos = XMLParser.getRemoteUserDeviceInfos(nUserID, szDeviceInfos);
        UserDeviceConfig udc;
        if (remoteUserDeviceInfos == null) {
            udc = new UserDeviceConfig(nUserID, "", false);
            PviewLog.e("OnConfMemberEnter parse user device infos failed! szDeviceInfos : " + szDeviceInfos);
        } else {
            PviewLog.d("OnConfMemberEnter -> update device, id : " + nUserID + " | devID : " + remoteUserDeviceInfos.mUserDeviceID
                    + " | inuse : " + remoteUserDeviceInfos.mInUse);
            udc = new UserDeviceConfig(nUserID, remoteUserDeviceInfos.mUserDeviceID, remoteUserDeviceInfos.mInUse);
        }
        GlobalHolder.getInstance().putOrUpdateUser(nUserID);
        GlobalHolder.getInstance().updateUserDevice(nUserID, udc);
        help.onMemberEnter(nConfID, nUserID, udc.getDeviceID(), userRole, speakStatus);
        JniWorkerThread mJniWorkerThread = GlobalHolder.getInstance().getWorkerThread();
        if (userRole == CLIENT_ROLE_BROADCASTER) {
            if (remoteUserDeviceInfos != null && remoteUserDeviceInfos.mInUse) {
                EnterConfApi.getInstance().openDeviceVideo(nUserID, remoteUserDeviceInfos.mUserDeviceID);
            }
            GlobalHolder.getInstance().getUser(nUserID).setUserIdentity(Constants.CLIENT_ROLE_BROADCASTER);
            mJniWorkerThread.sendMessage(JniWorkerThread.JNI_CALL_BACK_USER_JOIN, new Object[]{nUserID, CLIENT_ROLE_BROADCASTER});
        } else if (userRole == CLIENT_ROLE_AUDIENCE) {
            GlobalHolder.getInstance().getUser(nUserID).setUserIdentity(Constants.CLIENT_ROLE_AUDIENCE);
            mJniWorkerThread.sendMessage(JniWorkerThread.JNI_CALL_BACK_USER_JOIN, new Object[]{nUserID, CLIENT_ROLE_AUDIENCE});
        } else if (userRole == CLIENT_ROLE_ANCHOR) {
            GlobalHolder.getInstance().getUser(nUserID).setUserIdentity(Constants.CLIENT_ROLE_ANCHOR);
            mJniWorkerThread.sendMessage(JniWorkerThread.JNI_CALL_BACK_USER_JOIN, new Object[]{nUserID, CLIENT_ROLE_ANCHOR});
            if (remoteUserDeviceInfos != null && remoteUserDeviceInfos.mInUse) {
                EnterConfApi.getInstance().openDeviceVideo(nUserID, remoteUserDeviceInfos.mUserDeviceID);
            }
        }
    }

    public void OnConfMemberExitCallback(long nConfID, long nUserID, int reason) {
        help.onMemberQuit(nConfID, nUserID , reason);

        GlobalHolder instance = GlobalHolder.getInstance();
        User user = instance.getUser(nUserID);
        if (user != null) {
            UserDeviceConfig userDefaultDevice = instance.getUserDefaultDevice(nUserID);
            if (userDefaultDevice != null) {
                GlobalHolder.getInstance().closeHardwareDecoder(userDefaultDevice.getDeviceID());
                GlobalHolder.getInstance().removeRemoteFirstDecoders(userDefaultDevice.getDeviceID());
                // 用户退出，销毁该用户的解码器
                if (mUserExitNotifyCallBack != null) {
                    mUserExitNotifyCallBack.userExitRoom(userDefaultDevice.getDeviceID());
                }
            }
        }

        instance.delUser(nUserID);

        JniWorkerThread mJniWorkerThread = GlobalHolder.getInstance().getWorkerThread();
        mJniWorkerThread.sendMessage(JniWorkerThread.JNI_CALL_BACK_USER_EXIT, new Object[]{nUserID, Constants.USER_OFFLINE_QUIT});
    }

    public void OnKickConfCallback(long nConfId, long nSrcUserId, long nDstUserId, int nReason, String uuid) {
        help.onKickConference(nConfId, nSrcUserId, nDstUserId, nReason, uuid);
        JniWorkerThread mJniWorkerThread = GlobalHolder.getInstance().getWorkerThread();
        mJniWorkerThread.sendMessage(JniWorkerThread.JNI_CALL_BACK_ON_ERROR, new Object[]{200 + nReason});
    }

    public void OnConfPermissionApply(long userid, int type) {
        help.onApplyPermission(userid, type);
    }

    public void OnGrantPermissionCallback(long userid, int type, int status) {
        help.onGrantPermission(userid, type, status);
        if (type == RoomJni.PERMISSIONTYPE_SPEAK) {
            if (userid == GlobalConfig.mLocalUserID) {
                if (status == RoomJni.PERMISSIONSTATUS_GRANTED) {
                    if (GlobalConfig.mCurrentChannelMode == Constants.CHANNEL_PROFILE_LIVE_BROADCASTING) {
                        if (!GlobalConfig.mIsMuteLocalAudio) {
                            EnterConfApi.getInstance().muteLocalAudio(false);
                        }
                        // IOS当主播，需要手动返回自己静音的回调
                        JniWorkerThread mJniWorkerThread = GlobalHolder.getInstance().getWorkerThread();
                        mJniWorkerThread.sendMessage(JniWorkerThread.JNI_CALL_BACK_ON_AUDIO_MUTE, new Object[]{userid, false});
                    }
                } else if (status == RoomJni.PERMISSIONSTATUS_NORMAL) {
                    if (GlobalConfig.mCurrentChannelMode == Constants.CHANNEL_PROFILE_LIVE_BROADCASTING) {
                        EnterConfApi.getInstance().muteLocalAudio(true);
                        JniWorkerThread mJniWorkerThread = GlobalHolder.getInstance().getWorkerThread();
                        mJniWorkerThread.sendMessage(JniWorkerThread.JNI_CALL_BACK_ON_AUDIO_MUTE, new Object[]{userid, true});
                    }
                }
            } else {
                if (status == RoomJni.PERMISSIONSTATUS_GRANTED) {
                    JniWorkerThread mJniWorkerThread = GlobalHolder.getInstance().getWorkerThread();
                    mJniWorkerThread.sendMessage(JniWorkerThread.JNI_CALL_BACK_ON_AUDIO_MUTE, new Object[]{userid, false});
                } else {
                    JniWorkerThread mJniWorkerThread = GlobalHolder.getInstance().getWorkerThread();
                    mJniWorkerThread.sendMessage(JniWorkerThread.JNI_CALL_BACK_ON_AUDIO_MUTE, new Object[]{userid, true});
                }
            }
        }
    }

    public void OnConfChairChanged(long nConfID, long nChairID) {
        help.onConfChairChanged(nConfID, nChairID);
    }

    public void OnSetSei(long operUserId, String sei) {
        JniWorkerThread mJniWorkerThread = GlobalHolder.getInstance().getWorkerThread();
        mJniWorkerThread.sendMessage(JniWorkerThread.JNI_CALL_BACK_ON_SEI, new Object[]{sei});
    }

    public void OnChatSend(ChatInfo chatInfo, int error) {
        JniWorkerThread mJniWorkerThread = GlobalHolder.getInstance().getWorkerThread();
        mJniWorkerThread.sendMessage(JniWorkerThread.JNI_CALL_BACK_ON_CHAT_SEND, new Object[]{chatInfo, error});
    }

    public void OnChatRecv(long nSrcUserID, ChatInfo chatInfo) {
        JniWorkerThread mJniWorkerThread = GlobalHolder.getInstance().getWorkerThread();
        mJniWorkerThread.sendMessage(JniWorkerThread.JNI_CALL_BACK_ON_CHAT_RECV, new Object[]{nSrcUserID, chatInfo});
    }

    public void onPlayChatAudioCompletion(String filePath) {
        JniWorkerThread mJniWorkerThread = GlobalHolder.getInstance().getWorkerThread();
        mJniWorkerThread.sendMessage(JniWorkerThread.JNI_CALL_BACK_ON_CHAT_AUDIO_PLAY_COMPLETION, new Object[]{filePath});
    }

    public void onSpeechRecognized(String str) {
//        help.onSetSei(operUserId, sei);
        JniWorkerThread mJniWorkerThread = GlobalHolder.getInstance().getWorkerThread();
        mJniWorkerThread.sendMessage(JniWorkerThread.JNI_CALL_BACK_ON_CHAT_AUDIO_RECOGNIZED, new Object[]{str});
    }

    public void OnUpdateDevParam(String devParam) {
        help.onUpdateDevParam(devParam);
    }

    public void OnUpdateRtmpStatus(long groupId, String rtmpUrl, boolean status) {
        help.onUpdateRtmpStatus(groupId, rtmpUrl, status);
    }

    public void OnAnchorLinked(long nGroupID, long nUserID, String devID, int error) {
        help.onAnchorEnter(nGroupID, nUserID, devID, error);
        if (error == 0) {
            UserDeviceConfig udc = new UserDeviceConfig(nUserID, devID, true);
            GlobalHolder.getInstance().putOrUpdateUser(nUserID);
            GlobalHolder.getInstance().updateUserDevice(nUserID, udc);
            EnterConfApi.getInstance().openDeviceVideo(nUserID, devID);
        }
        JniWorkerThread mJniWorkerThread = GlobalHolder.getInstance().getWorkerThread();
        mJniWorkerThread.sendMessage(JniWorkerThread.JNI_CALL_BACK_ON_ANCHOR_LINKED, new Object[]{nGroupID, nUserID, devID, error});
    }

    public void OnAnchorUnlinked(long nGroupID, long nUserID) {
        help.onAnchorExit(nGroupID, nUserID);
        JniWorkerThread mJniWorkerThread = GlobalHolder.getInstance().getWorkerThread();
        mJniWorkerThread.sendMessage(JniWorkerThread.JNI_CALL_BACK_ON_ANCHOR_UNLINKED, new Object[]{nGroupID, nUserID});

        GlobalHolder instance = GlobalHolder.getInstance();
        User user = instance.getUser(nUserID);
        if (user != null) {
            UserDeviceConfig userDefaultDevice = instance.getUserDefaultDevice(nUserID);
            if (userDefaultDevice != null) {
                GlobalHolder.getInstance().closeHardwareDecoder(userDefaultDevice.getDeviceID());
                GlobalHolder.getInstance().removeRemoteFirstDecoders(userDefaultDevice.getDeviceID());
                // 用户退出，销毁该用户的解码器
                if (mUserExitNotifyCallBack != null) {
                    mUserExitNotifyCallBack.userExitRoom(userDefaultDevice.getDeviceID());
                }
            }
        }
        instance.delUser(nUserID);
    }

    public void OnAnchorLinkResponse(long nGroupID, long nUserID) {
        help.onAnchorLinkResponse(nGroupID, nUserID);
        JniWorkerThread mJniWorkerThread = GlobalHolder.getInstance().getWorkerThread();
        mJniWorkerThread.sendMessage(JniWorkerThread.JNI_CALL_BACK_ON_ANCHOR_LINK_RESPONSE, new Object[]{nGroupID, nUserID});
    }

    public void OnAnchorUnlinkResponse(long nGroupID, long nUserID) {
        help.onAnchorUnlinkResponse(nGroupID, nUserID);
        JniWorkerThread mJniWorkerThread = GlobalHolder.getInstance().getWorkerThread();
        mJniWorkerThread.sendMessage(JniWorkerThread.JNI_CALL_BACK_ON_ANCHOR_UNLINK_RESPONSE, new Object[]{nGroupID, nUserID});
    }

    public void OnUpdateReportLogConfig(boolean reportData, boolean reportEvent, int reportInterval) {
        help.onUpdateReportLogConfig(reportData, reportEvent, reportInterval);
    }

    public void OnRecvCmdMsg(long nGroupID, long nUserID, String msg) {
        help.onRecvCmdMsg(nGroupID, nUserID, msg);
    }

    public void OnReportMediaAddr(String aIp, String vIp) {
        help.onReportMediaAddr(aIp, vIp);
    }

    public void OnMediaReconnect(int type) {
        help.onMediaReconnect(type);
    }

    public void OnAudioLevelReport(long nUserID, int audioLevel, int audioLevelFullRange) {
        help.onAudioLevelReport(nUserID, audioLevel, audioLevelFullRange);
        JniWorkerThread mJniWorkerThread = GlobalHolder.getInstance().getWorkerThread();
        mJniWorkerThread.sendMessage(JniWorkerThread.JNI_CALL_BACK_AUDIO_VOLUME_INDICATION, new Object[]{nUserID, audioLevel, audioLevelFullRange});
    }

    public void OnRecvAudioMsg(String msg) {
        help.onRecvAudioMsg(msg);
    }

    public void OnRecvVideoMsg(String msg) {
        help.onRecvVideoMsg(msg);
    }

    public void OnStartSendVideo(boolean bMute, boolean bOpen) {
        help.onStartSendVideo(bMute, bOpen);
    }

    public void OnStopSendVideo(int reason) {
        help.onStopSendVideo(reason);
    }

    public void OnStartSendAudio() {
        help.onStartSendAudio();
    }

    public void OnStopSendAudio() {
        help.onStopSendAudio();
    }

    public void OnUpdateAudioStatus(long userID, boolean speak, boolean server_mix) {
        help.onUpdateAudioStatus(userID, speak, server_mix);
    }

    public void OnRemoteAudioMuted(long userID, boolean muted) {
        help.OnRemoteAudioMuted(userID, muted);
        JniWorkerThread mJniWorkerThread = GlobalHolder.getInstance().getWorkerThread();
        mJniWorkerThread.sendMessage(JniWorkerThread.JNI_CALL_BACK_ON_AUDIO_MUTE, new Object[]{userID, muted});
    }

    public void OnUserRoleChanged(long userID, int userRole) {
        User user = GlobalHolder.getInstance().getUser(userID);
        if (user != null) {
            user.setUserIdentity(userRole);
        }

        JniWorkerThread mJniWorkerThread = GlobalHolder.getInstance().getWorkerThread();
        mJniWorkerThread.sendMessage(JniWorkerThread.JNI_CALL_BACK_ON_USER_ROLE_CHANGED, new Object[]{userID, userRole});
    }

    public void OnFirstAudioSent() {
//        if (!mFirstAudioSent && !mFirstVideoSent) {
//            JniWorkerThread mJniWorkerThread = GlobalHolder.getInstance().getWorkerThread();
//            mJniWorkerThread.sendMessage(JniWorkerThread.JNI_CALL_BACK_ON_USER_ROLE_CHANGED, new Object[]{userID, userRole});
//        }
//        mFirstAudioSent = true;
    }

    public void OnFirstVideoSent() {
//        if (!mFirstAudioSent && !mFirstVideoSent) {
//            JniWorkerThread mJniWorkerThread = GlobalHolder.getInstance().getWorkerThread();
//            mJniWorkerThread.sendMessage(JniWorkerThread.JNI_CALL_BACK_ON_USER_ROLE_CHANGED, new Object[]{userID, userRole});
//        }
//        mFirstVideoSent = true;
    }

    public void OnReconnectTimeout() {
        JniWorkerThread mJniWorkerThread = GlobalHolder.getInstance().getWorkerThread();
        mJniWorkerThread.sendMessage(JniWorkerThread.JNI_CALL_BACK_RECONNECT_TIMEOUT, new Object[]{});
    }

    public void OnRtpRtcp(boolean bRtpRtcp) {
        PviewLog.testPrint("OnRtpRtcp", bRtpRtcp);
    }

    public void OnConnect(String uuid, String ip, int port) {
        PviewLog.testPrint("OnConnect", uuid, ip, port);
    }

    public void OnConnectSuccess(String uuid, String ip, int port) {
        PviewLog.testPrint("OnConnectSuccess", uuid, ip, port);
    }

    public void setUserExitNotifyCallBack(UserExitNotifyCallBack mUserExitNotifyCallBack) {
        this.mUserExitNotifyCallBack = mUserExitNotifyCallBack;
    }

    public UserExitNotifyCallBack getUserExitNotifyCallBack() {
        return mUserExitNotifyCallBack;
    }
}
