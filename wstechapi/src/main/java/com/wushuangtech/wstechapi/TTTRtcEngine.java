package com.wushuangtech.wstechapi;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.SurfaceView;

import com.wushuangtech.bean.ConfVideoFrame;
import com.wushuangtech.bean.ScreenRecordConfig;
import com.wushuangtech.bean.VideoCompositingLayout;
import com.wushuangtech.library.Constants;
import com.wushuangtech.library.LocalSDKConstants;
import com.wushuangtech.wstechapi.internal.TTTRtcEngineImpl;
import com.wushuangtech.wstechapi.model.PublisherConfiguration;
import com.wushuangtech.wstechapi.model.VideoCanvas;

/**
 * SDK主体接口类,执行SDK各种功能
 */
public abstract class TTTRtcEngine {

    /**
     * SDK的实例对象
     */
    private static volatile TTTRtcEngineImpl mInstance = null;

    public TTTRtcEngine() {
    }

    /**
     * 初始化SDK，在程序生命周期中只需要调用一次，即便执行destroy函数也不需要调用
     *
     * @param context 安卓程序的上下文
     * @param appId   SDK初始化需要用到的app id
     * @param handler SDK发送回调消息的接收对象
     * @return TTTRtcEngine
     * 返回SDK的实例对象
     */
    public static synchronized TTTRtcEngine create(Context context, String appId, TTTRtcEngineEventHandler handler) {
        if (mInstance == null) {
            mInstance = new TTTRtcEngineImpl(context, appId, handler);
        } else {
            mInstance.reinitialize(context, appId, handler);
        }
        return mInstance;
    }

    /**
     * 获取SDK的实例对象，必须在SDK初始化成功后方可调用。
     */
    public static TTTRtcEngine getInstance() {
        return mInstance;
    }

    /**
     * SDK的反初始化操作
     */
    public static synchronized void destroy() {
        if (mInstance != null) {
            mInstance.doDestroy();
            mInstance = null;
            System.gc();
        }
    }

    /**
     * 设置SDK发送回调消息的接收对象
     */
    public abstract void setTTTRtcEngineEventHandler(TTTRtcEngineEventHandler mTTTRtcEngineEventHandler);

    /**
     * 查询 SDK 版本号
     *
     * @return 返回 SDK 版本号字符串
     */
    public abstract String getVersion();

    /**
     * 设置频道模式
     *
     * @param profile 频道模式 {@link Constants#CHANNEL_PROFILE_COMMUNICATION}
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public abstract int setChannelProfile(int profile);

    /**
     * 创建一个用户的渲染视图
     *
     * @param context 安卓上下文
     * @return SurfaceView 渲染视图
     */
    public abstract SurfaceView CreateRendererView(Context context);

    /**
     * 加入视频通信房间，需要异步调用
     *
     * @param channelKey  频道key
     * @param channelName 频道名字
     * @param optionalUid 用户ID
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public abstract int joinChannel(String channelKey,
                                    String channelName,
                                    long optionalUid);

    /**
     * 加入视频通信房间，需要异步调用
     *
     * @param channelKey   频道key
     * @param channelName  频道名字
     * @param optionalUid  用户ID
     * @param enableChat   启用聊天
     * @param enableSignal 启用信令
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public abstract int joinChannel(String channelKey,
                                    String channelName,
                                    long optionalUid,
                                    boolean enableChat,
                                    boolean enableSignal);

    /**
     * 离开视频通信房间
     *
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public abstract int leaveChannel();

    /**
     * 启用视频模式
     *
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public abstract int enableVideo();

    /**
     * 禁用视频模式
     *
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public abstract int disableVideo();

    /**
     * 禁用/启用本地视频功能。该方法用于只看不发的视频场景。该方法不需要本地有摄像头。
     *
     * @param enabled true启用，false禁用
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public abstract int enableLocalVideo(boolean enabled);

    /**
     * 开启本地视频预览画面
     *
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public abstract int startPreview();

    /**
     * 关闭本地视频预览画面
     *
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public abstract int stopPreview();

    /**
     * 设置SDK日志输出目录，注意要保证指定的文件是存在并可写入的。
     *
     * @param mLogFilePath 应用程序必须保证指定的目录存在而且可写。
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public abstract int setLogFile(String mLogFilePath);

    /**
     * 设置SDK日志输出的过滤器
     *
     * @param filter 过滤器可分4个 {@link Constants#LOG_FILTER_INFO}
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public abstract int setLogFilter(int filter);

    /**
     * 设置本地视频显示属性
     *
     * @param local               视频显示属性对象
     * @param activityOrientation activity的显示方向
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public abstract int setupLocalVideo(VideoCanvas local, int activityOrientation);

    /**
     * 设置远端视频显示属性
     *
     * @param remote 视频显示属性对象
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public abstract int setupRemoteVideo(VideoCanvas remote);

    /**
     * 翻转本地预览摄像头
     *
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public abstract int switchCamera();

    /**
     * 设置本地视频属性
     *
     * @param profile            预设的视频质量 {@link Constants#VIDEO_PROFILE_DEFAULT}
     * @param swapWidthAndHeight 是否将视频的宽和高互调
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public abstract int setVideoProfile(int profile, boolean swapWidthAndHeight);

    /**
     * 设置扬声器的打开/关闭
     *
     * @param enabled true 打开扬声器(若已插入耳机、蓝牙等设备，也将切换到扬声器)，
     *                false 关闭扬声器，切为听筒
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public abstract int setEnableSpeakerphone(boolean enabled);

    /**
     * 获取当前扬声器的状态.
     *
     * @return true 表示扬声器处于打开状态，false表示听筒处于打开状态
     */
    public abstract boolean isSpeakerphoneEnabled();

    /**
     * 静音/取消静音。该方法用于允许/禁止往网络发送本地音频流。
     *
     * @param muted 静音/取消静音
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public abstract int muteLocalAudioStream(boolean muted);

    /**
     * 静音指定远端用户/对指定远端用户取消静音。本方法用于允许/禁止播放远端用户的音频流。
     *
     * @param uid   指定用户
     * @param muted true麦克风静音，false取消静音
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public abstract int muteRemoteAudioStream(long uid, boolean muted);

    /**
     * 暂停/继续发送本地视频流
     *
     * @param muted True: 不发送本地视频流，False: 发送本地视频流
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public abstract int muteLocalVideoStream(boolean muted);

    /**
     * 暂停指定用户的远端视频流
     *
     * @param uid   用户ID
     * @param muted True: 停止播放指定用户的视频流，False: 允许播放指定用户的视频流
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public abstract int muteRemoteVideoStream(long uid, boolean muted);

    /**
     * 该方法用于允许/禁止播放远端用户的音频流，即对所有远端用户进行静音与否。
     *
     * @param muted True麦克风静音，False取消静音
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public abstract int muteAllRemoteAudioStreams(boolean muted);

    /**
     * 暂停所有远端视频流
     *
     * @param muted True: 停止播放用户的视频流，False: 允许播放用户的视频流
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public abstract int muteAllRemoteVideoStreams(boolean muted);

    /**
     * 修改默认的语音路由
     *
     * @param defaultToSpeaker true 默认路由改为外放(扬声器)，false 默认路由改为听筒
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public abstract int setDefaultAudioRouteToSpeakerphone(boolean defaultToSpeaker);

    /**
     * 与其他主播连麦
     *
     * @param sessionId 对方房间的sessionId
     * @param userId    对方主播的userId
     */
    public abstract int linkOtherAnchor(long sessionId, long userId);

    /**
     * 结束与其他主播连麦
     *
     * @param sessionId 对方房间的sessionId
     * @param userId    对方主播的userId
     * @param devID     对方的设备ID
     */
    public abstract int unlinkOtherAnchor(long sessionId, long userId, String devID);

    /**
     * 使用该方法设定扬声器音量。
     *
     * @param volume 设定音量，最小为0，最大为255
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public abstract int setSpeakerphoneVolume(int volume);

    /**
     * 推送外部视频帧
     *
     * @param mFrame 该视频帧包含待SDK编码的视频数据。
     * @return True该帧已推送成功，False该帧已推送失败
     */
    public abstract boolean pushExternalVideoFrame(ConfVideoFrame mFrame);

    /**
     * 设置用户角色。在加入频道前，用户需要通过本方法设置观众,副播(默认)或主播模式。在加入频道后，用户可以通过本方法切换用户模式。
     *
     * @param role          直播场景里的用户角色。see {@link Constants#CLIENT_ROLE_ANCHOR}
     * @param permissionKey 连麦鉴权功能暂时没开放，传空字符串。
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public abstract int setClientRole(int role, String permissionKey);

    /**
     * 设置音频高音质选项。切勿在加入频道后再次调用本方法。若使用录屏或分享屏幕功能，则必须启用高音质。
     *
     * @param enable true启用，false禁用
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public abstract int setHighQualityAudioParameters(boolean enable);

    /**
     * 设置画中画布局
     *
     * @param layout the layout
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public abstract int setVideoCompositingLayout(VideoCompositingLayout layout);

    /**
     * 该方法允许SDK定期向应用程序反馈当前谁在说话以及说话者的音量。
     *
     * @param interval 指定音量提示的时间间隔。小于0禁用音量提示功能，单位为毫秒
     * @param smooth   平滑系数。默认可以设置为3。
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public abstract int enableAudioVolumeIndication(int interval, int smooth);

    /**
     * 开始客户端本地混音。指定本地音频文件来和麦克风采集的音频流进行混音和替换(用音频文件替换麦克风采集的音频流)，
     * 可以通过参数选择是否让对方听到本地播放的音频和指定循环播放的次数。
     *
     * @param filePath 指定需要混音的本地音频文件名和文件路径。支持以下音频格式：mp3, aac, m4a, 3gp, wav, flac。
     * @param loopback True只有本地可以听到混音或替换后的音频流，False本地和对方都可以听到混音或替换后的音频流。
     * @param replace  True音频文件内容将会替换本地录音的音频流，False音频文件内容将会和麦克风采集的音频流进行混音
     * @param cycle    指定音频文件循环播放的次数。正整数代表循环的次数
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public abstract int startAudioMixing(String filePath,
                                         boolean loopback, boolean replace, int cycle);

    /**
     * 使用该方法停止伴奏播放，请在频道内调用该方法。
     *
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public abstract int stopAudioMixing();

    /**
     * 使用该方法暂停伴奏播放。请在频道内调用该方法。
     *
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public abstract int pauseAudioMixing();

    /**
     * 使用该方法恢复混音，继续播放伴奏。请在频道内调用该方法。.
     *
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public abstract int resumeAudioMixing();

    /**
     * 使用该方法调节混音里伴奏与人声的音量比例大小。请在频道内调用该方法。
     *
     * @param volume 伴奏与人声音量比例范围为0~1。0代表无伴奏声音，1代表无人声。
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public abstract int adjustAudioMixingVolume(int volume);

    /**
     * 该方法检查该设备是否支持 texture 编码。
     *
     * @return True支持Texture编码，False不支持Texture编码
     */
    public abstract boolean isTextureEncodeSupported();

    /**
     * 该方法设置是否使用外部视频源
     *
     * @param enable     是否使用外部视频源。true使用，false不使用
     * @param useTexture 是否使用Texture作为输入。true使用，false不使用
     * @param pushMode   是否外部视频源需要调用PushVideoFrame将视频帧主动推送给SDK。
     *                   True使用推送(push)模式，False使用拉(pull) 模式(暂不支持)
     */
    public abstract void setExternalVideoSource(boolean enable, boolean useTexture, boolean pushMode);

    /**
     * 使用该方法获取当前伴奏播放进度，单位为毫秒。请在频道内调用该方法。
     *
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public abstract int getAudioMixingCurrentPosition();

    /**
     * 使用该方法获取伴奏时长，单位为毫秒。请在频道内调用该方法。
     *
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public abstract int getAudioMixingDuration();

    /**
     * 该方法用于在加入频道前为引擎创建一份推流设置。我们提供一个 Builder 类方便配置旁路直播推流，例如:
     *
     * @param config 封装配置信息的类
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public abstract int configPublisher(PublisherConfiguration config);

    //**************** SDK自行添加的新街口 *********************

    /**
     * 将某个用户请出直播房间
     *
     * @param uid 被请出的用户ID
     * @return 是否成功将用户请出
     */
    public abstract boolean kickChannelUser(long uid);

    /**
     * 将某个远端用户静音，其他所有用户都无法听到该用户的说话
     *
     * @param uid       要被静音的用户ID
     * @param isDisable true代表用户静音，false代表不静音
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public abstract int muteRemoteSpeaking(long uid, boolean isDisable);

    /**
     * 向h.264码流中插入sei内容
     *
     * @param content 字符串内容
     * @return 0代表方法调用成功
     */
    public abstract int insertH264SeiContent(String content);

    /**
     * 发送聊天信息
     *
     * @param nDstUserID 目标ID
     * @param type       消息类型
     * @param sSeqID     消息唯一标识
     * @param sData      消息内容
     */
    public abstract int sendChatMessage(long nDstUserID, int type, String sSeqID, String sData);

    /**
     * 发送信令
     *
     * @param nDstUserID 目标ID
     * @param sSeqID     消息唯一标识
     * @param sData      消息内容
     */
    public abstract int sendSignal(long nDstUserID, String sSeqID, String sData);

    /**
     * 开始采集聊天语音（限制60s以内）
     */
    public abstract int startRecordChatAudio();

    /**
     * 停止采集并且发送语音消息
     *
     * @param nDstUserID 目标ID
     * @return 语音时长
     */
    public abstract int stopRecordAndSendChatAudio(long nDstUserID, String sSeqID);

    /**
     * 取消语音采集
     */
    public abstract int cancleRecordChatAudio();

    /**
     * 播放语音消息
     *
     * @param audioPath 语音文件路径
     */
    public abstract int playChatAudio(String audioPath);

    /**
     * 停止播放语音消息
     */
    public abstract int stopChatAudio();

    /**
     * 是否正在播放语音消息
     *
     * @return true：正在播放 false：没有播放
     */
    public abstract boolean isChatAudioPlaying();

    /**
     * 语音文件内容识别
     *
     * @param path 语音文件路径
     */
    public abstract void speechRecognition(String path);

    /**
     * 是否分享当前的屏幕。
     *
     * @param isShare true分享，false不分享
     */
    public abstract void shareScreenRecorder(boolean isShare);

    /**
     * 提醒用户当前程序要录制屏幕。
     *
     * @param mActivity 当前要录屏时激活的Activity
     */
    public abstract void tryScreenRecorder(Activity mActivity);

    /**
     * 获得权限后开始录制屏幕。
     *
     * @param data    系统返回的Intent对象
     * @param mConfig 屏幕录制相关设置参数对象
     */
    public abstract boolean startScreenRecorder(Intent data, ScreenRecordConfig mConfig);

    /**
     * 停止屏幕采集。
     */
    public abstract void stopScreenRecorder();

    /**
     * 开始RTMP推流。
     *
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public abstract int startRtmpPublish(String rtmpUrl);

    /**
     * 停止RTMP推流。
     *
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public abstract int stopRtmpPublish();

    /**
     * 暂停RTMP推流。
     *
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public abstract int pauseRtmpPublish();

    /**
     * 恢复RTMP推流。
     *
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public abstract int resumeRtmpPublish();

    /**
     * 开始客户端本地混音。指定本地音频文件来和麦克风采集的音频流进行混音和替换(用音频文件替换麦克风采集的音频流)，
     * 可以通过参数选择是否让对方听到本地播放的音频和指定循环播放的次数。
     *
     * @param assertFileName 指定需要混音的本地音频文件名和文件路径。支持以下音频格式：mp3, aac, m4a, 3gp, wav, flac。
     * @param loopback       True只有本地可以听到混音或替换后的音频流，False本地和对方都可以听到混音或替换后的音频流。
     * @param replace        True音频文件内容将会替换本地录音的音频流，False音频文件内容将会和麦克风采集的音频流进行混音
     * @param cycle          指定音频文件循环播放的次数。正整数代表循环的次数
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public abstract int startAudioMixingAssert(String assertFileName,
                                               boolean loopback, boolean replace, int cycle);
    /**
     * 是否启用双流视频。
     */
    public abstract void enableDualVideoStream(boolean isEnable);

    /**
     * 是否启用跨房间连麦。
     */
    public abstract void enableCrossRoom(boolean isEnable);

    /**
     * 设置远端视频流类型。see {@link Constants#VIDEO_STEAM_TYPE_BIG}
     *
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public abstract int setVideoSteamType(long uid, int steamType);
}
