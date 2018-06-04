package com.wushuangtech.wstechapi.internal;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.projection.MediaProjection;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceView;

import com.wushuangtech.api.EnterConfApi;
import com.wushuangtech.api.EnterConfApiImpl;
import com.wushuangtech.api.ExternalAudioModule;
import com.wushuangtech.api.ExternalChatModule;
import com.wushuangtech.api.ExternalVideoModule;
import com.wushuangtech.api.JniWorkerThread;
import com.wushuangtech.api.LogWorkerThread;
import com.wushuangtech.audiocore.MyAudioApi;
import com.wushuangtech.bean.ConfVideoFrame;
import com.wushuangtech.bean.RtcStats;
import com.wushuangtech.bean.ScreenRecordConfig;
import com.wushuangtech.bean.TTTChatModuleContants;
import com.wushuangtech.bean.TTTVideoCanvas;
import com.wushuangtech.bean.VideoCompositingLayout;
import com.wushuangtech.inter.TTTEnterConfCallBack;
import com.wushuangtech.inter.TTTInterfaceTestCallBack;
import com.wushuangtech.jni.RoomJni;
import com.wushuangtech.jni.VideoJni;
import com.wushuangtech.library.Constants;
import com.wushuangtech.library.GlobalConfig;
import com.wushuangtech.library.GlobalHolder;
import com.wushuangtech.library.User;
import com.wushuangtech.library.UserDeviceConfig;
import com.wushuangtech.library.screenrecorder.EncoderConfig;
import com.wushuangtech.library.screenrecorder.RecordCallback;
import com.wushuangtech.library.screenrecorder.ScreenCapture;
import com.wushuangtech.utils.PviewLog;
import com.wushuangtech.videocore.LocaSurfaceView;
import com.wushuangtech.videocore.MyVideoApi;
import com.wushuangtech.wstechapi.TTTRtcEngine;
import com.wushuangtech.wstechapi.TTTRtcEngineEventHandler;
import com.wushuangtech.wstechapi.model.PublisherConfiguration;
import com.wushuangtech.wstechapi.model.TTTIjkModuleConstants;
import com.wushuangtech.wstechapi.model.TTTLocalModuleConfig;
import com.wushuangtech.wstechapi.model.TTTModuleConstants;
import com.wushuangtech.wstechapi.model.TTTRtmpModuleConstants;
import com.wushuangtech.wstechapi.model.VideoCanvas;
import com.wushuangtech.wstechapi.utils.AssertUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import project.android.imageprocessing.entity.Effect;

import static com.wushuangtech.api.EnterConfApi.RoomMode;
import static com.wushuangtech.library.Constants.CHANNEL_PROFILE_COMMUNICATION;
import static com.wushuangtech.library.Constants.CHANNEL_PROFILE_GAME_FREE_MODE;
import static com.wushuangtech.library.Constants.CHANNEL_PROFILE_LIVE_BROADCASTING;
import static com.wushuangtech.library.Constants.CLIENT_ROLE_BROADCASTER;
import static com.wushuangtech.library.Constants.CLIENT_ROLE_AUDIENCE;
import static com.wushuangtech.library.Constants.CLIENT_ROLE_ANCHOR;
import static com.wushuangtech.library.Constants.LOG_FILTER_DEBUG;
import static com.wushuangtech.library.Constants.LOG_FILTER_ERROR;
import static com.wushuangtech.library.Constants.LOG_FILTER_INFO;
import static com.wushuangtech.library.Constants.LOG_FILTER_OFF;
import static com.wushuangtech.library.Constants.LOG_FILTER_WARNING;
import static com.wushuangtech.library.GlobalConfig.mCDNPullAddressPrefix;
import static com.wushuangtech.library.GlobalConfig.mCurrentChannelMode;
import static com.wushuangtech.library.GlobalConfig.mExternalVideoSource;
import static com.wushuangtech.library.GlobalConfig.mExternalVideoSourceIsTexture;
import static com.wushuangtech.library.GlobalConfig.mIsEnableVideoMode;
import static com.wushuangtech.library.GlobalConfig.mIsInPullRoom;
import static com.wushuangtech.library.GlobalConfig.mIsInRoom;
import static com.wushuangtech.library.GlobalConfig.mIsSpeakerphoneEnabled;
import static com.wushuangtech.library.GlobalConfig.mLocalUserID;
import static com.wushuangtech.library.LocalSDKConstants.AUDIO_MIX_PLAY_FAILED;
import static com.wushuangtech.library.LocalSDKConstants.ERROR_FUNCTION_ERROR_ARGS;
import static com.wushuangtech.library.LocalSDKConstants.ERROR_FUNCTION_ERROR_EMPTY_VALUE;
import static com.wushuangtech.library.LocalSDKConstants.ERROR_FUNCTION_INVOKE_ERROR;
import static com.wushuangtech.library.LocalSDKConstants.ERROR_FUNCTION_STATED;
import static com.wushuangtech.library.LocalSDKConstants.FUNCTION_SUCCESS;
import static com.wushuangtech.library.LocalSDKConstants.LOG_CREATE_ERROR_NO_FILE;
import static com.wushuangtech.library.LocalSDKConstants.LOG_CREATE_ERROR_UNKNOW;
import static com.wushuangtech.utils.PviewLog.TAG;

/**
 * Created by wangzhiguo on 17/6/7.
 */
public class TTTRtcEngineImpl extends TTTRtcEngine implements TTTInterfaceTestCallBack {

    private int mLogFilterLevel = LOG_FILTER_OFF;
    private WorkerThread mWorkerThread;
    private JniWorkerThread mJniWorkerThread;
    private LogWorkerThread mLogWorkerThread;

    private Process mLogProcess;
    private ScreenCapture mScreenCapture;
    private Lock mRecorderLock = new ReentrantLock();
    private static final String[] H264_HW_BLACKLIST = {"SAMSUNG-SGH-I337", "Nexus 7", "Nexus 4",
            "P6-C00", "HM 2A", "XT105", "XT109", "XT1060"};

    private String mCacheLogPath;
    private long mCurrentLoginUserID;
    private String mCurrentLoginChanelKey;
    private String mCurrentLoginChannelName;
    private long mCurrentLoginChannel;
    private AudioManager mAudioManager;

    public TTTRtcEngineImpl(Context mContext, String mAppID,
                            TTTRtcEngineEventHandler mEnterConfApiCallbackHandler) {
        super();
        GlobalConfig.mIsInited.set(true);
        GlobalConfig.mAppID = mAppID;
        mAudioManager = (AudioManager) mContext.getSystemService(
                Context.AUDIO_SERVICE);

        EnterConfApi enterConfApi = EnterConfApi.getInstance();
        enterConfApi.setup(mAppID, mContext, true, 5);
        //音频模块初始化
        ExternalAudioModule externalAudioModule = ExternalAudioModule.getInstance();
        MyAudioApi audioApi = MyAudioApi.getInstance(mContext);
        externalAudioModule.setExternalAudioModuleCallback(audioApi);
        audioApi.addAudioSender(externalAudioModule);
        //视频模块初始化
        handleVideoModule(TTTModuleConstants.VIDEO_INIT);
        //初始化美颜模块
        MyVideoApi.getInstance().initFUBeautify(mContext);
        //聊天模块初始化
        handleChatModule(new TTTLocalModuleConfig(TTTChatModuleContants.ACTION_ALIOSS_INIT, new Object[]{mContext}));
        //RTMP模块初始化
        handleRTMPModule(new TTTLocalModuleConfig(TTTRtmpModuleConstants.RTMP_INIT, new Object[]{}));
        //IJKPlayer模块初始化
//        handleIJKModule(new TTTLocalModuleConfig(TTTIjkModuleConstants.IJK_INIT,
//                new Object[]{LocalSDKConstants.IJK_INIT_VIDEO_PLAYER, mContext}));

        initWorkerThread(mContext);
        GlobalConfig.mIsScreenRecordShare = new AtomicBoolean();
        if (mEnterConfApiCallbackHandler != null) {
            setTTTRtcEngineEventHandler(mEnterConfApiCallbackHandler);
        }

        EnterConfApiImpl.getInstance().setTTTEnterConfCallBack(new TTTEnterConfCallBack() {
            @Override
            public void setClientRole() {
                TTTRtcEngine.getInstance().setClientRole(GlobalConfig.mIsLocalHost, "");
            }

            @Override
            public void openUserDevice(long uid, String deviceID) {
                GlobalHolder instance = GlobalHolder.getInstance();
                if (instance != null) {
                    List<TTTVideoCanvas> waitOpenDevices = GlobalHolder.getInstance().getWaitOpenDevices();
                    TTTVideoCanvas mTTTVideoCanvas = null;
                    if (waitOpenDevices.size() > 0) {
                        for (int i = 0; i < waitOpenDevices.size(); i++) {
                            TTTVideoCanvas canvas = waitOpenDevices.get(i);
                            if (uid == canvas.getUserID()) {
                                PviewLog.d("setupRemoteVideo openUserDevice mUserID ID : " + uid
                                        + " | mDeviceID : " + deviceID + " | waitOpenDevices size : " + waitOpenDevices.size()
                                        + " | View : " + canvas.getSurface());
                                handleVideoModule(new TTTLocalModuleConfig(TTTModuleConstants.VIDEO_OPEN_REMOTE, new Object[]{
                                        canvas.getSurface(), canvas.getUserID(), deviceID, canvas.getShowMode()}));
                                mTTTVideoCanvas = canvas;
                                break;
                            }
                        }
                    }

                    if (mTTTVideoCanvas != null) {
                        waitOpenDevices.remove(mTTTVideoCanvas);
                    }
                }
            }
        });

        //测试接口的调用时间
        GlobalHolder instance = GlobalHolder.getInstance();
        if (instance != null) {
            GlobalHolder.getInstance().setTTTInterfaceTestCallBack(this);
        }
    }

    /**
     * Author: wangzg <br/>
     * Time: 2017-6-7 17:26:20<br/>
     * Description: 重新初始化SDK
     *
     * @param mContext                     安卓上下文
     * @param mAppID                       程序的唯一标识
     * @param mEnterConfApiCallbackHandler 回调接收者
     */
    public void reinitialize(Context mContext, String mAppID,
                             TTTRtcEngineEventHandler mEnterConfApiCallbackHandler) {
        GlobalConfig.mAppID = mAppID;
        EnterConfApi.getInstance().setAppID(mAppID);
        setTTTRtcEngineEventHandler(mEnterConfApiCallbackHandler);
//        if (GlobalConfig.mIsInited.get()) {
//            return;
//        }
//        handleVideoModule(new TTTLocalModuleConfig(TTTModuleConstants.VIDEO_SENDER_ADJUST, new Object[]{false}));
//        MyAudioApi.getInstance(mWorkerThread.getContext())
//                .addAudioSender(ExternalAudioModule.getInstance());
//        initWorkerThread(mContext);
//        GlobalConfig.mIsInited.set(true);
    }

    public void doDestroy() {
//        if (!GlobalConfig.mIsInited.get()) {
//            return;
//        }

//        GlobalConfig.mIsInited.set(false);
//        if (mLogProcess != null) {
//            mLogProcess.destroy();
//            mLogProcess = null;
//        }
//
//        if (mScreenCapture != null) {
//            mScreenCapture.stopProjection();
//            mScreenCapture = null;
//        }
//
//        EnterConfApi enterConfApi = EnterConfApi.getInstance();
//        if (GlobalConfig.mIsInRoom.get()) {
//            EnterConfApiImpl.getInstance().exitRoom();
//        }
//        enterConfApi.teardown();
//
//        MyAudioApi.getInstance(mWorkerThread.getContext().getApplicationContext()).removeAudioSender(ExternalAudioModule.getInstance());
//        handleVideoModule(new TTTLocalModuleConfig(TTTModuleConstants.VIDEO_SENDER_ADJUST, new Object[]{true}));
//        MyAudioApi.getInstance(mWorkerThread.getContext()).stopAudioFileMixing();

//        ExternalAudioModule.getInstance().unInitialize();
//        ExternalVideoModule.getInstance().uninitialize();
//        RoomJni.getInstance().unInitialize();
//        VideoJni.getInstance().unInitialize();
//        ReportLogJni.getInstance().unInitialize();
//        deInitWorkerThread();
    }

    private void initWorkerThread(Context mContext) {
        if (mWorkerThread == null) {
            mWorkerThread = new WorkerThread();
            mWorkerThread.setContext(mContext);
            mWorkerThread.setTTTRtcEngine(this);
            mWorkerThread.start();
            mWorkerThread.waitForReady();
        }

        if (mJniWorkerThread == null) {
            mJniWorkerThread = new JniWorkerThread();
            mJniWorkerThread.start();
            mJniWorkerThread.waitForReady();
            GlobalHolder instance = GlobalHolder.getInstance();
            if (instance != null) {
                GlobalHolder.getInstance().setWorkerThread(mJniWorkerThread);
            }
        }

        if (mLogWorkerThread == null) {
            mLogWorkerThread = new LogWorkerThread();
            mLogWorkerThread.start();
            mLogWorkerThread.waitForReady();
        }
    }

    private synchronized void deInitWorkerThread() {
        if (mWorkerThread == null) {
            return;
        }
        mWorkerThread.exit();
        try {
            mWorkerThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mWorkerThread = null;

        if (mJniWorkerThread == null) {
            return;
        }
        mJniWorkerThread.exit();
        try {
            mJniWorkerThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mJniWorkerThread = null;

        if (mLogWorkerThread == null) {
            return;
        }
        mLogWorkerThread.exit();
        try {
            mLogWorkerThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mLogWorkerThread = null;
    }

    synchronized Object handleVideoModule(TTTLocalModuleConfig config) {
        //IJK_handleVideoModuleTG
        return TTTVideoModule.getInstance().receiveVideoModuleEvent(config);
    }

    synchronized Object handleVideoModule(int eventType) {
        //IJK_handleVideoModuleTGEvent
        return TTTVideoModule.getInstance().receiveVideoModuleEvent(eventType);
    }

    Object handleRTMPModule(TTTLocalModuleConfig eventType) {
        if (!GlobalConfig.mIsUseRtmpModule) {
            return null;
        }

        if (eventType.eventType == TTTRtmpModuleConstants.RTMP_INIT) {
            handleVideoModule(new TTTLocalModuleConfig(TTTModuleConstants.VIDEO_RTMP_INIT, new Object[]{}));
        }
        return TTTRtmpModule.getInstance().receiveRtmpModuleEvent(eventType);
    }

    Object handleIJKModule(TTTLocalModuleConfig config) {
//        return TTTRtcIjkModule.getInstance().receiveVideoModuleEvent(config);
        return null;
    }

    Object handleChatModule(TTTLocalModuleConfig config) {
        return ExternalChatModule.getInstance().handleActionEvent(config.eventType, config.objs);
    }

    @Override
    public void setTTTRtcEngineEventHandler(TTTRtcEngineEventHandler mGSWSEngineEventHandler) {
        GlobalHolder.getInstance().setCommunicationHelper(mGSWSEngineEventHandler);
    }

    @Override
    public SurfaceView CreateRendererView(Context context) {
        Object obj = handleVideoModule(new TTTLocalModuleConfig(TTTModuleConstants.VIDEO_CREATE_VIEW, new Object[]{context}));
        if (obj != null) {
            return (SurfaceView) obj;
        }
        return null;
    }

    private boolean compareString(String s1, String s2) {
        return s1 == null && s2 == null || s1 != null && s1.equals(s2);
    }

    @Override
    public int joinChannel(String channelKey,
                           String channelName,
                           long optionalUid) {
        PviewLog.i(TAG, "joinRealChannel setup first channelKey : " + channelKey + " | channelName : " + channelName
                + " | optionalUid : " + optionalUid);
        PviewLog.i(TAG, "joinRealChannel setup first mCurrentLoginChanelKey : " + mCurrentLoginChanelKey + " | mCurrentLoginChannelName : " + mCurrentLoginChannelName
                + " | mCurrentLoginUserID : " + mCurrentLoginUserID);
        if (GlobalConfig.mIsLogining.get()
                && compareString(mCurrentLoginChanelKey, channelKey)
                && compareString(mCurrentLoginChannelName, channelName)
                && optionalUid == mCurrentLoginUserID) {
            PviewLog.i(TAG, "joinRealChannel error same opt!");
            return ERROR_FUNCTION_STATED;
        }

        mCurrentLoginChanelKey = channelKey;
        mCurrentLoginChannelName = channelName;
        mCurrentLoginUserID = optionalUid;
        // 全民修改：同步进退房间的操作，这样快速切换房间没问题
        GlobalConfig.mIsLogining.set(true);
        return mWorkerThread.joinChannel(channelKey, channelName, optionalUid);
    }

    @Override
    public int joinChannel(String channelKey,
                           String channelName,
                           long optionalUid,
                           boolean enableChat,
                           boolean enableSignal) {

        if (enableChat)
            EnterConfApi.getInstance().enableChat();
        if (enableSignal)
            EnterConfApi.getInstance().enableSignal();

        return joinChannel(channelKey, channelName, optionalUid);
    }

    int joinRealChannel(String channelKey,
                        String channelName,
                        long optionalUid) {
        PviewLog.i(TAG, "joinRealChannel mIsInPullRoom : " + mIsInPullRoom + " | mIsInRoom : " + mIsInRoom.get()
                + " | mCurrentLoginChannel : " + mCurrentLoginChannel);

        if (mCurrentChannelMode != CHANNEL_PROFILE_COMMUNICATION
                && mCurrentChannelMode != CHANNEL_PROFILE_LIVE_BROADCASTING
                && mCurrentChannelMode != CHANNEL_PROFILE_GAME_FREE_MODE) {
            return ERROR_FUNCTION_INVOKE_ERROR;
        }

        if (!TextUtils.isDigitsOnly(channelName) && !TextUtils.isEmpty(channelName)) {
            GlobalHolder.getInstance().notifyCHChannelNameError();
            return ERROR_FUNCTION_ERROR_EMPTY_VALUE;
        }

        mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);

        if (mIsInRoom.get()) {
            return ERROR_FUNCTION_STATED;
        }

        PviewLog.i(TAG, "joinRealChannel mCurrentLoginChannel : " + mCurrentLoginChannel);
        if (mCurrentLoginChannel != 0) {
            RoomJni.getInstance().RoomExit(mCurrentLoginChannel);
            mCurrentLoginChannel = 0;
        }
        mCurrentLoginChannel = Long.valueOf(channelName);

        mWorkerThread.checkHeadsetListener();
        GlobalConfig.mLocalUserID = optionalUid;
        boolean result;
        String mCdnText;
        mCdnText = mCDNPullAddressPrefix + channelName;
        String mPushUrl = GlobalConfig.mPushUrl;
        PviewLog.i(TAG, "joinRealChannel mPushUrl : " + mPushUrl);
        PviewLog.i(TAG, "joinRealChannel mIsEnableVideoMode : " + GlobalConfig.mIsEnableVideoMode);
        if (TextUtils.isEmpty(mPushUrl)) {
            mPushUrl = GlobalConfig.mCDNPushAddressPrefix + channelName;
        }

        if (GlobalConfig.mCurrentChannelMode != Constants.CHANNEL_PROFILE_LIVE_BROADCASTING
                || (GlobalConfig.mCurrentChannelMode == Constants.CHANNEL_PROFILE_LIVE_BROADCASTING && GlobalConfig.mIsLocalHost != Constants.CLIENT_ROLE_ANCHOR)) {
            mPushUrl = "";
        }

        // 纯音频模式下才开始低混流视频模式。
        if (!GlobalConfig.mIsEnableVideoMode) {
            RoomJni.getInstance().SetRoomLowerVideoMixer(true);
        }

        PviewLog.i(TAG, "joinRealChannel finally mPushUrl : " + mPushUrl);
        if (TextUtils.isEmpty(channelKey)) {
            if (GlobalConfig.mIsEnableVideoMode) {
                result = EnterConfApi.getInstance().enterRoom
                        ("", optionalUid, Long.valueOf(channelName), GlobalConfig.mIsLocalHost,
                                mPushUrl);
            } else {
                result = EnterConfApi.getInstance().enterAudioRoom
                        ("", optionalUid, Long.valueOf(channelName), GlobalConfig.mIsLocalHost,
                                mPushUrl);
            }
        } else {
            if (GlobalConfig.mIsEnableVideoMode) {
                result = EnterConfApi.getInstance().enterRoom
                        (channelKey, optionalUid, Long.valueOf(channelName), GlobalConfig.mIsLocalHost,
                                mPushUrl);
            } else {
                result = EnterConfApi.getInstance().enterAudioRoom
                        (channelKey, optionalUid, Long.valueOf(channelName), GlobalConfig.mIsLocalHost,
                                mPushUrl);
            }
        }
        mJniWorkerThread.clearDelayMessages();
        if (result) {
            GlobalConfig.mCDNAPullddress = mCdnText;
            return FUNCTION_SUCCESS;
        } else {
            return ERROR_FUNCTION_INVOKE_ERROR;
        }
    }

    @Override
    public int leaveChannel() {
        if (mIsInPullRoom) {
            handleIJKModule(new TTTLocalModuleConfig(TTTIjkModuleConstants.IJK_LEAVE_CHANNEL,
                    new Object[]{}));
        }

        return mWorkerThread.leaveChannel();
    }

    int leaveRealChannel() {
        PviewLog.i(TAG, "leaveChannel mIsInPullRoom : " + mIsInPullRoom + " | mIsInRoom : " + mIsInRoom.get()
                + " | mCurrentLoginChannel : " + mCurrentLoginChannel);
        mAudioManager.setMode(AudioManager.MODE_RINGTONE);

        if (!mIsInRoom.get()) {
            if (mCurrentLoginChannel != 0) {
                EnterConfApiImpl.getInstance().setCurrentRoomUUID("");
                specificLeaveChannel();
                mCurrentLoginChannel = 0;
            }
            return ERROR_FUNCTION_STATED;
        }
        EnterConfApiImpl.getInstance().exitRooms();
        mCurrentLoginChannel = 0;
        mWorkerThread.reset();
        return FUNCTION_SUCCESS;
    }

    private void specificLeaveChannel() {
        RtcStats rtcStats = GlobalHolder.getInstance().getRtcStats();
        JniWorkerThread mJniWorkerThread = GlobalHolder.getInstance().getWorkerThread();
        mJniWorkerThread.sendMessage(JniWorkerThread.JNI_CALL_BACK_LEAVE_CHANNEL, new Object[]{rtcStats});
        RoomJni.getInstance().RoomExit(mCurrentLoginChannel);
    }

    @Override
    public int enableVideo() {
        if (mIsEnableVideoMode) {
            return ERROR_FUNCTION_STATED;
        }
        mIsEnableVideoMode = true;

        handleVideoModule(new TTTLocalModuleConfig(TTTModuleConstants.VIDEO_PREVIEW_ADJUST, new Object[]{true}));
        handleVideoModule(new TTTLocalModuleConfig(TTTModuleConstants.VIDEO_CAPTURE_ADJUST, new Object[]{true}));
        if (GlobalConfig.mIsInRoom.get()) {
            EnterConfApi.getInstance().muteLocalVideo(false);
            handleVideoModule(new TTTLocalModuleConfig(TTTModuleConstants.VIDEO_REMOTE_VIDEO_ADJUST, new Object[]{true}));
        }
        return FUNCTION_SUCCESS;
    }

    @Override
    public int disableVideo() {
        if (!mIsEnableVideoMode) {
            return ERROR_FUNCTION_STATED;
        }

        handleVideoModule(new TTTLocalModuleConfig(TTTModuleConstants.VIDEO_PREVIEW_ADJUST, new Object[]{false}));
        handleVideoModule(new TTTLocalModuleConfig(TTTModuleConstants.VIDEO_CAPTURE_ADJUST, new Object[]{false}));
        if (GlobalConfig.mIsInRoom.get()) {
            EnterConfApi.getInstance().muteLocalVideo(true);
            handleVideoModule(new TTTLocalModuleConfig(TTTModuleConstants.VIDEO_REMOTE_VIDEO_ADJUST, new Object[]{false}));
        }
        mIsEnableVideoMode = false;
        return FUNCTION_SUCCESS;
    }

    @Override
    public int enableLocalVideo(boolean enabled) {
        handleVideoModule(new TTTLocalModuleConfig(TTTModuleConstants.VIDEO_LOCAL_VIDEO_ADJUST, new Object[]{enabled}));
        return FUNCTION_SUCCESS;
    }

    @Override
    public int startPreview() {
        if (!mIsEnableVideoMode || mExternalVideoSource) {
            return ERROR_FUNCTION_STATED;
        }

        Object obj = handleVideoModule(new TTTLocalModuleConfig(TTTModuleConstants.VIDEO_PREVIEW_ADJUST, new Object[]{true}));
        if (obj != null) {
            return (int) obj;
        }
        return ERROR_FUNCTION_ERROR_EMPTY_VALUE;
    }

    @Override
    public int stopPreview() {
        Object obj = handleVideoModule(new TTTLocalModuleConfig(TTTModuleConstants.VIDEO_PREVIEW_ADJUST, new Object[]{false}));
        if (obj != null) {
            return (int) obj;
        }
        return ERROR_FUNCTION_ERROR_EMPTY_VALUE;
    }

    @Override
    public int setLogFile(String mLogPath) {
        try {
            if (mLogProcess != null) {
                mLogProcess.destroy();
            }

            String clear = "logcat -c";
            Process mClearPro = Runtime.getRuntime().exec(clear);
            processWaitFor(mClearPro);
            mClearPro.destroy();

            if (TextUtils.isEmpty(mLogPath)) {
                return ERROR_FUNCTION_ERROR_EMPTY_VALUE;
            }

            File createLogFile = new File(mLogPath);
            if (!createLogFile.exists() || !createLogFile.isFile()) {
                return LOG_CREATE_ERROR_NO_FILE;
            }

            String mFilter = "";
            switch (mLogFilterLevel) {
                case LOG_FILTER_INFO:
                    mFilter = "*:i";
                    break;
                case LOG_FILTER_DEBUG:
                    mFilter = "*:d";
                    break;
                case LOG_FILTER_WARNING:
                    mFilter = "*:w";
                    break;
                case LOG_FILTER_ERROR:
                    mFilter = "*:e";
                    break;
                case LOG_FILTER_OFF:
                    mFilter = "";
                    break;
            }
            String log = "logcat " + mFilter + " -v time -f " + createLogFile.getAbsolutePath();
            mLogProcess = Runtime.getRuntime().exec(log);
            PviewLog.i("Cache mLogPath : " + mLogPath);
            mCacheLogPath = mLogPath;
        } catch (Exception e) {
            PviewLog.i(PviewLog.FUN_ERROR, "setLogFile exception : " + e.getLocalizedMessage());
            return LOG_CREATE_ERROR_UNKNOW;
        }
        return FUNCTION_SUCCESS;
    }

    @Override
    public int setLogFilter(int var1) {
        mLogFilterLevel = var1;
        if (!TextUtils.isEmpty(mCacheLogPath)) {
            setLogFile(mCacheLogPath);
            return FUNCTION_SUCCESS;
        }
        return ERROR_FUNCTION_ERROR_ARGS;
    }

    @Override
    public int setupLocalVideo(VideoCanvas canvas, int activityOrientation) {
        if (!mIsEnableVideoMode || mExternalVideoSource) {
            return ERROR_FUNCTION_STATED;
        }

        handleVideoModule(new TTTLocalModuleConfig(TTTModuleConstants.VIDEO_OPEN_LOCAL, new Object[]{canvas.getSurface(), activityOrientation, null, canvas.getShowMode()}));
        return FUNCTION_SUCCESS;
    }

    @Override
    public int setupRemoteVideo(VideoCanvas canvas) {
        int result = FUNCTION_SUCCESS;
        long userID = 0;
        if (canvas == null) {
            result = ERROR_FUNCTION_ERROR_EMPTY_VALUE;
        } else {
            userID = canvas.getUserID();
            if (!mIsEnableVideoMode) {
                result = ERROR_FUNCTION_STATED;
            } else {
                UserDeviceConfig udc = GlobalHolder.getInstance().getUserDefaultDevice(canvas.getUserID());
                if (udc == null) {
                    GlobalHolder.getInstance().getWaitOpenDevices().add(new TTTVideoCanvas(canvas.getUserID(), canvas.getShowMode(), canvas.getSurface()));
                    PviewLog.funEmptyError("setupRemoteVideo", "UserDeviceConfig", String.valueOf(canvas.getUserID()));
                    result = ERROR_FUNCTION_ERROR_EMPTY_VALUE;
                } else {
                    if (!udc.isUse()) {
                        PviewLog.funEmptyError("setupRemoteVideo", "该用户的视频设备未启用,inuse=0", String.valueOf(canvas.getUserID()));
                        result = ERROR_FUNCTION_ERROR_EMPTY_VALUE;
                    }
                    handleVideoModule(new TTTLocalModuleConfig(TTTModuleConstants.VIDEO_OPEN_REMOTE, new Object[]{
                            canvas.getSurface(), canvas.getUserID(), udc.getDeviceID(), canvas.getShowMode()}));
                }
            }

        }
        PviewLog.d("setupRemoteVideo 1 start open user video , id : " + userID);
        return result;
    }

    @Override
    public int switchCamera() {
        if (!mIsEnableVideoMode || mExternalVideoSource) {
            return ERROR_FUNCTION_STATED;
        }

        Object obj = handleVideoModule(new TTTLocalModuleConfig(TTTModuleConstants.SWITCH_CAMERA, new Object[]{}));
        if (obj != null) {
            return (int) obj;
        }
        return ERROR_FUNCTION_ERROR_EMPTY_VALUE;
    }

    @Override
    public int setVideoProfile(int profile, boolean swapWidthAndHeight) {
        handleVideoModule(new TTTLocalModuleConfig(TTTModuleConstants.VIDEO_PROFILE, new Object[]{profile, swapWidthAndHeight}));
        return FUNCTION_SUCCESS;
    }

    @Override
    public int setEnableSpeakerphone(boolean isOpenSpeaker) {
        AudioManager audioManager = (AudioManager) mWorkerThread.getContext().getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            if (isOpenSpeaker) {
                try {
                    audioManager.setSpeakerphoneOn(true);
                } catch (Exception e) {
                    PviewLog.d("setEnableSpeakerphone , setSpeakerphoneOn error! ");
                }
                mIsSpeakerphoneEnabled = true;
                PviewLog.i("setEnableSpeakerphone audio -> mIsSpeakerphoneEnabled true , speaker!");
                GlobalHolder.getInstance().notifyCHAudioRouteChanged(Constants.AUDIO_ROUTE_SPEAKER);
            } else {
                try {
                    audioManager.setSpeakerphoneOn(false);
                } catch (Exception e) {
                    PviewLog.d("setEnableSpeakerphone , setSpeakerphoneOn error! ");
                }
                PviewLog.i("setEnableSpeakerphone audio -> mIsSpeakerphoneEnabled false , headphone!");
                mIsSpeakerphoneEnabled = false;
                GlobalHolder.getInstance().notifyCHAudioRouteChanged(Constants.AUDIO_ROUTE_HEADPHONE);
            }
        } else {
            return ERROR_FUNCTION_ERROR_EMPTY_VALUE;
        }
        return FUNCTION_SUCCESS;
    }

    @Override
    public boolean isSpeakerphoneEnabled() {
        return mIsSpeakerphoneEnabled;
    }

    @Override
    public int muteLocalAudioStream(boolean muted) {
        GlobalConfig.mIsMuteLocalAudio = muted;
        EnterConfApi.getInstance().muteLocalAudio(muted);
//        EnterConfApi.getInstance().applySpeakPermission(muted);
        return FUNCTION_SUCCESS;
    }

    @Override
    public int muteLocalVideoStream(boolean muted) {
        GlobalConfig.mIsMuteLocalVideo = muted;
        EnterConfApi.getInstance().muteLocalVideo(muted);
        return FUNCTION_SUCCESS;
    }

    @Override
    public int muteRemoteAudioStream(long uid, boolean muted) {
        EnterConfApi.getInstance().muteRemoteAudio(uid, muted);
        return FUNCTION_SUCCESS;
    }

    @Override
    public int muteRemoteVideoStream(long uid, boolean muted) {
        UserDeviceConfig udc = GlobalHolder.getInstance().getUserDefaultDevice(uid);
        if (udc != null) {
            if (muted) {
                EnterConfApi.getInstance().openDeviceVideo(uid, udc.getDeviceID());
            } else {
                EnterConfApi.getInstance().closeDeviceVideo(uid, udc.getDeviceID());
            }
        } else {
            return ERROR_FUNCTION_INVOKE_ERROR;
        }
        return FUNCTION_SUCCESS;
    }

    @Override
    public int muteAllRemoteAudioStreams(boolean muted) {
        EnterConfApi.getInstance().muteAllRemoteAudio(muted);
        return FUNCTION_SUCCESS;
    }

    @Override
    public int muteAllRemoteVideoStreams(boolean muted) {
        EnterConfApi.getInstance().muteAllRemoteVideo(muted);
        return FUNCTION_SUCCESS;
    }

    @Override
    public int setChannelProfile(int mode) {
        RoomMode value = RoomMode.getValue(mode);
        if (value == RoomMode.ROOM_MODE_UNFINE) {
            return ERROR_FUNCTION_ERROR_ARGS;
        }

        mCurrentChannelMode = mode;
        EnterConfApi.getInstance().setRoomMode(value);
        return FUNCTION_SUCCESS;
    }

    @Override
    public int setDefaultAudioRouteToSpeakerphone(boolean defaultToSpeaker) {
        if (GlobalConfig.mIsInRoom.get()) {
            setEnableSpeakerphone(defaultToSpeaker);
        }

        if (defaultToSpeaker) {
            GlobalConfig.mDefaultAudioRoute = Constants.AUDIO_ROUTE_SPEAKER;
        } else {
            GlobalConfig.mDefaultAudioRoute = Constants.AUDIO_ROUTE_HEADPHONE;
        }
        return FUNCTION_SUCCESS;
    }

    @Override
    public int linkOtherAnchor(long sessionId, long userId) {
        EnterConfApi.getInstance().linkOtherAnchor(sessionId, userId);
        return FUNCTION_SUCCESS;
    }

    @Override
    public int unlinkOtherAnchor(long sessionId, long userId, String devID) {
        EnterConfApi.getInstance().unlinkOtherAnchor(sessionId, userId, devID);
        return FUNCTION_SUCCESS;
    }

    @Override
    public int setClientRole(int role, String permissionKey) {
        if (role == Constants.CLIENT_ROLE_ANCHOR) {
            GlobalConfig.mIsLocalHost = CLIENT_ROLE_ANCHOR;
            if (GlobalConfig.mIsInRoom.get()) {
                RoomJni.getInstance().RoomChangeMyRole(CLIENT_ROLE_ANCHOR);
                EnterConfApi.getInstance().applySpeakPermission(true);
                EnterConfApi.getInstance().muteLocalAudio(false);
                if (GlobalConfig.mIsEnableVideoMode) {
                    EnterConfApi.getInstance().muteLocalVideo(false);
                } else {
                    EnterConfApi.getInstance().muteLocalVideo(false);
                }
            }
        } else if (role == Constants.CLIENT_ROLE_AUDIENCE) {
            GlobalConfig.mIsLocalHost = CLIENT_ROLE_AUDIENCE;
            if (GlobalConfig.mIsInRoom.get()) {
                RoomJni.getInstance().RoomChangeMyRole(CLIENT_ROLE_AUDIENCE);
                EnterConfApi.getInstance().applySpeakPermission(false);
                EnterConfApi.getInstance().muteLocalAudio(true);
                EnterConfApi.getInstance().muteLocalVideo(true);
            }
        } else if (role == CLIENT_ROLE_BROADCASTER) {
            GlobalConfig.mIsLocalHost = CLIENT_ROLE_BROADCASTER;
            if (GlobalConfig.mIsInRoom.get()) {
                RoomJni.getInstance().RoomChangeMyRole(CLIENT_ROLE_BROADCASTER);
                EnterConfApi.getInstance().applySpeakPermission(true);
                EnterConfApi.getInstance().muteLocalAudio(false);
                if (GlobalConfig.mIsEnableVideoMode) {
                    EnterConfApi.getInstance().muteLocalVideo(false);
                } else {
                    EnterConfApi.getInstance().muteLocalVideo(false);
                }
            }
        }
        PviewLog.d("setClientRole : " + GlobalConfig.mIsLocalHost);
        return FUNCTION_SUCCESS;
    }

    @Override
    public int setHighQualityAudioParameters(boolean enable) {
        EnterConfApi.getInstance().useHighQualityAudio(enable);
        return FUNCTION_SUCCESS;
    }

    @Override
    public int setVideoCompositingLayout(VideoCompositingLayout layout) {
        if (layout == null) {
            return ERROR_FUNCTION_ERROR_ARGS;
        }

        VideoCompositingLayout.Region[] regions = layout.regions;
        if (GlobalConfig.mIsLocalHost != Constants.CLIENT_ROLE_ANCHOR ||
                regions == null || regions.length <= 0) {
            return ERROR_FUNCTION_ERROR_ARGS;
        }

        try {
            UserDeviceConfig localDeviceConfig = GlobalHolder.getInstance().getUserDefaultDevice(GlobalConfig.mLocalUserID);
            if (localDeviceConfig == null) {
                PviewLog.e("SEI -> setVideoCompositingLayout error! < " + GlobalConfig.mLocalUserID + " > Get local device obj is null!");
                return ERROR_FUNCTION_INVOKE_ERROR;
            }

            String localDeviceID = localDeviceConfig.getDeviceID();
            if (TextUtils.isEmpty(localDeviceID)) {
                PviewLog.e("SEI -> setVideoCompositingLayout error! < " + GlobalConfig.mLocalUserID + " > Get local device id is null!");
                return ERROR_FUNCTION_INVOKE_ERROR;
            }
            JSONObject Sei = new JSONObject();
            Sei.put("mid", localDeviceID);
            JSONArray pos = new JSONArray();
            for (VideoCompositingLayout.Region region : regions) {
                JSONObject temp = new JSONObject();
                UserDeviceConfig mDefDeviceConfig = GlobalHolder.getInstance().getUserDefaultDevice(region.mUserID);
                if (mDefDeviceConfig == null) {
                    PviewLog.e("SEI -> setVideoCompositingLayout error! < " + region.mUserID + " > 获取此用户ID对应默认的设备ID是空的!");
                    continue;
                }

                String mDefDeviceID = mDefDeviceConfig.getDeviceID();
                if (TextUtils.isEmpty(mDefDeviceID)) {
                    PviewLog.e("SEI -> setVideoCompositingLayout error! < " + region.mUserID + " > 从UserDeviceConfig获取此用户ID对应默认的设备ID是空的!");
                    continue;
                }

                VideoJni.getInstance().RtmpAddVideo(region.mUserID, mDefDeviceID, 1);
                PviewLog.i("SEI -> RtmpAddVideo uid : " + region.mUserID + " | device id : " + mDefDeviceID);

                temp.put("id", mDefDeviceID);
                temp.put("z", region.zOrder);
                temp.put("x", region.x);
                temp.put("y", region.y);
                temp.put("w", region.width);
                temp.put("h", region.height);
                pos.put(temp);
            }

            int[] encSize = ExternalVideoModule.getInstance().getEncodeSize();
            Sei.put("pos", pos);
            long ts = System.currentTimeMillis();
            Sei.put("ts", ts);
            Sei.put("ver", "20161227");
            JSONObject canvas = new JSONObject();
            canvas.put("w", encSize[0]);
            canvas.put("h", encSize[1]);
            JSONArray bgrgb = new JSONArray();
            bgrgb.put(layout.backgroundColor[0]).put(layout.backgroundColor[1]).
                    put(layout.backgroundColor[2]);
            canvas.put("bgrgb", bgrgb);
            Sei.put("canvas", canvas);
            PviewLog.d("SEI -> finally send : " + Sei.toString());
            VideoJni.getInstance().RtmpSetH264Sei(Sei.toString(), Sei.toString());
        } catch (JSONException e) {
            e.printStackTrace();
            return ERROR_FUNCTION_ERROR_EMPTY_VALUE;
        }
        return FUNCTION_SUCCESS;
    }

    @Override
    public int setSpeakerphoneVolume(int volume) {
        if (volume < 0 || volume > 255) {
            return ERROR_FUNCTION_ERROR_ARGS;
        }

        float rate = (float) volume / 255f;
        AudioManager audioManager = (AudioManager) mWorkerThread.getContext().
                getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            int streamMaxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL);
            float result = streamMaxVolume * rate + 0.5f;
            audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL,
                    (int) result, 0);
        } else {
            return ERROR_FUNCTION_ERROR_EMPTY_VALUE;
        }
        return FUNCTION_SUCCESS;
    }

    @Override
    public int enableAudioVolumeIndication(int interval, int smooth) {
        int finallyInterval;
        if (interval <= 0) {
            finallyInterval = Integer.MAX_VALUE;
        } else {
            finallyInterval = interval;
        }
        EnterConfApi.getInstance().setAudioLevelReportInterval(finallyInterval);
        return FUNCTION_SUCCESS;
    }

    @Override
    public int startAudioMixing(String filePath, boolean loopback, boolean replace, int cycle) {
        if (cycle <= 0 && cycle != -1) {
            return ERROR_FUNCTION_ERROR_ARGS;
        }

        if (cycle == -1) {
            cycle = Integer.MAX_VALUE;
        }

        boolean mIsOk = MyAudioApi.getInstance(mWorkerThread.getContext()).startAudioFileMixing(filePath, loopback, cycle);
        if (mIsOk) {
            float mixBit = 0.3f;
            if (replace) {
                mixBit = 1.0f;
            }
            MyAudioApi.getInstance(mWorkerThread.getContext()).adjustAudioFileVolumeScale(mixBit);
        }
        return mIsOk ? FUNCTION_SUCCESS : AUDIO_MIX_PLAY_FAILED;
    }

    @Override
    public int stopAudioMixing() {
        if (!GlobalConfig.mIsInRoom.get()) {
            return ERROR_FUNCTION_STATED;
        }
        MyAudioApi.getInstance(mWorkerThread.getContext()).stopAudioFileMixing();
        return FUNCTION_SUCCESS;
    }

    @Override
    public int pauseAudioMixing() {
        if (!GlobalConfig.mIsInRoom.get()) {
            return ERROR_FUNCTION_STATED;
        }
        MyAudioApi.getInstance(mWorkerThread.getContext()).pauseAudioFileMix();
        return FUNCTION_SUCCESS;
    }

    @Override
    public int resumeAudioMixing() {
        if (!GlobalConfig.mIsInRoom.get()) {
            return ERROR_FUNCTION_STATED;
        }
        MyAudioApi.getInstance(mWorkerThread.getContext()).resumeAudioFileMix();
        return FUNCTION_SUCCESS;
    }

    @Override
    public int getAudioMixingDuration() {
        return GlobalConfig.mCurrentAudioMixingDuration;
    }

    @Override
    public boolean kickChannelUser(long uid) {
        if (uid == mLocalUserID) {
            return false;
        }
        EnterConfApi.getInstance().kickUser(uid);
        return true;
    }

    @Override
    public int muteRemoteSpeaking(long uid, boolean isDisable) {
        if (isDisable) {
            RoomJni.getInstance().RoomGrantPermission(uid, 1, 1);
        } else {
            RoomJni.getInstance().RoomGrantPermission(uid, 1, 3);
        }
        return FUNCTION_SUCCESS;
    }

    @Override
    public int getAudioMixingCurrentPosition() {
        return GlobalConfig.mCurrentAudioMixingPosition;
    }

    @Override
    public String getVersion() {
        return GlobalConfig.SDK_VERSION_NAME;
    }

    @Override
    public boolean isTextureEncodeSupported() {
        List<String> exceptionModels = Arrays.asList(H264_HW_BLACKLIST);
        if (exceptionModels.contains(Build.MODEL)) {
            Log.w("DeviceUtils", "Model: " + Build.MODEL + " has black listed H.264 encoder.");
            return false;
        }
        if (Build.VERSION.SDK_INT <= 18) {
            return false;
        }
        return true;
    }

    @Override
    public void setExternalVideoSource(boolean enable, boolean useTexture, boolean pushMode) {
        mExternalVideoSource = enable;
        mExternalVideoSourceIsTexture = enable && useTexture && isTextureEncodeSupported();
        // TODO 第三个参数没还没管
    }

    @Override
    public boolean pushExternalVideoFrame(ConfVideoFrame mFrame) {
        if (!mIsEnableVideoMode && (mFrame == null) || (mFrame.format == 12)) {
            return false;
        }

        Object obj = handleVideoModule(new TTTLocalModuleConfig(TTTModuleConstants.VIDEO_EXTERNAL_VIDEO_FRAME, new Object[]{mFrame, mWorkerThread.getContext()}));
        return obj != null && (boolean) obj;
    }

    @Override
    public int adjustAudioMixingVolume(int volume) {
        if (!GlobalConfig.mIsInRoom.get()) {
            return ERROR_FUNCTION_ERROR_EMPTY_VALUE;
        }
        MyAudioApi.getInstance(mWorkerThread.getContext()).adjustAudioFileVolumeScale(volume);
        return FUNCTION_SUCCESS;
    }

    @Override
    public int configPublisher(PublisherConfiguration config) {
        if (config == null) {
            return ERROR_FUNCTION_ERROR_EMPTY_VALUE;
        }

        String pushUrl = config.getPushUrl();
        if (TextUtils.isEmpty(pushUrl)) {
            return ERROR_FUNCTION_ERROR_EMPTY_VALUE;
        }
        GlobalConfig.mPushUrl = pushUrl;
        return FUNCTION_SUCCESS;
    }

    @Override
    public int sendChatMessage(long nDstUserID, int type, String sSeqID, String sData) {
        handleChatModule(new TTTLocalModuleConfig(TTTChatModuleContants.CHAT_SEND_MESSAGE, new Object[]{nDstUserID, type,
                sSeqID, sData}));
        return FUNCTION_SUCCESS;
    }

    @Override
    public int sendSignal(long nDstUserID, String sSeqID, String sData) {
        handleChatModule(new TTTLocalModuleConfig(TTTChatModuleContants.CHAT_SEND_SIGNAL, new Object[]{nDstUserID, sSeqID,
                sData}));
        return FUNCTION_SUCCESS;
    }

    @Override
    public int startRecordChatAudio() {
        handleChatModule(new TTTLocalModuleConfig(TTTChatModuleContants.CHAT_RECORD_AUDIO, new Object[]{}));
        return FUNCTION_SUCCESS;
    }

    @Override
    public int stopRecordAndSendChatAudio(long nDstUserID, String sSeqID) {
        Object obj = handleChatModule(new TTTLocalModuleConfig(
                TTTChatModuleContants.CHAT_STOP_RECORD_AUDIO_AND_SEND, new Object[]{nDstUserID, sSeqID}));
        if (obj != null) {
            return (int) obj;
        }
        return FUNCTION_SUCCESS;
    }

    @Override
    public int cancleRecordChatAudio() {
        handleChatModule(new TTTLocalModuleConfig(TTTChatModuleContants.CHAT_CANNEL_RECORD_AUDIO, new Object[]{}));
        return FUNCTION_SUCCESS;
    }

    @Override
    public int playChatAudio(String audioPath) {
        handleChatModule(new TTTLocalModuleConfig(TTTChatModuleContants.CHAT_PLAY_CHAT_AUDIO, new Object[]{audioPath}));
        return FUNCTION_SUCCESS;
    }

    @Override
    public int stopChatAudio() {
        handleChatModule(new TTTLocalModuleConfig(TTTChatModuleContants.CHAT_STOP_CHAT_AUDIO, new Object[]{}));
        return FUNCTION_SUCCESS;
    }

    @Override
    public boolean isChatAudioPlaying() {
        Object obj = handleChatModule(new TTTLocalModuleConfig(TTTChatModuleContants.CHAT_AUDIO_STATE, new Object[]{}));
        return obj != null && (boolean) obj;
    }

    @Override
    public void shareScreenRecorder(boolean isShare) {
        if (isShare) {
            GlobalConfig.mIsScreenRecordShare.set(true);
        } else {
            GlobalConfig.mIsScreenRecordShare.set(false);
        }
    }

    @Override
    public void tryScreenRecorder(Activity mActivity) {
        try {
            mRecorderLock.lock();
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                return;
            }

            if (mScreenCapture == null) {
                mScreenCapture = new ScreenCapture(mActivity);
            }

            if (mScreenCapture.isRecording()) {
                return;
            }
            tryRecordScreen();
        } finally {
            mRecorderLock.unlock();
        }
    }

    @Override
    public boolean startScreenRecorder(Intent data, ScreenRecordConfig mConfig) {
        try {
            mRecorderLock.lock();
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                return false;
            }

            if (mScreenCapture == null) {
                return false;
            }

            EncoderConfig mEncoderConfig = new EncoderConfig(getFile(),
                    mConfig.mRecordWidth, mConfig.mRecordHeight,
                    mConfig.mRecordBitRate, mConfig.mRecordFrameRate, 1);
            mScreenCapture.startProjection(data, mEncoderConfig);
            mScreenCapture.attachRecorder(mEncoderConfig);
            return true;
        } finally {
            mRecorderLock.unlock();
        }
    }

    @Override
    public void stopScreenRecorder() {
        try {
            mRecorderLock.lock();
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                return;
            }

            if (mScreenCapture != null && mScreenCapture.isProjecting()) {
                mScreenCapture.stopProjection();
                mScreenCapture = null;
            }
        } finally {
            mRecorderLock.unlock();
        }
    }

    @Override
    public void speechRecognition(String path) {
        handleChatModule(new TTTLocalModuleConfig(TTTChatModuleContants.ACTION_SPEECH_RECOGNITION, new Object[]{path}));
    }

    @Override
    public int startRtmpPublish(String rtmpUrl) {
        if (TextUtils.isEmpty(rtmpUrl)) {
            return ERROR_FUNCTION_ERROR_EMPTY_VALUE;
        }

        Object obj = handleRTMPModule(new TTTLocalModuleConfig(TTTRtmpModuleConstants.RTMP_START_PUSH, new Object[]{rtmpUrl}));
        if (obj != null) {
            boolean result = (boolean) obj;
            return result ? FUNCTION_SUCCESS : ERROR_FUNCTION_INVOKE_ERROR;
        }
        return ERROR_FUNCTION_INVOKE_ERROR;
    }

    @Override
    public int stopRtmpPublish() {
        Object obj = handleRTMPModule(new TTTLocalModuleConfig(TTTRtmpModuleConstants.RTMP_STOP_PUSH, null));
        if (obj != null) {
            boolean result = (boolean) obj;
            return result ? FUNCTION_SUCCESS : ERROR_FUNCTION_INVOKE_ERROR;
        }
        return ERROR_FUNCTION_INVOKE_ERROR;
    }

    @Override
    public int pauseRtmpPublish() {
        handleRTMPModule(new TTTLocalModuleConfig(TTTRtmpModuleConstants.RTMP_PAUSE, new Object[]{true}));
        return FUNCTION_SUCCESS;
    }

    @Override
    public int resumeRtmpPublish() {
        handleRTMPModule(new TTTLocalModuleConfig(TTTRtmpModuleConstants.RTMP_PAUSE, new Object[]{false}));
        return FUNCTION_SUCCESS;
    }

    private void processWaitFor(Process process) {
        InputStream stderr = process.getErrorStream();
        InputStreamReader isr = new InputStreamReader(stderr);
        BufferedReader br = new BufferedReader(isr);
        String line;
        try {
            while ((line = br.readLine()) != null)
                System.out.println(line);
            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public File getFile() {
        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath();
        File file = new File(path,
                System.currentTimeMillis() + ".temp");
        PviewLog.i("System Image Path : " + path);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        return file;
    }

    private void tryRecordScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (mScreenCapture == null) {
                return;
            }

            mScreenCapture.setMediaProjectionReadyListener(new ScreenCapture.OnMediaProjectionReadyListener() {
                @Override
                public void onMediaProjectionReady(MediaProjection mediaProjection) {
                    PviewLog.i("录屏初始化成功，准备开始");
                }
            });

            mScreenCapture.setRecordCallback(new RecordCallback() {
                @Override
                public void onRecordSuccess(String filePath, long duration) {
                    PviewLog.i("录屏成功结束。filePath : " + filePath + " | duration : " + duration);
                }

                @Override
                public void onRecordFailed(Throwable e, long duration) {
                    PviewLog.i("录屏发生错误，失败。Throwable : " + e.getLocalizedMessage() + " | duration : " + duration);
                }

                @Override
                public void onRecordedDurationChanged(int s) {
                    PviewLog.i("录屏时间。time : " + s);
                    JniWorkerThread mJniWorkerThread = GlobalHolder.getInstance().getWorkerThread();
                    mJniWorkerThread.sendMessage(JniWorkerThread.JNI_CALL_BACK_ON_RECORD_TIME, new Object[]{s});
                }
            });
            mScreenCapture.requestScreenCapture();
        }
    }

    @Override
    public int insertH264SeiContent(String content) {
        if (TextUtils.isEmpty(content)) {
            return ERROR_FUNCTION_ERROR_EMPTY_VALUE;
        }

        handleVideoModule(new TTTLocalModuleConfig(TTTModuleConstants.VIDEO_INSERT_SEI, new Object[]{content}));
        return FUNCTION_SUCCESS;
    }

    @Override
    public int startAudioMixingAssert(String assertFileName, boolean loopback, boolean replace, int cycle) {
        String result = AssertUtils.transformToFile(mWorkerThread.getContext(), assertFileName);
        if (TextUtils.isEmpty(result)) {
            return ERROR_FUNCTION_INVOKE_ERROR;
        }

        return startAudioMixing(result, loopback, replace, cycle);
    }

    @Override
    public void enableDualVideoStream(boolean isEnable) {
        GlobalConfig.mIsEnableDual = isEnable;
        EnterConfApi.getInstance().enableDualVideoStream(isEnable);
    }

    @Override
    public void enableCrossRoom(boolean isEnable) {
        GlobalConfig.mIsEnableCrossRoom = isEnable;
        EnterConfApi.getInstance().enableCrossRoom(isEnable);
    }

    @Override
    public int setVideoSteamType(long uid, int steamType) {
        if (steamType != Constants.VIDEO_STEAM_TYPE_BIG
                && steamType != Constants.VIDEO_STEAM_TYPE_SMALL) {
            return ERROR_FUNCTION_ERROR_EMPTY_VALUE;
        }

        if (!GlobalConfig.mIsEnableDual) {
            return ERROR_FUNCTION_STATED;
        }

        User user = GlobalHolder.getInstance().getUser(uid);
        if (user == null) {
            PviewLog.i("setVideoSteamType -> Get null User , id : " + uid);
            return ERROR_FUNCTION_INVOKE_ERROR;
        }

        UserDeviceConfig userDefaultDevice = GlobalHolder.getInstance().getUserDefaultDevice(uid);
        if (userDefaultDevice == null) {
            PviewLog.i("setVideoSteamType -> Get null User device , id : " + uid);
            return ERROR_FUNCTION_INVOKE_ERROR;
        }

        user.setVideoSteamType(steamType);
        EnterConfApi.getInstance().openDeviceVideo(uid, userDefaultDevice.getDeviceID());
        return FUNCTION_SUCCESS;
    }

    // ------------------------------------IJKPLAYER MODULE-------------------------------------------------

    // TODO SDK新内容，IJKPLAYER需对外公开
    public int joinChannel(String channelName) {
        PviewLog.i(TAG, "Pull channel : " + channelName + " | mIsInPullRoom : " + mIsInPullRoom);
        if (!TextUtils.isDigitsOnly(channelName)) {
            GlobalHolder.getInstance().notifyCHChannelNameError();
            return ERROR_FUNCTION_ERROR_EMPTY_VALUE;
        }

        mAudioManager.setMode(AudioManager.MODE_RINGTONE);

        if (mIsInPullRoom) {
            return ERROR_FUNCTION_STATED;
        }

        Object obj = handleIJKModule(new TTTLocalModuleConfig(TTTIjkModuleConstants.IJK_JOIN_CHANNEL,
                new Object[]{channelName}));
        if (obj != null) {
            return (int) obj;
        }
        return FUNCTION_SUCCESS;
    }

    @Override
    public void reportTestString(String testString) {
        Context context = mWorkerThread.getContext();
        Intent i = new Intent();
        i.setAction("ttt.test.interface.string");
        i.addCategory("ttt.test.interface");
        i.putExtra("testString", testString);
        context.sendBroadcast(i);
    }

    // TODO SDK新内容，IJKPLAYER需对外公开
//    public IjkVideoView CreateIjkRendererView(Context context, int showMode) {
//        Object obj = handleIJKModule(new TTTLocalModuleConfig(TTTIjkModuleConstants.IJK_CREATE_VIEW,
//                new Object[]{showMode, context}));
//        if (obj != null) {
//            return (IjkVideoView) obj;
//        }
//        return null;
//    }

    @Override
    public void openFaceBeavty(boolean flag) {
        LocaSurfaceView.getInstance().mPreviewInput.openFaceBeauty(flag);
    }

    @Override
    public void setBlurLevel(int blurLevel) {
        LocaSurfaceView.getInstance().mPreviewInput.setBlurLevel(blurLevel);
    }

    @Override
    public void setColorLevel(float colorLevel) {
        LocaSurfaceView.getInstance().mPreviewInput.setColorLevel(colorLevel);
    }

    @Override
    public void setCheekThinning(float cheekThinning) {
        LocaSurfaceView.getInstance().mPreviewInput.setCheekThinning(cheekThinning);
    }

    @Override
    public void setEyeEnlarging(float eyeEnlarging) {
        LocaSurfaceView.getInstance().mPreviewInput.setEyeEnlarging(eyeEnlarging);
    }

    @Override
    public void onEffectSelected(Effect effect) {
        LocaSurfaceView.getInstance().mPreviewInput.createItem(effect);
    }
}
