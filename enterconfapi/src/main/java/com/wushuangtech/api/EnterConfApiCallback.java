package com.wushuangtech.api;

/**
 * Created by apple on 16/10/14.
 */

import java.util.ArrayList;

/**
 * sdk回调对象,由调用方实现
 */
public interface EnterConfApiCallback {

    int RE_NEW_CHANNEL_KEY_SUCCESS = 0;
    int RE_NEW_CHANNEL_KEY_FAILD = -1;
    int CHANNELKEYEXPIRED = 9; // Channel Key失效

    int USER_EXIT_REASON_NORMAL = 1;    // 正常退出
    int USER_EXIT_REASON_TIMEOUT = 2;   // 超时退出
    int USER_EXIT_REASON_LINKCLOSE = 3; // 网络断开退出

    class GSVideoStats {
        public long userId;
        public int bitrate;
        public int fps;
    }

    class GSAudioStats {
        public long userId;
        public int lossRate; // 丢包率计算为lossRate/2^14
    }

    /**
     * 初始化Api的回调
     *
     * @param errNo  ENTERCONFAPI_NOERROR:成功 ENTERCONFAPI_TIMEOUT:超时失败 ENTERCONFAPI_ENTER_FAILED:无法连接服务器 ENTERCONFAPI_VERIFY_FAILED:错误安全码 ENTERCONFAPI_BAD_VERSION:版本错误
     * @param isHost 是否获取了主播身份
     */
    void onEnterRoom(int errNo, boolean isHost);

    /**
     * 退出会议的回调
     * 网络连接断开也会触发该回调, 为保证状态的一致性，网络异常断开后底层不再重复触发重连机制，由sdk调用方再次调用initConfApi重新进入
     */
    void onExitRoom();

    /**
     * 退出房间
     *
     * @param unexpected 是否因发生异常状况退出
     */
    public abstract void exitRoom(boolean unexpected);

    /**
     * 其他人进入会议的回调
     *
     * @param sessionId   sessionId
     * @param userid      进入的用户Id
     * @param deviceInfo  用户设备信息
     * @param userRole    用户角色
     * @param speakStatus 发言状态
     */
    void onMemberEnter(long sessionId, long userid, String deviceInfo, int userRole, int speakStatus);

    /**
     * 其他人退出会议的回调
     *
     * @param sessionId sessionId
     * @param unserid   退出用户的Id
     */
    void onMemberExit(long sessionId, long unserid , int reason);

    /**
     * 按比例设置视频小窗口的位置回调
     *
     * @param operUserId 操作人
     * @param userId     被操作人
     * @param devId      被操作人的设备Id
     * @param x          相对于大窗口的位置 0.0~1.0
     * @param y          相对于大窗口的位置 0.0~1.0
     * @param width      相对于大窗口的位置 0.0~1.0
     * @param height     相对于大窗口的位置 0.0~1.0
     */
    void onSetSubVideoPosRation(long operUserId, long userId, String devId, double x, double y, double width, double height);

    /**
     * 收到sei设置信息
     *
     * @param userId 操作人
     * @param sei    sei,json格式
     */
    void onSetSei(long userId, String sei);

    /**
     * 当被host踢出时收到的回调
     * 注意：该回调触发后,依然会触发 onExitRoom(true) 的回调
     *
     * @param sessionId  房间的sessionId
     * @param operUserId 操作者id
     * @param userId     被踢人id
     * @param reason     被踢出原因
     */
    void onKickedOut(long sessionId, long operUserId, long userId, int reason);

    /**
     * 当网络连接断开、推流失败或中断时触发的回调
     * 注意：该回调触发后,依然会触发 onExitRoom(true) 的回调，此时才是完全退出
     */
    void onDisconnected(int errNo);

    /**
     * 进房间推流成功后的回调
     * 注意：若失败不会触发该回调，而是走okKickedOut流程
     *
     * @param sessionId 房间的sessionId
     * @param rtmpUrl   推流地址
     * @param status    true
     */
    void onUpdateRtmpStatus(long sessionId, String rtmpUrl, boolean status);

    /**
     * 连麦其他主播成功或失败的回调
     * 调用LinkOtherAnchor后触发
     *
     * @param sessionId 对方房间的sessionId
     * @param userId    对方的id
     * @param devID     对方的设备ID
     * @param error     0-成功
     */
    void onAnchorEnter(long sessionId, long userId, String devID, int error);

    /**
     * 结束与其他主播连麦的回调
     * 调用UnlinkOtherAnchor后触发
     *
     * @param sessionId 对方房间的sessionId
     * @param userId    对方的id
     */
    void onAnchorExit(long sessionId, long userId);

    /**
     * 其他主播向"我"发起连麦的回调
     *
     * @param sessionId 对方房间的sessionId
     * @param userId    对方的id
     */
    void onAnchorLinkResponse(long sessionId, long userId);

    /**
     * 其他主播结束与"我"连麦的回调
     *
     * @param sessionId 对方房间的sessionId
     * @param userId    对方的id
     */
    void onAnchorUnlinkResponse(long sessionId, long userId);

    /**
     * 申请发言回调
     *
     * @param userId 申请发言的用户id
     */
    void onApplySpeakPermission(long userId);

    /**
     * 申请发言许可回调
     *
     * @param userId 申请发言的用户id
     * @param type
     */
    void onGrantPermissionCallback(long userId, int type, int status);

    /**
     * 会议主席变更
     *
     * @param sessionId 会议ID
     * @param userId    主席id
     */
    void onConfChairmanChanged(long sessionId, long userId);

    void onAudioLevelReport(long userId, int audioLevel, int audioLevelFullRange);

    /**
     * 用户静音回调
     *
     * @param muted  是否静音
     * @param userId 用户Id
     */
    void onAudioMuted(boolean muted, long userId);

    /**
     * 用户禁用视频回调
     *
     * @param muted  是否禁用视频
     * @param userId 用户Id
     */
    void onVideoMuted(boolean muted, long userId);

    /**
     * 接收到自定义消息回调
     *
     * @param userId    消息发送者id
     * @param msg       消息内容
     */
    void onRecvCustomizedMsg(long userId, String msg);

    /**
     * 接收到自定义音频消息的回调
     *
     * @param msg        自定义消息
     */
    void onRecvCustomizedAudioMsg(String msg);

    /**
     * 接收到自定义视频消息的回调
     *
     * @param msg 自定义消息
     */
    void onRecvCustomizedVideoMsg(String msg);

    /**
     * 用户静音回调
     *
     * @param muted  是否静音
     * @param userID 用户Id
     */
    void OnRemoteAudioMuted(long userID, boolean muted);
    /**
     * Channel Key过期回调
     */
    void onRequestChannelKey();
    /**
     * Channel Key重新申请结果回掉
     *
     * @param result        0:成功  <0:失败
     */
    void onRenewChannelKeyResult(int result);
    /**
     * 视频上行状态回调，每2秒更新一次
     *
     *  @param videoStats 统计信息，包括发送码率和帧率
     */
    void onReportLocalVideoStats(GSVideoStats videoStats);
    /**
     * 视频上行丢包率回调，每2秒更新一次
     *
     * @param lossRate 丢包率 0 - 1.0
     */
    void onReportLocalVideoLossRate(float lossRate);
    /**
     * 视频下行状态回调，每2秒更新一次
     *
     *  @param videoStats 统计信息，包括接收码率和帧率
     */
    void onReportRemoteVideoStats(ArrayList<GSVideoStats> videoStats);

    void onReportRemoteAudioStats(ArrayList<GSAudioStats> audioStats);
    /**
     * 用户是否启用双流功能
     *
     * @param enabled YES-启用，NO-停用
     * @param userId   用户Id
     */
    void onVideoaDualStreamEnabled(boolean enabled, long userId);
}

