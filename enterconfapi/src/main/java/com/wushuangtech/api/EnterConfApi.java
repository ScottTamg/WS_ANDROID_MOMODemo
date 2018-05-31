package com.wushuangtech.api;

import android.content.Context;

import java.util.List;

public abstract class EnterConfApi {

    public enum RoomMode
    {
        ROOM_MODE_COMMUNICATION,    // 互动模式
        ROOM_MODE_LIVE,             // 直播模式
        ROOM_MODE_GAME_FREE,        // 游戏自由模式
        ROOM_MODE_GAME_COMMAND,     // 游戏指挥模式
        ROOM_MODE_CONFERENCE,       // 会议模式
        ROOM_MODE_MANUAL,           // 手动模式
        ROOM_MODE_UNFINE,           // 未定义模式
        ;

        public static RoomMode getValue(int num) {
            switch (num) {
                case 0:
                    return ROOM_MODE_COMMUNICATION;
                case 1:
                    return ROOM_MODE_LIVE;
                case 2:
                    return ROOM_MODE_GAME_FREE;
                case 3:
                    return ROOM_MODE_GAME_COMMAND;
                default:
                    return ROOM_MODE_UNFINE;
            }
        }
    }

    /**
     *  混屏视频的位置信息
     *  mUserID      混屏视频用户ID
     *  x      相对于大窗口的范围 0.0~1.0
     *  y      相对于大窗口的范围 0.0~1.0
     *  w      相对于大窗口的范围 0.0~1.0
     *  h      相对于大窗口的范围 0.0~1.0
     */
    public static class VideoPosRation
    {
        public long id;
        public float x;
        public float y;
        public float w;
        public float h;
        public int z =0;// 主播在上还是在下0：下，1:上
    }

    /**
     * 获取api对象实例，singleton
     *
     * @return 对象实例
     */
    public static synchronized EnterConfApi getInstance() {
        return EnterConfApiImpl.getInstance();
    }

    /**
     *  初始化sdk, 加载资源
     *
     *  @param appID 应用ID，由 Wushuangtech 统一分配，用于区分不同的客户和应用。
     *  @param context 程序上下文
     */
    public abstract void setup(String appID, Context context , int logLevel);

    /**
     *  初始化sdk, 加载资源
     *
     *  @param appID 应用ID，由 Wushuangtech 统一分配，用于区分不同的客户和应用。
     *  @param context 程序上下文
     *  @param enableChat 是否启用聊天
     *
     */
    public abstract void setup(String appID, Context context, boolean enableChat , int logLevel);

    public abstract void setAppID(String mAppID);

    /**
     *  反始化sdk, 释放资源
     */
    public abstract void teardown();

    /**
     *  设置sdk回调对象，由调用方赋值
     */
    public abstract void setEnterConfApiCallback(EnterConfApiCallback callback);

    /**
     * 设置服务器地址
     * 若不调用该接口sdk默认使用国士无双服务器
     *
     * @param ip    ip地址
     * @param port  端口
     *
     */
    public abstract void setServerAddress(String ip, int port);

    /**
     *
     * 设置房间模式
     * 直播模式下，必须主播先进入房间。副播发言需要主播审批。当主播退出，副播也会被踢出房间。直播模式会推流到cdn
     * 互动模式，所有用户身份平等，可任意进出房间，可自由发言。不会推流到cdn
     * 会议模式，房间需要提前预定
     * 全手动模式
     *
     * 默认不启用服务器混音
     *
     * @param mode  房间模式
     */
    public abstract void setRoomMode(RoomMode mode);

    /**
     *
     * 设置房间模式
     * 直播模式下，必须主播先进入房间。副播发言需要主播审批。当主播退出，副播也会被踢出房间。直播模式会推流到cdn
     * 互动模式，所有用户身份平等，可任意进出房间，可自由发言。不会推流到cdn
     * 会议模式，房间需要提前预定
     * 全手动模式
     *
     * @param mode  房间模式
     * @param useServerAudioMixer 是否启用服务器混音，启用服务器混音适用于纯音频场景
     */
    public abstract void setRoomMode(RoomMode mode, boolean useServerAudioMixer);

    /**
     * 获取当前房间模式
     *
     * @return 房间模式
     */
    public abstract RoomMode getRoomMode();

    /**
     *
     * 设置房间是否录制Mp4
     *
     * @param flag  是否录制mp4，默认不录制。
     */
    public abstract void setRecordMp4Flag(boolean flag);

    /**
     * 使用高音质
     * 建议仅在音频模式下使用
     *
     * @param enable 是否启用
     */
    public abstract void useHighQualityAudio(boolean enable);

    /**
     * 设置服务器混屏参数
     * @param bitrate 码率 单位kbps, 取值范围 0~3000
     * @param fps     帧率
     * @param width   视频宽度
     * @param height  视频高度
     */
    public abstract void setVideoMixerParams(int bitrate, int fps, int width, int height);

    /**
     * 设置服务器混屏参数
     * @param bitrate       码率 单位kbps, 取值范围 0~192
     * @param samplerate    采样率 ：8000， 16000， 24000， 32000， 44100， 48000
     * @param channels      通道数 ：1， 2
     */
    public abstract void setAudioMixerParams(int bitrate, int samplerate, int channels);

    /**
     *  进入需要鉴权的房间
     *
     *  @param channelKey  用于鉴权
     *  @param userId    用户Id
     *  @param sessionId sessionId
     *  @param userRole  用户角色
     *  @param rtmpUrl   推流地址
     *
     *  @return 调用exitConf后,过快再次调用该函数，可能返回false
     */
    public abstract boolean enterRoom(String channelKey, long userId, long sessionId, int userRole, String rtmpUrl);

    public abstract boolean enterAudioRoom(String channelKey , long userId, long sessionId, int userRole, String rtmpUrl);

    /**
     *  退出房间
     */
    public abstract void exitRoom();

    public abstract void renewChannelKey(String channelKey);

    /**
     *  踢出房间
     *  踢人成功后会受到被踢人的onMemberExit回调, host调用有效
     *
     *  @param userId 被踢者userId
     */
    public abstract void kickUser(long userId);

    /**
     *  是否混屏guest视频，并且设置混频视频位置
     *  若不调用该函数，默认不混屏guest,
     *  在有guest进入（onMemberEnter）后有效
     *
     *  @param userId 用户ID
     *
     *  @param enable YES-混屏guest NO-不混屏guest
     *
     *  @param poss  混屏数据体集合位置
     *
     *  @return true-成功 false-失败
     */
    public abstract boolean mixAndSetSubVideoPos(long userId, String devId, boolean enable, List<VideoPosRation> poss);

    /**
     *  是否混屏guest视频，并且设置混频视频位置
     *  若不调用该函数，默认不混屏guest,
     *  在有guest进入（onMemberEnter）后有效
     *
     *  @param userId 用户ID
     *
     *  @param enable YES-混屏guest NO-不混屏guest
     *
     *  @param pos    混屏数据
     *
     *  @return true-成功 false-失败
     */
    public abstract boolean mixAndSetSubVideoPos(long userId, String devId, boolean enable, VideoPosRation pos);

    /**
     *  改变混屏布局
     *
     *  @param poss    混屏数据体集合位置
     *
     */
    public abstract void setSubVideoPosRation(List<VideoPosRation> poss);

    /**
     * 设置h.264 sei内容
     * @param sei json格式
     */
    public abstract void setSei(String sei , String szSeiExt);

    /**
     * 打开远端视频
     *
     * @param userid    用户ID
     * @param deviceid  设备ID
     */
    public abstract void openDeviceVideo(long userid,String deviceid);

    /**
     * 关闭远端视频
     *
     * @param userid    用户ID
     * @param deviceid  设备ID
     */
    public abstract void closeDeviceVideo(long userid,String deviceid);

    /**
     * 打开双流视频
     *
     * @param userid    用户ID
     */
    public abstract void openDualVideo(long userid , String deviceid);

    /**
     * 关闭双流视频
     *
     * @param userid    用户ID
     */
    public abstract void closeDualVideo(long userid , String deviceid);

    /**
     * 设置音量上报间隔
     *
     * @param interval 毫秒数
     */
    public abstract void setAudioLevelReportInterval(int interval);

    /**
     * 禁用本地音频
     *
     * @param mute true-禁用，false-启用
     */
    public abstract void muteLocalAudio(boolean mute);

    /**
     * 禁用本地视频
     *
     * @param mute true-禁用，false-启用
     */
    public abstract void muteLocalVideo(boolean mute);

    /**
     * 禁用所有远端音频
     *
     * @param mute true-禁用，false-启用
     */
    public abstract void muteAllRemoteAudio(boolean mute);

    /**
     * 禁用所有远端视频
     *
     * @param mute true-禁用，false-启用
     */
    public abstract void muteAllRemoteVideo(boolean mute);

    /**
     * 静音远端用户
     *
     * @param userId 远端用户Id
     * @param mute 是否静音
     */
    public abstract void muteRemoteAudio(long userId, boolean mute);

    /**
     * 申请/释放发言权限
     *
     * @param enable true:申请 false:释放
     */
    public abstract void applySpeakPermission(boolean enable);

    /**
     * 同意某人的发言申请
     * 在直播以及互动模式下无需调用该函数，自动同意发言申请
     *
     * @param userId    用户ID
     */
    public abstract void grantSpeakPermission(long userId);

    /**
     * 与其他主播连麦
     *
     * @param sessionId 对方房间的sessionId
     * @param userId 对方主播的userId
     */
    public abstract void linkOtherAnchor(long sessionId, long userId);

    /**
     * 结束与其他主播连麦
     *
     * @param sessionId 对方房间的sessionId
     * @param userId 对方主播的userId
     * @param devId 对方的设备ID
     */
    public abstract void unlinkOtherAnchor(long sessionId, long userId, String devId);

    /**
     * 调整远端回声参数
     *
     * @param userid 用户ID
     * @param param  参数值 -250 ~ 250之间，通常应该大于0
     */
    public abstract void adjustRemoteAudioParam(long userid, int param);

    /*
     * 当aec参数调整好后，可将该参数发送至服务器，用于服务器做数据收集和分析以便进行自动分配
     *
     * @param delayOffset aec参数值
     */
    public abstract void sendAecParam(int delayOffset);

    /*
     * 获取默认aec参数值，进入房间成功后调用
     *
     * @return aec参数值
     */
    public abstract int getDefaultAecParam();

    public abstract boolean uploadMyVideo(boolean enable);

    /*
     * 禁用音频
     */
    public abstract void enableAudio(boolean enable);

    /*
     * 发送自定义消息
     *
     * @param userId    接收者id，如果为0，群发
     * @param msg       消息内容
     */
    public abstract void sendCustomizedMsg(long userId, String msg);

    /*
     * 发送自定义音频消息
     * 伴随音频链路发送
     *
     * @param msg       消息内容
     */
    public abstract void sendCustomizedAudioMsg(String msg);

    /*
     * 发送自定义视频消息
     * 伴随视频链路发送
     *
     * @param msg       消息内容
     */
    public abstract void sendCustomizedVideoMsg(String msg);

    /*
     * 启用聊天功能
     */
    public abstract void enableChat();

    /*
     * 启用信令
     */
    public abstract void enableSignal();

    /*
     * 发送房间聊天信息
     *
     * @param msg       消息内容
     */
    public abstract void sendChat(long nDstUserID, int type, String sSeqID, String sData);

    /*
     * 发送信令
     *
     * @param msg       消息内容
     */
    public abstract void sendSignal(long nDstUserID, String sSeqID, String sData);

    /*
     * 开始采集音频（语音消息）
     */
    public abstract void startRecordChatAudio();

    /*
     * 停止采集并且开始发送（语音消息）
     *
     * @param nDstUserID     接收方ID，0表示发送给房间内所有人
     */
    public abstract int stopRecordAndSendChatAudio(long nDstUserID, String sSeqID);

    /*
     * 取消发送语音消息
     */
    public abstract void cancleRecordChatAudio();

    /*
     * 播放语音消息
     *
     * @param audioPath       语音消息本地目录
     */
    public abstract void playChatAudio(String audioPath);

    /*
     * 停止播放语音消息
     */
    public abstract void stopChatAudio();

    /*
     * 判断是否正在播放语音消息
     */
    public abstract boolean isChatAudioPlaying();

    /**
     *  设置混屏背景图片
     *  当前版本仅支持进房间前进行设置，退出房间清空，再次进房间需要重新设置。
     *
     *  @param url 背景图片所在的地址
     */
    public abstract void setVideoMixerBackgroundImgUrl(String url);

    /**
     * 设置是否启用跨房间连麦
     * 若未启用，多个主播进入同一个房间，前面的主播会被踢下线
     *
     * @param enable 是否启用
     */
    public abstract void enableCrossRoom(boolean enable);

    public abstract void enableDualVideoStream(boolean enable);

    public abstract boolean isAudience();

    // for debug
    public abstract void reportAudioRecError(int error);
}
