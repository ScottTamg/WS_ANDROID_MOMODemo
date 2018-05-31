package com.wushuangtech.jni;

import android.util.Log;
import com.wushuangtech.api.JniWorkerThread;
import com.wushuangtech.inter.InstantRequestCallBack;
import com.wushuangtech.library.GlobalConfig;
import com.wushuangtech.library.GlobalHolder;
import com.wushuangtech.library.PviewConferenceRequest;
import com.wushuangtech.utils.PviewLog;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static com.wushuangtech.library.Constants.ERROR_LINK_OTHER_ANCHOR_RELINK;
import static com.wushuangtech.library.Constants.ERROR_LINK_OTHER_ANCHOR_TIMEOUT;
public class RoomJni {

    public static final int PERMISSIONTYPE_SPEAK = 1;

    /*
     * 未获取权限
     */
    public static final int PERMISSIONSTATUS_NORMAL = 1;
    /*
     * 权限申请中
     */
    public static final int PERMISSIONSTATUS_APPLYING = 2;
    /*
     * 已获取权限
     */
    public static final int PERMISSIONSTATUS_GRANTED = 3;

    public static final int AUDIOCODEC_DEFAULT = 0;
    public static final int AUDIOCODEC_AAC = 1;
    public static final int AUDIOCODEC_WB = 2;
    public static final int AUDIOCODEC_UWB = 3;

    public static final int ROOM_MODE_LIVE = 0;
    public static final int ROOM_MODE_COMMUNICATION = 1;

    public static final int ROOM_UR_NULL = 0;
    public static final int ROOM_UR_CHAIRMAN = 1;
    public static final int ROOM_UR_PARTICIPANT = 2;
    public static final int ROOM_UR_AUDIANCE = 3;

    private static RoomJni mRoomJni;
    private List<WeakReference<PviewConferenceRequest>> mCallBacks;
    private List<WeakReference<InstantRequestCallBack>> mInstantRequestCallBacks;
    private LinkTimer mLinkTimer = new LinkTimer();
    private HashMap<Long, Long> mLinks = new HashMap<>();

    private RoomJni() {
        this.mCallBacks = new ArrayList<>();
        mInstantRequestCallBacks = new ArrayList<>();
    }

    public static synchronized RoomJni getInstance() {
        if (mRoomJni == null) {
            synchronized (RoomJni.class) {
                if (mRoomJni == null) {
                    mRoomJni = new RoomJni();
                    if (!mRoomJni.initialize(mRoomJni)) {
                        throw new RuntimeException("can't initilaize RoomJni");
                    }
                }
            }
        }
        return mRoomJni;
    }

    /**
     * 添加自定义的回调，监听接收到的服务信令
     *
     * @param callback 回调对象
     */
    public void addCallback(PviewConferenceRequest callback) {
        this.mCallBacks.add(new WeakReference<>(callback));
    }

    public void addCallback(InstantRequestCallBack callback) {
        this.mInstantRequestCallBacks.add(new WeakReference<>(callback));
    }

    /**
     * 移除自定义添加的回调
     *
     * @param callback 回调对象
     */
    public void removeCallback(PviewConferenceRequest callback) {
        for (int i = 0; i < mCallBacks.size(); i++) {
            WeakReference<PviewConferenceRequest> wf = mCallBacks.get(i);
            if (wf != null && wf.get() != null) {
                if (wf.get() == callback) {
                    mCallBacks.remove(wf);
                    return;
                }
            }
        }
    }

    public void removeCallback(InstantRequestCallBack callback) {
        for (int i = 0; i < mInstantRequestCallBacks.size(); i++) {
            WeakReference<InstantRequestCallBack> wf = mInstantRequestCallBacks.get(i);
            if (wf != null && wf.get() != null) {
                if (wf.get() == callback) {
                    mInstantRequestCallBacks.remove(wf);
                    return;
                }
            }
        }
    }

    public native boolean initialize(RoomJni roomJni);

    public native void unInitialize();

    /**
     * 退出会议
     *
     * @param nRoomID 房间ID
     */
    public native void RoomExit(long nRoomID);

    /**
     * 将某人请出会议
     *
     * @param nUserID 需要请出会议的用户ID
     */
    public native void RoomKickUser(long nUserID);

    /**
     * 会议中申请权限
     *
     * @param type 权限类型
     */
    public native void RoomApplyPermission(int type);

    /**
     * 会议中释放权限
     *
     * @param type 权限类型
     */
    public native void RoomReleasePermission(int type);

    /**
     * 给一个用户授权
     *
     * @param nUserID           用户ID
     * @param nType             权限类型
     * @param nPermissionStatus 权限状态
     */
    public native void RoomGrantPermission(long nUserID, int nType, int nPermissionStatus);

    /**
     * 更改会议主席
     *
     * @param nUserID 用户ID
     */
    public native void RoomChangeChairman(long nUserID);

    public native void RoomQuickEnter(String appId, long nUserID, long nRoomID, int userRole, String rtmpUrl, String model , boolean recordMp4);

    public native void RoomNormalEnter(String appId, long nUserID, long nRoomID, String password, boolean mixVideo, String rtmpUrl, String model, boolean recordMp4);

    public native void UploadMyVideo(String szDeviceID, boolean bUpload);

    public native void LinkOtherAnchor(long nGroupID, long nUserID);

    public native void UnlinkOtherAnchor(long nGroupID, long nUserID, String szDeviceID);

    public native void SendCmdMsg(long nUserID, String msg);
    public native void SendCustomizedAudioMsg(String msg);
    public native void SendCustomizedVideoMsg(String msg);

    public native void RoomApplyChairman(String password);

    public native void RoomSetUUID(String uuid);

    public native void RoomSendAECParam(String model, int delayOffset);

    public native void SetAudioLevelReportInterval(int interval);

    public native void setServerAddress(String ip, int port);

    public native void SetPreferAudioCodec(int audioCodec, int bitrate);

    public native void SetRoomRequireChair(boolean require);

    public native void SetRoomCreateVideoMixer(boolean create);

    public native void MuteLocalVideo(boolean mute);

    public native void MuteAllRemoteAudio(boolean mute);

    public native void MuteAllRemoteVideo(boolean mute);

    public native void MuteRemoteAudio(long nUserID, boolean mute);

    public native void SetUseAudioServerMixer(boolean useAudioServerMixer);

    public native void MuteLocalAudio(boolean mute);
    //进房间前调用
    public native void SetVideoMixerParams(int bitrate, int fps, int width, int height);
    //进房间前调用
    public native void SetAudioMixerParams(int bitrate, int samplerate, int channels);

    public native void ClearGlobalStatus();

    public native void RoomChangeMyRole(int userRole);

    public native void SetVideoMixerBackgroundImgUrl(String url);

    public native void SetRoomLowerVideoMixer(boolean lower);

    public native void EnableCrossRoom(boolean crossRoom);

    public native void EnableDualVideoStream(boolean enable);

    public void roomExit(long nRoomID) {
        RoomExit(nRoomID);
        mLinks.clear();
    }
    public void LinkAnchor(long nGroupID, long nUserID) {
        synchronized (this) {
            if (mLinks.get(nUserID)== null || mLinks.get(nUserID) != nGroupID) {
                mLinks.put(nUserID, nGroupID);
        LinkOtherAnchor(nGroupID, nUserID);
        mLinkTimer.addTask(String.valueOf(nGroupID), String.valueOf(nUserID), 5000);
            } else {
                OnAnchorLinked(nGroupID, nUserID, String.valueOf(nUserID), ERROR_LINK_OTHER_ANCHOR_RELINK);
            }
    }
    }
    private void OnRoomEnter(long nConfID, int nResult, int userRole) {
        PviewLog.jniCall("OnRoomEnter", "nConfID : " + nConfID + " | nResult : " + nResult + " | userRole : " + userRole);
        if (GlobalConfig.trunOnCallback) {
            for (int i = 0; i < this.mInstantRequestCallBacks.size(); i++) {
                WeakReference<InstantRequestCallBack> wf = this.mInstantRequestCallBacks.get(i);
                if (wf != null && wf.get() != null) {
                    wf.get().OnEnterConfCallback(nConfID, nResult, userRole);
                }
            }
        }
    }

    private void OnRoomMemberEnter(long nConfID, long nUserID, String szUserXml,
                                   int userRole, int speakStatus) {
        PviewLog.jniCall("OnRoomMemberEnter", "nConfID : " + nConfID + " | nUserID : " + nUserID + " | szUserXml : " + szUserXml
                + " | userRole : " + userRole + " | speakStatus : " + speakStatus);
        if (GlobalConfig.trunOnCallback) {
            for (int i = 0; i < this.mCallBacks.size(); i++) {
                WeakReference<PviewConferenceRequest> wf = this.mCallBacks.get(i);
                if (wf != null && wf.get() != null) {
                    wf.get().OnConfMemberEnter(nConfID, nUserID, szUserXml, userRole, speakStatus);
                }
            }
        }
    }

    private void OnRoomMemberExit(long nConfID, long nUserID, int reason) {
        PviewLog.jniCall("OnRoomMemberExit", "nConfID : " + nConfID + " | nUserID : " + nUserID);
        if (GlobalConfig.trunOnCallback) {
            for (int i = 0; i < this.mCallBacks.size(); i++) {
                WeakReference<PviewConferenceRequest> wf = this.mCallBacks.get(i);
                if (wf != null && wf.get() != null) {
                    wf.get().OnConfMemberExitCallback(nConfID, nUserID , reason);
                }
            }
        }
    }

    private void OnRoomKicked(long nConfId, long nSrcUserId, long nDstUserId, int nReason , String uuid) {
        PviewLog.jniCall("OnRoomKicked", "nConfId : " + nConfId + " | nSrcUserId : " + nSrcUserId + " | nDstUserId : " + nDstUserId
                + " | nReason : " + nReason + " | uuid : " + uuid);
        if (GlobalConfig.trunOnCallback) {
            for (int i = 0; i < this.mCallBacks.size(); i++) {
                WeakReference<PviewConferenceRequest> wf = this.mCallBacks.get(i);
                if (wf != null && wf.get() != null) {
                    wf.get().OnKickConfCallback(nConfId, nSrcUserId, nDstUserId, nReason , uuid);
                }
            }
        }
    }

    private void OnRoomPermissionApply(long nUserID, int nPermissionType) {
        PviewLog.jniCall("OnRoomPermissionApply", "nUserID : " + nUserID + " | nPermissionType : " + nPermissionType);
        if (GlobalConfig.trunOnCallback) {
            for (int i = 0; i < this.mCallBacks.size(); i++) {
                WeakReference<PviewConferenceRequest> wf = this.mCallBacks.get(i);
                if (wf != null && wf.get() != null) {
                    wf.get().OnConfPermissionApply(nUserID, nPermissionType);
                }
            }
        }
    }

    private void OnRoomPermissionGranted(long nUserID, int nPermissionType, int nPermissionStatus) {
        PviewLog.jniCall("OnRoomPermissionGranted", "nUserID : " + nUserID + " | nPermissionType : " + nPermissionType
                + " | nPermissionStatus : " + nPermissionStatus);
        if (GlobalConfig.trunOnCallback) {
            for (int i = 0; i < this.mCallBacks.size(); i++) {
                WeakReference<PviewConferenceRequest> wf = this.mCallBacks.get(i);
                if (wf != null && wf.get() != null) {
                    wf.get().OnGrantPermissionCallback(nUserID, nPermissionType, nPermissionStatus);
                }
            }
        }
    }

    private void OnRoomChairChanged(long nConfId, long nUserId) {
        PviewLog.jniCall("OnRoomChairChanged", "nConfId : " + nConfId + " | nUserId : " + nUserId);
        if (GlobalConfig.trunOnCallback) {
            for (int i = 0; i < this.mCallBacks.size(); i++) {
                WeakReference<PviewConferenceRequest> wf = this.mCallBacks.get(i);
                if (wf != null && wf.get() != null) {
                    wf.get().OnConfChairChanged(nConfId, nUserId);
                }
            }
        }
    }

    private void OnRoomDisconnected(String uuid) {
        PviewLog.jniCall("OnRoomDisconnected", "OnConfDisconnected... uuid : " + uuid);
        if (GlobalConfig.trunOnCallback) {
            for (int i = 0; i < this.mCallBacks.size(); i++) {
                WeakReference<PviewConferenceRequest> wf = this.mCallBacks.get(i);
                if (wf != null && wf.get() != null) {
                    wf.get().OnConfDisconnected(uuid);
                }
            }
        }
    }

    private void OnUpdateDevParam(String devParam) {
        PviewLog.jniCall("OnUpdateDevParam", "devParam : " + devParam);
        if (GlobalConfig.trunOnCallback) {
            for (int i = 0; i < this.mCallBacks.size(); i++) {
                WeakReference<PviewConferenceRequest> wf = this.mCallBacks.get(i);
                if (wf != null && wf.get() != null) {
                    wf.get().OnUpdateDevParam(devParam);
                }
            }
        }
    }

    private void OnUpdateRtmpStatus(long nGroupID, String rtmpUrl, boolean status) {
        PviewLog.jniCall("OnUpdateRtmpStatus", "nGroupID : " + nGroupID + " | rtmpUrl : " + rtmpUrl + " | status : " + status);
        if (GlobalConfig.trunOnCallback) {
            for (int i = 0; i < this.mCallBacks.size(); i++) {
                WeakReference<PviewConferenceRequest> wf = this.mCallBacks.get(i);
                if (wf != null && wf.get() != null) {
                    wf.get().OnUpdateRtmpStatus(nGroupID, rtmpUrl, status);
                }
            }
        }
    }

    private void OnAnchorLinked(long nGroupID, long nUserID, String devID, int error) {
        PviewLog.jniCall("OnAnchorLinked", "nGroupID : " + nGroupID + " | nUserID : " + nUserID + " | devID : " + devID
                + " | error : " + error);
        if (GlobalConfig.trunOnCallback) {
            for (int i = 0; i < this.mCallBacks.size(); i++) {
                WeakReference<PviewConferenceRequest> wf = this.mCallBacks.get(i);
                if (wf != null && wf.get() != null) {
                    wf.get().OnAnchorLinked(nGroupID, nUserID, devID, error);
                    mLinkTimer.removeTask(String.valueOf(nGroupID), String.valueOf(nUserID));
                    if (error == 6)
                        mLinks.remove(nUserID);
                }
            }
        }
    }

    private void OnAnchorUnlinked(long nGroupID, long nUserID) {
        PviewLog.jniCall("OnAnchorUnlinked", "nGroupID : " + nGroupID + " | nUserID : " + nUserID);
        if (GlobalConfig.trunOnCallback) {
            for (int i = 0; i < this.mCallBacks.size(); i++) {
                WeakReference<PviewConferenceRequest> wf = this.mCallBacks.get(i);
                if (wf != null && wf.get() != null) {
                    mLinks.remove(nUserID);
                    wf.get().OnAnchorUnlinked(nGroupID, nUserID);
                }
            }
        }
    }

    private void OnAnchorLinkResponse(long nGroupID, long nUserID) {
        PviewLog.jniCall("OnAnchorLinkResponse", "nGroupID : " + nGroupID + " | nUserID : " + nUserID);
        if (GlobalConfig.trunOnCallback) {
            for (int i = 0; i < this.mCallBacks.size(); i++) {
                WeakReference<PviewConferenceRequest> wf = this.mCallBacks.get(i);
                if (wf != null && wf.get() != null) {
                    wf.get().OnAnchorLinkResponse(nGroupID, nUserID);
                }
            }
        }
    }

    private void OnAnchorUnlinkResponse(long nGroupID, long nUserID) {
        PviewLog.jniCall("OnAnchorUnlinkResponse", "nGroupID : " + nGroupID + " | nUserID : " + nUserID);
        if (GlobalConfig.trunOnCallback) {
            for (int i = 0; i < this.mCallBacks.size(); i++) {
                WeakReference<PviewConferenceRequest> wf = this.mCallBacks.get(i);
                if (wf != null && wf.get() != null) {
                    wf.get().OnAnchorUnlinkResponse(nGroupID, nUserID);
                }
            }
        }
    }

    private void OnRecvCmdMsg(long nGroupID, long nUserID, String msg) {
        PviewLog.jniCall("OnRecvCmdMsg", "nGroupID : " + nGroupID + " | nUserID : " + nUserID
                + " | msg : " + msg);
        if (GlobalConfig.trunOnCallback) {
            for (int i = 0; i < this.mCallBacks.size(); i++) {
                WeakReference<PviewConferenceRequest> wf = this.mCallBacks.get(i);
                if (wf != null && wf.get() != null) {
                    wf.get().OnRecvCmdMsg(nGroupID, nUserID, msg);
                }
            }
        }
    }

    private void OnReportMediaAddr(String aIp, String vIp) {
        PviewLog.jniCall("OnReportMediaAddr", "aIp : " + aIp + " | vIp : " + vIp);
        if (GlobalConfig.trunOnCallback) {
            for (int i = 0; i < this.mCallBacks.size(); i++) {
                WeakReference<PviewConferenceRequest> wf = this.mCallBacks.get(i);
                if (wf != null && wf.get() != null) {
                    wf.get().OnReportMediaAddr(aIp, vIp);
                }
            }
        }
    }

    private void OnMediaReconnect(int type) {
        PviewLog.jniCall("OnMediaReconnect", "type : " + type);
        if (GlobalConfig.trunOnCallback) {
            for (int i = 0; i < this.mCallBacks.size(); i++) {
                WeakReference<PviewConferenceRequest> wf = this.mCallBacks.get(i);
                if (wf != null && wf.get() != null) {
                    wf.get().OnMediaReconnect(type);
                }
            }
        }
    }

    private void OnUpdateVideoDev(long uid, String szXmlData) {
        PviewLog.jniCall("OnVideoUserDevices", "uid : " + uid + " | szXmlData : " + szXmlData);
        if (GlobalConfig.trunOnCallback) {
            for (int i = 0; i < mCallBacks.size(); i++) {
                WeakReference<PviewConferenceRequest> wf = mCallBacks.get(i);
                if (wf != null && wf.get() != null) {
                    wf.get().OnUpdateVideoDev(uid, szXmlData);
                }
            }
        }
    }

    private void OnAudioLevelReport(long nUserID, int audioLevel, int audioLevelFullRange) {
//        PviewLog.jniCall("OnAudioLevelReport", "uid : " + nUserID + " | audioLevel : "
//                + audioLevel+ " | audioLevelFullRange : " + audioLevelFullRange);
        if (GlobalConfig.trunOnCallback) {
            for (int i = 0; i < mCallBacks.size(); i++) {
                WeakReference<PviewConferenceRequest> wf = mCallBacks.get(i);
                if (wf != null && wf.get() != null) {
                    wf.get().OnAudioLevelReport(nUserID, audioLevel, audioLevelFullRange);
                }
            }
        }
    }

    private void OnRecvAudioMsg(String msg) {
        PviewLog.jniCall("OnRecvAudioMsg", "msg : " + msg);
        if (GlobalConfig.trunOnCallback) {
            for (int i = 0; i < mCallBacks.size(); i++) {
                WeakReference<PviewConferenceRequest> wf = mCallBacks.get(i);
                if (wf != null && wf.get() != null) {
                    wf.get().OnRecvAudioMsg(msg);
                }
            }
        }
    }

    private void OnRecvVideoMsg(String msg) {
        PviewLog.jniCall("OnRecvVideoMsg", "msg : " + msg);
        if (GlobalConfig.trunOnCallback) {
            for (int i = 0; i < mCallBacks.size(); i++) {
                WeakReference<PviewConferenceRequest> wf = mCallBacks.get(i);
                if (wf != null && wf.get() != null) {
                    wf.get().OnRecvVideoMsg(msg);
                }
            }
        }
    }

    private void OnStartSendVideo(boolean bMute, boolean bOpen) {
        PviewLog.jniCall("OnStartSendVideo", "Begin bMute : " + bMute + " | bOpen : " + bOpen);
        if (GlobalConfig.trunOnCallback) {
            for (int i = 0; i < mCallBacks.size(); i++) {
                WeakReference<PviewConferenceRequest> wf = mCallBacks.get(i);
                if (wf != null && wf.get() != null) {
                    wf.get().OnStartSendVideo(bMute, bOpen);
                }
            }
        }
    }

    private void OnStopSendVideo(int reason) {
        PviewLog.jniCall("OnStopSendVideo", "Begin");
        if (GlobalConfig.trunOnCallback) {
            for (int i = 0; i < mCallBacks.size(); i++) {
                WeakReference<PviewConferenceRequest> wf = mCallBacks.get(i);
                if (wf != null && wf.get() != null) {
                    wf.get().OnStopSendVideo(reason);
                }
            }
        }
    }

    private void OnStartSendAudio() {
        PviewLog.jniCall("OnStartSendAudio", "Begin");
        if (GlobalConfig.trunOnCallback) {
            for (int i = 0; i < mCallBacks.size(); i++) {
                WeakReference<PviewConferenceRequest> wf = mCallBacks.get(i);
                if (wf != null && wf.get() != null) {
                    wf.get().OnStartSendAudio();
                }
            }
        }
    }

    private void OnStopSendAudio() {
        PviewLog.jniCall("OnStopSendAudio", "Begin");
        if (GlobalConfig.trunOnCallback) {
            for (int i = 0; i < mCallBacks.size(); i++) {
                WeakReference<PviewConferenceRequest> wf = mCallBacks.get(i);
                if (wf != null && wf.get() != null) {
                    wf.get().OnStopSendAudio();
                }
            }
        }
    }

    private void OnUpdateAudioStatus(long userID, boolean speak, boolean server_mix) {
        PviewLog.jniCall("OnUpdateAudioStatus", "Begin");
        if (GlobalConfig.trunOnCallback) {
            for (int i = 0; i < mCallBacks.size(); i++) {
                WeakReference<PviewConferenceRequest> wf = mCallBacks.get(i);
                if (wf != null && wf.get() != null) {
                    wf.get().OnUpdateAudioStatus(userID, speak, server_mix);
                }
            }
        }
    }

    private void OnRemoteAudioMuted(long userID, boolean muted) {
        PviewLog.jniCall("OnRemoteAudioMuted", "userID : " + userID + " | muted : " + muted);
        Log.d("zhx", "OnRemoteAudioMuted: " + userID + " " + muted);
        if (GlobalConfig.trunOnCallback) {
            for (int i = 0; i < mCallBacks.size(); i++) {
                WeakReference<PviewConferenceRequest> wf = mCallBacks.get(i);
                if (wf != null && wf.get() != null) {
                    wf.get().OnRemoteAudioMuted(userID, muted);
                }
            }
        }
    }

    private void OnUserRoleChanged(long userID, int userRole) {
        PviewLog.jniCall("OnUserRoleChanged", "userID : " + userID + " | userRole : " + userRole);
        if (GlobalConfig.trunOnCallback) {
            for (int i = 0; i < mCallBacks.size(); i++) {
                WeakReference<PviewConferenceRequest> wf = mCallBacks.get(i);
                if (wf != null && wf.get() != null) {
                    wf.get().OnUserRoleChanged(userID, userRole);
                }
            }
        }
    }

    private void OnFirstAudioSent() {
        PviewLog.jniCall("OnFirstAudioSent", "");
        if (GlobalConfig.trunOnCallback) {
            for (int i = 0; i < mCallBacks.size(); i++) {
                WeakReference<PviewConferenceRequest> wf = mCallBacks.get(i);
                if (wf != null && wf.get() != null) {
                    wf.get().OnFirstAudioSent();
                }
            }
        }
    }

    private void OnFirstVideoSent() {
        PviewLog.jniCall("OnFirstVideoSent", "");
        if (GlobalConfig.trunOnCallback) {
            for (int i = 0; i < mCallBacks.size(); i++) {
                WeakReference<PviewConferenceRequest> wf = mCallBacks.get(i);
                if (wf != null && wf.get() != null) {
                    wf.get().OnFirstVideoSent();
                }
            }
        }
    }

    /**
     * 当网络异常断开后，将尝试重连，
     * 若在服务器容忍的超时范围外才重连上服务器，服务器将会拒绝，其房间状态将不可用。
     * 此时触发该回调，上层应该在收到此回调后退出房间。
     */
    private void OnReconnectTimeout() {
        PviewLog.jniCall("OnReconnectTimeout", "");
        if (GlobalConfig.trunOnCallback) {
            for (int i = 0; i < this.mCallBacks.size(); i++) {
                WeakReference<PviewConferenceRequest> wf = this.mCallBacks.get(i);
                if (wf != null && wf.get() != null) {
                    wf.get().OnReconnectTimeout();
                }
            }
        }
    }

    private void OnRtpRtcp(boolean bRtpRtcp) {
        PviewLog.jniCall("OnRtpRtcp", "bRtpRtcp : " + bRtpRtcp);
        if (GlobalConfig.trunOnCallback) {
            for (int i = 0; i < this.mCallBacks.size(); i++) {
                WeakReference<PviewConferenceRequest> wf = this.mCallBacks.get(i);
                if (wf != null && wf.get() != null) {
                    wf.get().OnRtpRtcp(bRtpRtcp);
                }
            }
        }
    }

    private void OnConnect(String uuid, String ip, int port) {
        PviewLog.jniCall("OnConnect", "uuid : " + uuid + " ip : " + ip + " port : " + port);
        if (GlobalConfig.trunOnCallback) {
            for (int i = 0; i < this.mCallBacks.size(); i++) {
                WeakReference<PviewConferenceRequest> wf = this.mCallBacks.get(i);
                if (wf != null && wf.get() != null) {
                    wf.get().OnConnect(uuid , ip , port);
                }
            }
        }
    }

    private void OnConnectSuccess(String uuid, String ip, int port) {
        PviewLog.jniCall("OnConnectSuscess", "uuid : " + uuid + " ip : " + ip + " port : " + port);
        if (GlobalConfig.trunOnCallback) {
            for (int i = 0; i < this.mCallBacks.size(); i++) {
                WeakReference<PviewConferenceRequest> wf = this.mCallBacks.get(i);
                if (wf != null && wf.get() != null) {
                    wf.get().OnConnectSuccess(uuid , ip , port);
                }
            }
        }
    }

    private class LinkTimer extends Timer {
        private ArrayList<LinkTask> tasks = new ArrayList<>();
        public void addTask(String roomID, String userID, int time) {
            LinkTask linkTask = new LinkTask(roomID, userID);
            tasks.add(linkTask);
            this.schedule(linkTask, time);
        }
        public void removeTask(String roomID, String userID) {
            for (int i = 0; i < tasks.size(); i ++) {
                if (roomID.equals(tasks.get(i).mRoomID) && userID.equals(tasks.get(i).mUserID)) {
                    tasks.get(i).cancel();
                    tasks.remove(i);
                    return;
                }
            }
        }
        private class LinkTask extends TimerTask {
            public String mRoomID, mUserID;
            public LinkTask(String roomID, String userID) {
                this.mRoomID = roomID;
                this.mUserID = userID;
            }
            @Override
            public void run() {
                // 超时没有拿到devID 暂时使用UserID
                UnlinkOtherAnchor(Long.parseLong(mRoomID), Long.parseLong(mUserID), mUserID);
                OnAnchorLinked(Long.parseLong(mRoomID), Long.parseLong(mUserID), mUserID, ERROR_LINK_OTHER_ANCHOR_TIMEOUT);
            }
        }
    }
}