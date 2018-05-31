package com.wushuangtech.inter;

import com.wushuangtech.bean.ChatInfo;
import com.wushuangtech.bean.ConfVideoFrame;
import com.wushuangtech.bean.LocalAudioStats;
import com.wushuangtech.bean.LocalVideoStats;
import com.wushuangtech.bean.RemoteAudioStats;
import com.wushuangtech.bean.RemoteVideoStats;
import com.wushuangtech.bean.RtcStats;
import com.wushuangtech.library.Constants;

/**
 * 主体SDK的回调接收接口
 */
public interface TTTRtcEngineEventInter {

    /**
     * 加入频道回调，表示客户端已经登入服务器
     *
     * @param channel 频道名称
     * @param uid     登录的用户ID
     */
    void onJoinChannelSuccess(String channel, long uid);

    /**
     * 表示SDK运行时出现了（网络或媒体相关的）错误。
     *
     * @param err 错误码 {@link Constants#ERROR_ENTER_ROOM_TIMEOUT}
     */
    void onError(int err);

    /**
     * 其他用户加入当前频道回调
     *
     * @param uid      加入频道的用户ID
     * @param identity 加入频道的用户的身份，副播或观众
     */
    void onUserJoined(long uid, int identity);

    /**
     * 本地视频显示回调，提示第一帧本地视频画面已经显示在屏幕上。
     *
     * @param width  该用户视频显示的宽度
     * @param height 该用户视频显示的高度
     */
    void onFirstLocalVideoFrame(int width, int height);

    /**
     * 远端视频显示回调,第一帧远程视频显示在视图上时，触发此调用
     *
     * @param uid    加入频道的用户ID
     * @param width  该用户视频显示的宽度
     * @param height 该用户视频显示的高度
     */
    void onFirstRemoteVideoFrame(long uid, int width, int height);

    /**
     * 收到第一帧远程视频流并解码成功时，触发此调用。应用程序可以在此回调中设置该用户的view。
     *
     * @param uid    加入频道的用户ID
     * @param width  视频流宽
     * @param height 视频流高
     */
    void onFirstRemoteVideoDecoded(long uid,
                                   int width,
                                   int height);

    /**
     * 收到远程视频流解码帧，触发此调用。。
     *
     * @param uid    视频流所属用户的ID
     * @param mFrame 视频流信息体
     */
    void onRemoteVideoDecoded(long uid, ConfVideoFrame mFrame);

    /**
     * 其他用户离开当前频道回调，主动离开或掉线
     *
     * @param uid    离开频道的用户ID
     * @param reason 该用户离开频道的原因 {@link Constants#USER_OFFLINE_QUIT}
     */
    void onUserOffline(long uid, int reason);

    /**
     * 连接丢失回调
     */
    void onConnectionLost();

    /**
     * 本地视频统计回调，报告更新本地视频统计信息，该回调函数每两秒触发一次
     *
     * @param stats 本地视频相关的统计信息，包含:
     *              sentBitrate:（上次统计后）发送的码率(kbps)
     *              sentFrameRate:（上次统计后）发送的帧率(fps)
     */
    void onLocalVideoStats(LocalVideoStats stats);

    /**
     * 远端视频统计回调，报告更新远端视频统计信息，该回调函数每两秒触发一次
     *
     * @param stats 远端视频的统计信息，包含:
     *              uid: 用户ID，指定是哪个用户的视频流
     *              receivedBitrate: 接收码率(kbps)
     *              receivedFrameRate: 接收帧率(fps)
     */
    void onRemoteVideoStats(RemoteVideoStats stats);

    /**
     * 本地音频统计回调，报告更新本地音频统计信息，该回调函数每两秒触发一次
     *
     * @param stats 本地音频相关的统计信息，包含:
     *              sentBitrate:（上次统计后）发送的码率(kbps)
     */
    void onLocalAudioStats(LocalAudioStats stats);

    /**
     * 远端音频统计回调，报告更新远端音频统计信息，该回调函数每两秒触发一次
     *
     * @param stats 远端音频的统计信息，包含:
     *              uid: 用户ID，指定是哪个用户的音频流
     *              receivedBitrate: 接收码率(kbps)
     */
    void onRemoteAudioStats(RemoteAudioStats stats);

    /**
     * 摄像头启用回调，提示已成功打开摄像头
     */
    void onCameraReady();

    /**
     * 提示有其他用户启用/关闭了视频功能。关闭视频功能是指该用户只能进行语音通话，不能显示、
     * 发送自己的视频。
     *
     * @param uid     用户ID
     * @param enabled 用户已启用/关闭了视频功能
     */
    void onUserEnableVideo(long uid, boolean enabled);

    /**
     * 应用程序调用leaveChannel()方法时，SDK提示应用程序离开频道成功。
     *
     * @param stats 通话相关的统计信息。
     */
    void onLeaveChannel(RtcStats stats);

    /**
     * 说话声音音量提示回调。默认禁用。可以通过 enableAudioVolumeIndication 方法设置。
     *
     * @param nUserID             说话者的用户ID
     * @param audioLevel          说话者的音量，在0-9之间
     * @param audioLevelFullRange 说话者的音量，音量范围更大0-32767.5之间
     */
    void onAudioVolumeIndication(long nUserID, int audioLevel, int audioLevelFullRange);

    /**
     * 统计数据回调。该回调定期上报 Engine 的运行时的状态，每两秒触发一次
     *
     * @param stats 通话相关的统计信息<br/>
     *              totalDuration: 通话时长（秒），累计值<br/>
     *              txBytes: 发送字节数（bytes), 累计值<br/>
     *              rxBytes: 接收字节数（bytes), 累计值<br/>
     *              txKBitRate: 发送码率（kbps), 瞬时值<br/>
     *              rxKBitRate: 接收码率（kbps), 瞬时值<br/>
     *              lastmileQuality: 客户端接入网络质量<br/>
     *              cpuTotalQuality: 当前系统的CPU使用率（%）<br/>
     *              cpuAppQuality: 当前应用程序的CPU使用率（%）<br/>
     */
    void onRtcStats(RtcStats stats);

    /**
     * 语音路由已变更回调。当调用 setEnableSpeakerphone 成功时，SDK会通过该回调通知App语音路由状态已发生变化。
     *
     * @param routing 当前已切换的语言路由。
     */
    void onAudioRouteChanged(int routing);

    /**
     * 根据服务器返回的Sei，获得副播的显示信息
     *
     * @param sei 包含副播显示位置信息的json字符串
     */
    void onSetSEI(String sei);

    /**
     * 提示视频功能已停止。应用程序如需在停止视频后对view做其他处理（例如显示其他画面），可以在这个回调中进行。
     */
    void onVideoStopped();

    /**
     * 在调用joinChannel时如果指定了Channel Key，由于Channel Key具有一定的时效，在通话过程中SDK可能由于网络原因和服务器失去连接，重连时可能需要新的Channel Key。
     * 该回调通知APP需要生成新的Channel Key，并需调用renewChannelKey()为SDK指定新的Channel Key。
     */
    void onRequestChannelKey();

    /**
     * 用户角色切换回调。
     *
     * @param userID   切换角色的用户ID
     * @param userRole 该用户切换的角色
     */
    void onUserRoleChanged(long userID, int userRole);

    /**
     * 当前录屏时间的回调。
     *
     * @param tims 已经录屏的时间
     */
    void onScreenRecordTime(int tims);

    /**
     * 聊天消息发送的回调
     *
     * @param chatInfo 消息信息
     * @param error  发送的状态码
     */
    void OnChatMessageSent(ChatInfo chatInfo, int error);

    /**
     * 信令发送回掉
     *
     * @param sSeqID 消息的ID
     * @param error  发送的状态码
     */
    void OnSignalSent(String sSeqID, int error);

    /**
     * 聊天消息接收的回调
     *
     * @param nSrcUserID 该消息的发送者ID
     * @param chatInfo   消息信息,图片暂不支持
     */
    void OnChatMessageRecived(long nSrcUserID, ChatInfo chatInfo);

    /**
     * 信令接收的回调
     *
     * @param nSrcUserID 该消息的发送者ID
     * @param sSeqID     消息ID
     * @param strData    消息体XML
     */
    void OnSignalRecived(long nSrcUserID, String sSeqID, String strData);

    /**
     * 聊天语音消息播放完毕的回调
     */
    void onPlayChatAudioCompletion(String filePath);

    /**
     * 用户静音回调
     *
     * @param uid   用户ID
     * @param muted True: 该用户已静音音频
     *              False: 该用户已取消音频静音
     */
    void onUserMuteAudio(long uid, boolean muted);

    /**
     * 聊天语音识别完成回掉
     */
    void onSpeechRecognized(String str);

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
     * 用户的H264 SEI内容
     *
     * @param sei_content sei内容
     * @param uid         用户ID
     */
    void reportH264SeiContent(String sei_content, long uid);

    /**
     * RTMP推流状态回调
     */
    void onStatusOfRtmpPublish(int errorType);
}
