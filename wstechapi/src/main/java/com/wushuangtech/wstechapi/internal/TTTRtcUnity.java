package com.wushuangtech.wstechapi.internal;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.SurfaceView;

import com.wushuangtech.api.EnterConfApi;
import com.wushuangtech.api.ExternalChatModule;
import com.wushuangtech.bean.ChatInfo;
import com.wushuangtech.bean.LocalVideoStats;
import com.wushuangtech.bean.RemoteVideoStats;
import com.wushuangtech.bean.RtcStats;
import com.wushuangtech.bean.TTTChatModuleContants;
import com.wushuangtech.jni.RoomJni;
import com.wushuangtech.library.Constants;
import com.wushuangtech.library.LocalSDKConstants;
import com.wushuangtech.wstechapi.TTTRtcEngine;
import com.wushuangtech.wstechapi.TTTRtcEngineEventHandler;
import com.wushuangtech.wstechapi.model.TTTLocalModuleConfig;
import com.wushuangtech.wstechapi.model.TTTModuleConstants;
import com.wushuangtech.wstechapi.model.VideoCanvas;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.LinkedList;

/**
 * SDK原生游戏接口类,执行游戏相关的SDK各种功能
 */
public final class TTTRtcUnity {

    /**
     * 主体SDK的实例对象
     */
    private TTTRtcEngine mTTTRtcEngine;

    /**
     * 游戏SDK的实例对象
     */
    private static TTTRtcUnity mInstance = null;

    private Context mContext;
    private long mLocalId;
    private int mRoomMode = Constants.CHANNEL_PROFILE_GAME_FREE_MODE;
    private LinkedList<String> mMessage = new LinkedList<>();
    private boolean isJoined = false;
    private int mLocalWidth, mLocalHeight;
    private int mVideoProfile = Constants.VIDEO_PROFILE_360P;
    private boolean mSwapWidthAndHeight = true;

    /**
     * 游戏SDK的构造函数
     *
     * @param context 安卓程序的上下文
     * @param mAppID  SDK初始化需要用到的app id
     */
    private TTTRtcUnity(Context context, String mAppID) {
        mContext = context;
        Constants.IS_UNITY = true;
        EngineHandler engineHandler = new EngineHandler();
        mTTTRtcEngine = TTTRtcEngine.create(mContext, mAppID, engineHandler);
        mTTTRtcEngine.setTTTRtcEngineEventHandler(engineHandler);
        RoomJni.getInstance().setServerAddress("39.107.64.215", 5000);
        mTTTRtcEngine.setChannelProfile(mRoomMode);
        mTTTRtcEngine.setClientRole(Constants.CLIENT_ROLE_BROADCASTER, null);
    }

    /**
     * 初始化游戏SDK，在程序生命周期中只需要调用一次，即便执行destroy函数也不需要调用
     *
     * @param mContext 安卓程序的上下文
     * @param mAppID   SDK初始化需要用到的app id
     * @return TTTRtcEngineForGamming
     * 返回游戏SDK的实例对象
     */
    public static synchronized TTTRtcUnity create(Context mContext, String mAppID) {
        if (mInstance == null) {
            synchronized (TTTRtcUnity.class) {
                if (mInstance == null) {
                    mInstance = new TTTRtcUnity(mContext, mAppID);
                }
            }
        }
        return mInstance;
    }

    /**
     * SDK的反初始化操作
     */
    public synchronized void destroy() {
    }

    /**
     * 查询 SDK 版本号
     *
     * @return 返回 SDK 版本号字符串
     */
    public String getVersion() {
        return mTTTRtcEngine.getVersion();
    }

    /**
     * 加入视频通信房间，需要异步调用
     *
     * @param channelName 频道名字
     * @param optionalUid 用户ID
     * @return 0代表方法调用成功，其他代表失败。
     */
    public synchronized int joinChannel(String channelName, int optionalUid) {
        return mTTTRtcEngine.joinChannel("", channelName, optionalUid);
    }

    /**
     * 加入视频通信房间，需要异步调用
     *
     * @param channelKey  频道key
     * @param channelName 频道名字
     * @param optionalUid 用户ID
     * @return 0代表方法调用成功，其他代表失败。
     */
    public synchronized int joinChannel(String channelKey, String channelName, int optionalUid) {
        Log.d("zhx", "joinChannel: ");
        mTTTRtcEngine.setChannelProfile(mRoomMode);
        enableChat();
        mLocalId = optionalUid;
        return mTTTRtcEngine.joinChannel(channelKey, channelName, optionalUid);
    }

    /**
     * 离开视频通信房间
     *
     * @return 0代表方法调用成功，其他代表失败。
     */
    public synchronized int leaveChannel() {
        Log.d("zhx", "leaveChannel: ");
        return this.mTTTRtcEngine.leaveChannel();
    }

    public int enableVideo() {
        Log.d("zhx", "enableVideo: ");
        mTTTRtcEngine.setVideoProfile(mVideoProfile, mSwapWidthAndHeight);
        mTTTRtcEngine.enableVideo();
        return 0;
    }

    public int disableVideo() {
        return this.mTTTRtcEngine.disableVideo();
    }

    public int enableLocalVideo(boolean enabled) {
        return this.mTTTRtcEngine.enableLocalVideo(enabled);
    }

    public int startPreview() {
        return this.mTTTRtcEngine.startPreview();
    }

    public int stopPreview() {
        return this.mTTTRtcEngine.stopPreview();
    }

    public int setEnableSpeakerphone(boolean enabled) {
        return this.mTTTRtcEngine.setEnableSpeakerphone(enabled);
    }

    public boolean isSpeakerphoneEnabled() {
        return this.mTTTRtcEngine.isSpeakerphoneEnabled();
    }

    public int setDefaultAudioRoutetoSpeakerphone(boolean defaultToSpeaker) {
        return this.mTTTRtcEngine.setDefaultAudioRouteToSpeakerphone(defaultToSpeaker);
    }

    public int enableAudioVolumeIndication(int interval, int smooth) {
        return this.mTTTRtcEngine.enableAudioVolumeIndication(interval, smooth);
    }

    public int startAudioMixing(String filePath, boolean loopback, boolean replace, int cycle) {
        return this.mTTTRtcEngine.startAudioMixing(filePath, loopback, replace, cycle);
    }

    public int stopAudioMixing() {
        return this.mTTTRtcEngine.stopAudioMixing();
    }

    public int pauseAudioMixing() {
        return this.mTTTRtcEngine.pauseAudioMixing();
    }

    public int resumeAudioMixing() {
        return this.mTTTRtcEngine.resumeAudioMixing();
    }

    public int adjustAudioMixingVolume(int volume) {
        return this.mTTTRtcEngine.adjustAudioMixingVolume(volume);
    }

    public int getAudioMixingDuration() {
        return this.mTTTRtcEngine.getAudioMixingDuration();
    }

    public int getAudioMixingCurrentPosition() {
        return this.mTTTRtcEngine.getAudioMixingCurrentPosition();
    }

    public int muteLocalAudioStream(boolean mute) {
        return this.mTTTRtcEngine.muteLocalAudioStream(mute);
    }

    public int muteAllRemoteAudioStreams(boolean mute) {
        return this.mTTTRtcEngine.muteAllRemoteAudioStreams(mute);
    }

    public int muteRemoteAudioStream(int uid, boolean mute) {
        return this.mTTTRtcEngine.muteRemoteAudioStream(uid, mute);
    }

    public int switchCamera() {
        return this.mTTTRtcEngine.switchCamera();
    }

    public int setVideoProfile(int profile, boolean swapWidthAndHeight) {
        switch (profile) {
            case 0:
                mVideoProfile = 110;
                break;
            case 10:
                mVideoProfile = 111;
                break;
            case 20:
                mVideoProfile = 112;
                break;
            case 30:
                mVideoProfile = 113;
                break;
            case 40:
                mVideoProfile = 114;
                break;
            case 50:
                mVideoProfile = 115;
                break;
            case 60:
                mVideoProfile = 116;
                break;
        }
        mSwapWidthAndHeight = swapWidthAndHeight;
        return 0;
    }

    public int muteAllRemoteVideoStreams(boolean muted) {
        return this.mTTTRtcEngine.muteAllRemoteVideoStreams(muted);
    }

    public int muteLocalVideoStream(boolean muted) {
        return this.mTTTRtcEngine.muteLocalVideoStream(muted);
    }

    public int muteRemoteVideoStream(long uid, boolean muted) {
        return this.mTTTRtcEngine.muteRemoteVideoStream(uid, muted);
    }

    /**
     * 设置频道模式
     *
     * @param profile 频道模式 {@link Constants#CHANNEL_PROFILE_COMMUNICATION}
     * @return 0代表方法调用成功，其他代表失败。see {@link LocalSDKConstants#FUNCTION_SUCCESS}
     */
    public int setChannelProfile(int profile) {
        mRoomMode = profile;
        return LocalSDKConstants.FUNCTION_SUCCESS;
    }

    public int setLogFile(String filePath) {
        return this.mTTTRtcEngine.setLogFile(filePath);
    }

    public int setLogFilter(int filter) {
        return this.mTTTRtcEngine.setLogFilter(filter);
    }

    public int getMessageCount() {
        return mMessage.size();
    }

    public String getMessage() {
        return mMessage.poll();
    }

    private class EngineHandler extends TTTRtcEngineEventHandler {

        @Override
        public void onError(int err) {
            Log.d("zhx", "onError: " + err);
            mMessage.add("onError\t" + err);
        }

        @Override
        public void onConnectionLost() {
            Log.d("zhx", "onConnectionLost: ");
            mMessage.add("onConnectionLost");
        }

        @Override
        public void onJoinChannelSuccess(String channel, long uid) {
            Log.d("zhx", "onJoinChannelSuccess channel: " + channel + "uid:" + uid + " mLocalWidth:" + mLocalWidth + " mLocalHeight:" + mLocalHeight);
            isJoined = true;
            mMessage.add("onJoinChannelSuccess\t" + channel + "\t" + uid + "\t0");

            if (mLocalWidth != 0 && mLocalHeight != 0)
                mMessage.add("onFirstLocalVideoFrame\t" + mLocalId + "\t" + mLocalWidth + "\t" + mLocalHeight + "\t" + 0);
        }

        @Override
        public void onUserJoined(long nUserId, int identity) {
            Log.d("zhx", "onUserJoined: " + nUserId);
            SurfaceView mSurfaceView = mTTTRtcEngine.CreateRendererView(mContext);
            mTTTRtcEngine.setupRemoteVideo(new VideoCanvas(nUserId, Constants.RENDER_MODE_HIDDEN, mSurfaceView));
            mMessage.add("onUserJoined\t" + nUserId + "\t" + identity);
        }

        @Override
        public void onFirstLocalVideoFrame(int width, int height) {
            Log.d("zhx", "onFirstLocalVideoFrame: " + width + " " + height);
            mLocalWidth = width;
            mLocalHeight = height;
            if (isJoined)
                mMessage.add("onFirstLocalVideoFrame\t" + mLocalId + "\t" + width + "\t" + height + "\t" + 0);
        }

        @Override
        public void onFirstRemoteVideoFrame(long uid, int width, int height) {
            Log.d("zhx", "onFirstRemoteVideoFrame: uid:" + uid + " " + width + " " + height);
            mMessage.add("onFirstRemoteVideoFrameDecoded\t" + uid + "\t" + width + "\t" + height + "\t" + 0);
        }

        @Override
        public void onUserOffline(long nUserId, int reason) {
            Log.d("zhx", "onUserOffline: ");
            mMessage.add("onUserOffline\t" + nUserId + "\t" + reason);
        }

        @Override
        public void onLocalVideoStats(LocalVideoStats stats) {
//            Log.d("zhx", "onLocalVideoStats: ");
//            mMessage.add("onLocalVideoStats");
        }

        @Override
        public void onRemoteVideoStats(RemoteVideoStats stats) {
//            Log.d("zhx", "onRemoteVideoStats: ");
//            mMessage.add("onRemoteVideoStats");
        }

        @Override
        public void onCameraReady() {
            Log.d("zhx", "onCameraReady: ");
            mMessage.add("onCameraReady");
        }

        @Override
        public void onLeaveChannel(RtcStats stats) {
            Log.d("zhx", "onLeaveChannel: ");
            isJoined = false;
//            mMessage.add("onLeaveChannel");
        }

        @Override
        public void onAudioVolumeIndication(long nUserID, int audioLevel, int audioLevelFullRange) {
//            Log.d("zhx", "onAudioVolumeIndication: ");
            mMessage.add("onReportAudioVolumeIndications\t" + nUserID + "\t" + audioLevel);
        }

        @Override
        public void onRtcStats(RtcStats stats) {
//            Log.d("zhx", "onRtcStats: ");
//            mMessage.add("onRtcStats");
        }

        @Override
        public void OnChatMessageSent(ChatInfo chatInfo, int error) {
            Log.d("zhx", "OnChatMessageSent: " + chatInfo.chatData);
            mMessage.add("onChatMessageSent\t" + chatInfo.chatType + "\t" + chatInfo.seqID + "\t" + chatInfo.chatData + "\t" + chatInfo.audioDuration + "\t" + error);
        }

        @Override
        public void OnChatMessageRecived(long nSrcUserID, ChatInfo chatInfo) {
            Log.d("zhx", "OnChatMessageRecived: " + chatInfo.chatData);
            mMessage.add("onChatMessageReceived\t" + nSrcUserID + "\t" + chatInfo.chatType + "\t" + chatInfo.seqID + "\t" + chatInfo.chatData + "\t" + chatInfo.audioDuration);
        }

        @Override
        public void onPlayChatAudioCompletion(String filePath) {
            Log.d("zhx", "onPlayChatAudioCompletion: ");
            mMessage.add("onPlayChatAudioCompletion\t" + filePath);
        }

        @Override
        public void onSpeechRecognized(String str) {
            Log.d("zhx", "onSpeechRecognized: ");
            mMessage.add("onSpeechRecognized\t" + str);
        }

        @Override
        public void onUserMuteAudio(long uid, boolean muted) {
            Log.d("zhx", "onUserMuteAudio: ");
            mMessage.add("onAudioMutedByPeer\t" + uid + "\t" + muted);
        }

        @Override
        public void onAudioRouteChanged(int routing) {
            Log.d("zhx", "onAudioRouteChanged: ");
            mMessage.add("onAudioRouteChanged\t" + routing);
        }
    }

    public void enableChat() {
        EnterConfApi.getInstance().enableChat();
        RoomJni.getInstance().setServerAddress("39.107.64.215", 5000);
    }

    public void sendChatMessage(int nDstUserID, int type, String sSeqID, String sData) {
        EnterConfApi.getInstance().sendChat(nDstUserID, type, sSeqID, sData);
    }

    public void sendSignal(long nDstUserID, String sSeqID, String sData) {
        EnterConfApi.getInstance().sendSignal(nDstUserID, sSeqID, sData);
    }

    public void startRecordChatAudio() {
        EnterConfApi.getInstance().startRecordChatAudio();
    }

    public int stopRecordAndSendChatAudio(int nDstUserID, String sSeqID) {
        Log.d("zhx", "TTTRtcUnity stopRecordAndSendChatAudio: ");
//        return EnterConfApi.getInstance().stopRecordAndSendChatAudio(nDstUserID, sSeqID);
        return EnterConfApi.getInstance().stopRecordAndSendChatAudio((long) nDstUserID, sSeqID);
    }

    public void cancleRecordChatAudio() {
        EnterConfApi.getInstance().cancleRecordChatAudio();
    }

    public void playChatAudio(String audioPath) {
        Log.d("zhx", "playChatAudio: " + audioPath);
        EnterConfApi.getInstance().playChatAudio(audioPath);
    }

    public void stopChatAudio() {
        EnterConfApi.getInstance().stopChatAudio();
    }

    public boolean isChatAudioPlaying() {
        return EnterConfApi.getInstance().isChatAudioPlaying();
    }

    public void speechRecognition(String path) {
        ExternalChatModule.getInstance().handleActionEvent(TTTChatModuleContants.ACTION_SPEECH_RECOGNITION, mContext, path);
    }

    /*--------------------------------------------------------------Unity JNI层接口---------------------------------------------------------------*/

    int i = 0;

    private byte[] getRemoteBuffer(String devId) {
        Object o = handleVideoModule(new TTTLocalModuleConfig(TTTModuleConstants.VIDEO_UNITY_REMOTE_BUFFER, new Object[]{devId}));
        if (o != null) {
            return (byte[]) o;
        }
        return null;
    }

    private byte[] getLocalBuffer() {
        Object o = handleVideoModule(new TTTLocalModuleConfig(TTTModuleConstants.VIDEO_UNITY_GET_LOCAL_BUFFER, new Object[]{}));
        if (o != null) {
            return (byte[]) o;
        }
        return null;
    }

    private int getRemoteWidth(String devId) {
        Object o = handleVideoModule(new TTTLocalModuleConfig(TTTModuleConstants.VIDEO_UNITY_GET_REMOTE_WIDTH, new Object[]{devId}));
        if (o != null) {
            return (int) o;
        }
        return 0;
    }

    private int getRemoteHeight(String devId) {
        Object o = handleVideoModule(new TTTLocalModuleConfig(TTTModuleConstants.VIDEO_UNITY_GET_REMOTE_HEIGHT, new Object[]{devId}));
        if (o != null) {
            return (int) o;
        }
        return 0;
    }

    private int getLocalWidth() {
        Object o = handleVideoModule(new TTTLocalModuleConfig(TTTModuleConstants.VIDEO_UNITY_GET_LOCAL_WIDTH, new Object[]{}));
        if (o != null) {
            return (int) o;
        }
        return 0;
    }

    private int getLocalHeight() {
        Object o = handleVideoModule(new TTTLocalModuleConfig(TTTModuleConstants.VIDEO_UNITY_GET_LOCAL_WIDTH, new Object[]{}));
        if (o != null) {
            return (int) o;
        }
        return 0;
    }

    public byte[] getDeviceBuffer(int uid) {
        return uid == mLocalId ? getLocalBuffer() : getRemoteBuffer(String.valueOf(uid));
    }

    public int getDeviceWidth(int uid) {
        return uid == mLocalId ? getLocalWidth() : getRemoteWidth(String.valueOf(uid));
    }

    public int getDeviceHeight(int uid) {
        return uid == mLocalId ? getLocalHeight() : getRemoteHeight(String.valueOf(uid));
    }
    /*--------------------------------------------------------------Unity JNI层接口---------------------------------------------------------------*/

    private Bitmap convertToBitmap(byte[] ia) {
        if (ia == null) return null;
        Log.d("zhx", "convertToBitmap: local width:" + getLocalWidth() + " height:" + getLocalHeight());
        int mWidth = getLocalWidth();
        int mHeight = getLocalHeight();
        byte[] iat = new byte[mWidth * mHeight * 4];
        for (int i = 0; i < mHeight; i++) {
            System.arraycopy(ia, i * mWidth * 4, iat, (mHeight - i - 1) * mWidth * 4, mWidth * 4);
        }
        Bitmap bitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(iat));
        File file = new File("/sdcard/PNG/unity.png");
        if (file.exists()) {
            file.delete();
        }
        try {
            FileOutputStream out = new FileOutputStream(file);
            if (bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)) {
                out.flush();
                out.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return bitmap;
    }

    public Object handleVideoModule(TTTLocalModuleConfig config) {
        //IJK_handleVideoModuleTG
        return TTTVideoModule.getInstance().receiveVideoModuleEvent(config);
    }

    public Object handleVideoModule(int eventType) {
        //IJK_handleVideoModuleTGEvent
        return TTTVideoModule.getInstance().receiveVideoModuleEvent(eventType);
    }
}
