package com.wushuangtech.library;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.wushuangtech.library.Constants.AUDIO_ROUTE_SPEAKER;

/**
 * Created by wangzhiguo on 17/6/9.
 */

public class GlobalConfig {

    /**
     * SDK的版本信息
     */
    public static final String SDK_VERSION_NAME = "2.0.0 (0525)";
    /**
     * SDK初始化的状态
     */
    public static AtomicBoolean mIsInited = new AtomicBoolean();
    /**
     * 频道模式
     */
    public static int mCurrentChannelMode = Constants.CHANNEL_PROFILE_COMMUNICATION;
    /**
     * APP ID
     */
    public static String mAppID;
    /**
     * 自己的ID
     */
    public static long mLocalUserID;
    /**
     * 成功进入房间时的时间戳
     */
    public static long mStartRoomTime;
    /**
     * 是否已经进入房间的标识
     */
    public static AtomicBoolean mIsInRoom = new AtomicBoolean();
    /**
     * 是否已经进入拉流房间的标识
     */
    public static boolean mIsInPullRoom;
    /**
     * 本地视频编码和预览的模式
     */
    public static int mLocalCameraShowMode = Constants.RENDER_MODE_HIDDEN;
    /**
     * 自己是否是主播
     */
    public static int mIsLocalHost = Constants.CLIENT_ROLE_AUDIENCE;
    /**
     * 是否启用视频模式，true启用，false禁用
     */
    public static boolean mIsEnableVideoMode;
    /**
     * 是否禁用本地音频数据的发送
     */
    public static boolean mIsMuteLocalAudio;
    /**
     * 是否禁用本地视频数据的发送
     */
    public static boolean mIsMuteLocalVideo;
    /**
     * 是否是耳机输出优先，true是耳机优先，false扬声器优先
     */
    public static boolean mIsHeadsetPriority = true;
    /**
     * 当前是设置耳机输出还是扬声器输出
     */
    public static boolean mIsSpeakerphoneEnabled = true;
    /**
     * 程序默认音频路由
     */
    public static int mDefaultAudioRoute = AUDIO_ROUTE_SPEAKER;
    /**
     * 程序当前播放伴奏，该伴奏的时长
     */
    public static int mCurrentAudioMixingDuration;
    /**
     * 程序当前播放伴奏的时间进度
     */
    public static int mCurrentAudioMixingPosition;
    /**
     * 是否使用外部视频源
     */
    public static boolean mExternalVideoSource;
    /**
     * 外部视频源是否使用 Texture 作为输入
     */
    public static boolean mExternalVideoSourceIsTexture;
    /**
     * SDK默认的推流地址的前缀
     */
    public static String mCDNPushAddressPrefix = "rtmp://push.3ttech.cn/sdk/";
    /**
     * SDK默认的拉流地址的前缀
     */
    public static String mCDNPullAddressPrefix = "rtmp://pull.3ttech.cn/sdk/";
    /**
     * SDK的拉流地址
     */
    public static String mCDNAPullddress;
    /**
     * SDK的推流地址
     */
    public static String mPushUrl;
    /**
     * 屏幕录制状态，是否在录制中
     */
    public static AtomicBoolean mIsScreenRecordShare;
    //解决在进房间过程中，即还未收到进房间成功的回调，再次调用setClientRole函数会出问题
    /**
     * 登录状态，是否正在登录中
     */
    public static AtomicBoolean mIsLogining = new AtomicBoolean();
    /**
     * 视频采集编码开始的时机，用于外部视频源接口
     */
    public static AtomicBoolean mIsCapturing = new AtomicBoolean(true);
    /**
     * 聊天音频文件发送路径
     */
    public static String mChatSendPath;
    /**
     * 聊天音频文件接收路径
     */
    public static String mChatReceivePath;
    /**
     * 是否启用双流功能
     */
    public static boolean mIsEnableDual;
    /**
     * 是否启用跨房间连麦
     */
    public static boolean mIsEnableCrossRoom;
    /**
     * 服务器回调接收开关
     */
    public static boolean trunOnCallback;
    /**
     * 音频采集状态，是否禁用了音频采集
     */
    public static boolean mIsMuteAudioCapture;
    /**
     * 是否启用IJK模块
     */
    public static boolean mIsUseIjkModule = false;
    /**
     * 是否启用RTMP推流模块
     */
    public static boolean mIsUseRtmpModule = true;
    /**
     * 是否启用视频模块
     */
    public static boolean mIsUseVideoModule = true;

}
