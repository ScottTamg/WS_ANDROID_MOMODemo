package com.wushuangtech.wstechapi;

import android.content.Context;
import android.view.SurfaceView;

import com.wushuangtech.api.EnterConfApi;
import com.wushuangtech.library.Constants;
import com.wushuangtech.library.LocalSDKConstants;
import com.wushuangtech.wstechapi.model.VideoCanvas;

/**
 * SDK原生游戏接口类,执行游戏相关的SDK各种功能
 */
public final class TTTRtcEngineForGamming {

    /**
     * 游戏SDK的实例对象
     */
    private static TTTRtcEngineForGamming mInstance = null;

    /**
     * 主体SDK的实例对象
     */
    private TTTRtcEngine mTTTRtcEngine;

    /**
     * 游戏SDK的构造函数
     *
     * @param mContext 安卓程序的上下文
     * @param mAppID   SDK初始化需要用到的app id
     * @param mHandler SDK发送回调消息的接收对象
     */
    private TTTRtcEngineForGamming(Context mContext, String mAppID,
                                   TTTRtcEngineEventHandler mHandler) {
        mTTTRtcEngine = TTTRtcEngine.create(mContext, mAppID, mHandler);
    }

    /**
     * 初始化游戏SDK，在程序生命周期中只需要调用一次，即便执行destroy函数也不需要调用
     *
     * @param mContext 安卓程序的上下文
     * @param mAppID   SDK初始化需要用到的app id
     * @param mHandler SDK发送回调消息的接收对象
     * @return TTTRtcEngineForGamming
     * 返回游戏SDK的实例对象
     */
    public static synchronized TTTRtcEngineForGamming create(Context mContext, String mAppID, TTTRtcEngineEventHandler mHandler) {
        if (mInstance == null) {
            synchronized (TTTRtcEngineForGamming.class) {
                if (mInstance == null) {
                    mInstance = new TTTRtcEngineForGamming(mContext, mAppID, mHandler);
                }
            }
        }
        return mInstance;
    }

    /**
     * 获取游戏SDK的实例对象，必须在游戏SDK初始化成功后方可调用。
     */
    public static TTTRtcEngineForGamming getInstance() {
        return mInstance;
    }

    /**
     * SDK的反初始化操作
     */
    public static synchronized void destroy() {
        if (mInstance != null) {
            TTTRtcEngine.destroy();
            System.gc();
        }
    }

    /**
     * 设置游戏SDK发送回调消息的接收对象
     */
    public void setTTTRtcEngineForGammingEventHandler(TTTRtcEngineEventHandler
                                                              mTTTRtcEngineEventHandler) {
        mTTTRtcEngine.setTTTRtcEngineEventHandler(mTTTRtcEngineEventHandler);
    }

    /**
     * 设置频道模式
     *
     * @param profile 频道模式 {@link Constants#CHANNEL_PROFILE_GAME_FREE_MODE} , 该游戏接口仅支持
     *                CHANNEL_PROFILE_GAME_FREE_MODE
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public int setChannelProfile(int profile) {
        if (profile != Constants.CHANNEL_PROFILE_GAME_FREE_MODE) {
            return LocalSDKConstants.ERROR_FUNCTION_ERROR_ARGS;
        }
        return this.mTTTRtcEngine.setChannelProfile(profile);
    }

    /**
     * 设置用户角色，用户指挥官模式。在加入频道前，用户需要通过本方法设置被指挥者(默认)或指挥者角色式。在加入频道后，用户可以通过本方法切换用户模式。
     *
     * @param role          直播场景里的用户角色。see {@link Constants#CLIENT_ROLE_ANCHOR}
     * @param permissionKey 连麦鉴权功能暂时没开放，将其设置为空。
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public int setClientRole(int role, String permissionKey) {
        return this.mTTTRtcEngine.setClientRole(role, permissionKey);
    }

    /**
     * 加入游戏房间，需要异步调用
     *
     * @param channelName 频道名字
     * @param optionalUid 用户ID
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public synchronized int joinChannel(String channelName, long optionalUid) {
        return this.mTTTRtcEngine.joinChannel("", channelName, optionalUid);
    }

    /**
     * 加入游戏房间，需要异步调用
     *
     * @param channelKey  频道key
     * @param channelName 频道名字
     * @param optionalUid 用户ID
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public synchronized int joinChannel(String channelKey, String channelName, long optionalUid,
                                        boolean enableChat, boolean enableSignal) {
        return this.mTTTRtcEngine.joinChannel(channelKey, channelName, optionalUid, enableChat, enableSignal);
    }

    /**
     * 加入游戏房间，需要异步调用
     *
     * @param channelKey  频道key
     * @param channelName 频道名字
     * @param optionalUid 用户ID
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public synchronized int joinChannel(String channelKey, String channelName, long optionalUid) {
        return this.mTTTRtcEngine.joinChannel(channelKey, channelName, optionalUid);
    }

    /**
     * 离开视频通信房间
     *
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public synchronized int leaveChannel() {
        return this.mTTTRtcEngine.leaveChannel();
    }

    /**
     * 设置SDK日志输出目录，注意要保证指定的文件是存在并可写入的。
     *
     * @param mLogFilePath 应用程序必须保证指定的目录存在而且可写。
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public int setLogFile(String mLogFilePath) {
        return this.mTTTRtcEngine.setLogFile(mLogFilePath);
    }

    /**
     * 设置SDK日志输出的过滤器
     *
     * @param filter 过滤器可分4个 {@link Constants#LOG_FILTER_INFO}
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public int setLogFilter(int filter) {
        return this.mTTTRtcEngine.setLogFilter(filter);
    }

    /**
     * 修改默认的语音路由
     *
     * @param defaultToSpeaker true 默认路由改为外放(扬声器)，false 默认路由改为听筒
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public int setDefaultAudioRoutetoSpeakerphone(boolean defaultToSpeaker) {
        return this.mTTTRtcEngine.setDefaultAudioRouteToSpeakerphone(defaultToSpeaker);
    }

    /**
     * 获取当前扬声器的状态，检查外放是否已打开
     *
     * @return true 表示扬声器处于打开状态，false表示听筒处于打开状态
     */
    public boolean isSpeakerphoneEnabled() {
        return this.mTTTRtcEngine.isSpeakerphoneEnabled();
    }

    /**
     * 使用该方法设定扬声器音量。
     *
     * @param volume 设定音量，最小为0，最大为255
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public int setSpeakerphoneVolume(int volume) {
        return this.mTTTRtcEngine.setSpeakerphoneVolume(volume);
    }

    /**
     * 该方法允许SDK定期向应用程序反馈当前谁在说话以及说话者的音量。
     *
     * @param interval 指定音量提示的时间间隔。小于0禁用音量提示功能，单位为毫秒
     * @param smooth   平滑系数。默认可以设置为3。
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public int enableAudioVolumeIndication(int interval, int smooth) {
        return this.mTTTRtcEngine.enableAudioVolumeIndication(interval, smooth);
    }

    /**
     * 静音/取消静音。该方法用于允许/禁止往网络发送本地音频流。
     *
     * @param muted 静音/取消静音
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public int muteLocalAudioStream(boolean muted) {
        return this.mTTTRtcEngine.muteLocalAudioStream(muted);
    }

    /**
     * 该方法用于允许/禁止播放远端用户的音频流，即对所有远端用户进行静音与否。
     *
     * @param muted True麦克风静音，False取消静音
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public int muteAllRemoteAudioStreams(boolean muted) {
        return this.mTTTRtcEngine.muteAllRemoteAudioStreams(muted);
    }

    /**
     * 静音指定远端用户/对指定远端用户取消静音。本方法用于允许/禁止播放远端用户的音频流。
     *
     * @param uid   指定用户
     * @param muted true麦克风静音，false取消静音
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public int muteRemoteAudioStream(long uid, boolean muted) {
        return this.mTTTRtcEngine.muteRemoteAudioStream(uid, muted);
    }

    /**
     * 暂停指定用户的远端视频流
     *
     * @param uid   用户ID
     * @param muted True: 停止播放指定用户的视频流，False: 允许播放指定用户的视频流
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public int muteRemoteVideoStream(long uid, boolean muted) {
        return this.mTTTRtcEngine.muteRemoteVideoStream(uid, muted);
    }

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
    public int startAudioMixing(String filePath, boolean loopback, boolean replace, int cycle) {
        return this.mTTTRtcEngine.startAudioMixing(filePath, loopback, replace, cycle);
    }

    /**
     * 使用该方法停止伴奏播放，请在频道内调用该方法。
     *
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public int stopAudioMixing() {
        return this.mTTTRtcEngine.stopAudioMixing();
    }

    /**
     * 使用该方法暂停伴奏播放。请在频道内调用该方法。
     *
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public int pauseAudioMixing() {
        return this.mTTTRtcEngine.pauseAudioMixing();
    }

    /**
     * 使用该方法恢复混音，继续播放伴奏。请在频道内调用该方法。.
     *
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public int resumeAudioMixing() {
        return this.mTTTRtcEngine.resumeAudioMixing();
    }

    /**
     * 使用该方法调节混音里伴奏的音量大小。请在频道内调用该方法。(暂未实现)
     *
     * @param volume 伴奏音量范围为0~100(取整数)。默认100为原始文件音量。
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public int adjustAudioMixingVolume(int volume) {
        return this.mTTTRtcEngine.adjustAudioMixingVolume(volume);
    }

    /**
     * 使用该方法获取伴奏时长，单位为毫秒。请在频道内调用该方法。
     *
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public int getAudioMixingDuration() {
        return this.mTTTRtcEngine.getAudioMixingDuration();
    }

    /**
     * 使用该方法获取当前伴奏播放进度，单位为毫秒。请在频道内调用该方法。
     *
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public int getAudioMixingCurrentPosition() {
        return this.mTTTRtcEngine.getAudioMixingCurrentPosition();
    }

    /**
     * 启用视频模式
     *
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public int enableVideo() {
        return this.mTTTRtcEngine.enableVideo();
    }

    /**
     * 禁用视频模式
     *
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public int disableVideo() {
        return this.mTTTRtcEngine.disableVideo();
    }

    /**
     * 查询 SDK 版本号
     *
     * @return 返回 SDK 版本号字符串
     */
    public String getVersion() {
        return mTTTRtcEngine.getVersion();
    }

    //**************** 根据声网添加但未实现的接口 *********************

    //**************** SDK自行添加的新街口 *********************

    /**
     * 将某个用户请出直播房间
     *
     * @param uid 被请出的用户ID
     * @return 是否成功将用户请出
     */
    public boolean kickChannelUser(long uid) {
        return this.mTTTRtcEngine.kickChannelUser(uid);
    }

    /**
     * 创建一个用户的渲染视图
     *
     * @param context 安卓上下文
     * @return SurfaceView 渲染视图
     */
    public SurfaceView CreateRendererView(Context context) {
        return this.mTTTRtcEngine.CreateRendererView(context);
    }

    /**
     * 设置本地视频显示属性
     *
     * @param local               视频显示属性对象
     * @param activityOrientation activity的显示方向
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public int setupLocalVideo(VideoCanvas local, int activityOrientation) {
        return this.mTTTRtcEngine.setupLocalVideo(local, activityOrientation);
    }

    /**
     * 设置远端视频显示属性
     *
     * @param remote 视频显示属性对象
     * @return 0代表方法调用成功，其他代表失败。
     */
    public int setupRemoteVideo(VideoCanvas remote) {
        return this.mTTTRtcEngine.setupRemoteVideo(remote);
    }

    /**
     * 暂停/继续发送本地视频流
     *
     * @param muted True: 不发送本地视频流，False: 发送本地视频流
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public int muteLocalVideoStream(boolean muted) {
        return this.mTTTRtcEngine.muteLocalVideoStream(muted);
    }

    /**
     * 设置扬声器的打开/关闭
     *
     * @param enabled true 打开扬声器(若已插入耳机、蓝牙等设备，也将切换到扬声器)，
     *                false 关闭扬声器，切为听筒
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public int setEnableSpeakerphone(boolean enabled) {
        return this.mTTTRtcEngine.setEnableSpeakerphone(enabled);
    }

    /**
     * 设置本地视频属性
     *
     * @param profile            预设的视频质量 {@link Constants#VIDEO_PROFILE_DEFAULT}
     * @param swapWidthAndHeight 是否将视频的宽和高互调
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public int setVideoProfile(int profile, boolean swapWidthAndHeight) {
        return this.mTTTRtcEngine.setVideoProfile(profile, swapWidthAndHeight);
    }

    public int sendChatMessage(long nDstUserID, int type, String sSeqID, String sData) {
        return this.mTTTRtcEngine.sendChatMessage(nDstUserID, type, sSeqID, sData);
    }

    public int sendSignal(long nDstUserID, String sSeqID, String sData) {
        return this.mTTTRtcEngine.sendSignal(nDstUserID, sSeqID, sData);
    }

    public int startRecordChatAudio() {
        return this.mTTTRtcEngine.startRecordChatAudio();
    }

    public int stopRecordAndSendChatAudio(long nDstUserID, String sSeqID) {
        return this.mTTTRtcEngine.stopRecordAndSendChatAudio(nDstUserID, sSeqID);
    }

    public int cancleRecordChatAudio() {
        return this.mTTTRtcEngine.cancleRecordChatAudio();
    }

    public int playChatAudio(String audioPath) {
        return this.mTTTRtcEngine.playChatAudio(audioPath);
    }

    public int stopChatAudio() {
        return this.mTTTRtcEngine.stopChatAudio();
    }

    public boolean isChatAudioPlaying() {
        return EnterConfApi.getInstance().isChatAudioPlaying();
    }

    public void speechRecognition(String path) {
        this.mTTTRtcEngine.speechRecognition(path);
    }
}
