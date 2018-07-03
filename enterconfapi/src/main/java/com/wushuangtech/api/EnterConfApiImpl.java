package com.wushuangtech.api;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.wushuangtech.bean.RtcStats;
import com.wushuangtech.bean.TTTChatModuleContants;
import com.wushuangtech.inter.ConferenceHelpe;
import com.wushuangtech.inter.TTTEnterConfCallBack;
import com.wushuangtech.inter.UserExitNotifyCallBack;
import com.wushuangtech.jni.ChatJni;
import com.wushuangtech.jni.RoomJni;
import com.wushuangtech.jni.VideoJni;
import com.wushuangtech.library.Conference;
import com.wushuangtech.library.Constants;
import com.wushuangtech.library.GlobalConfig;
import com.wushuangtech.library.GlobalHolder;
import com.wushuangtech.library.InitLibData;
import com.wushuangtech.library.InstantRequest;
import com.wushuangtech.library.JNIResponse;
import com.wushuangtech.library.PviewConferenceRequest;
import com.wushuangtech.library.User;
import com.wushuangtech.library.UserDeviceConfig;
import com.wushuangtech.utils.HttpUtil;
import com.wushuangtech.utils.PviewLog;
import com.wushuangtech.utils.ReportLogger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import static com.wushuangtech.library.Constants.CLIENT_ROLE_BROADCASTER;
import static com.wushuangtech.library.Constants.CLIENT_ROLE_AUDIENCE;
import static com.wushuangtech.library.Constants.CLIENT_ROLE_ANCHOR;
import static com.wushuangtech.library.Constants.ERROR_ENTER_ROOM_VERIFY_FAILED;

public class EnterConfApiImpl extends EnterConfApi implements ConferenceHelpe {

    private static EnterConfApiImpl mEnterConfApiImpl;

    private WeakReference<EnterConfApiCallback> mCallback;
    private long mUserId;
    private long mConfId;
    private boolean mIsHost;
    private AtomicBoolean mExitConf = new AtomicBoolean();
    private boolean mOptExitConf;
    private String mAppId;
    private ReportLogger reportLogger;
    private List<Long> remote_user_list = new ArrayList<>();
    private HandlerThread handlerThread = null;
    private int mDelayOffset = 0;

    private RoomMode mRoomMode = RoomMode.ROOM_MODE_LIVE;
    private boolean mRecordFlag = false;
    private boolean mCrossRoom;

    private LocalHandler mHandler;
    private Thread mHandlerThread;
    private TTTEnterConfCallBack mTTTEnterConfCallBack;
    private PviewConferenceRequest v2ConferenceRequest;
    private static final int REQUEST_UPLOAD_LOCAL_VIDEO = 0x0004;
    private String mCurrentRoomUUID = "";

    public void setCurrentRoomUUID(String mCurrentRoomUUID) {
        this.mCurrentRoomUUID = mCurrentRoomUUID;
    }

    private EnterConfApiImpl() {
        PviewLog.d("EnterConfApiImpl init ...");
        if (!GlobalConfig.mIsInited.get()) {
            return ;
        }
        mHandlerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                mHandler = new LocalHandler();
                Looper.loop();
            }
        });
        mHandlerThread.start();
    }

    public static synchronized EnterConfApiImpl getInstance() {
        if (mEnterConfApiImpl == null) {
            synchronized (EnterConfApiImpl.class) {
                if (mEnterConfApiImpl == null) {
                    mEnterConfApiImpl = new EnterConfApiImpl();
                    mEnterConfApiImpl.handlerThread = new HandlerThread("my_thread");
                    mEnterConfApiImpl.handlerThread.start();
                }
            }
        }
        return mEnterConfApiImpl;
    }

    public void setTTTEnterConfCallBack(TTTEnterConfCallBack mTTTEnterConfCallBack) {
        this.mTTTEnterConfCallBack = mTTTEnterConfCallBack;
    }

    public void setup(String appID, Context context, int logLevel) {
        setup(appID, context, false, logLevel);
    }

    public void setup(String appID, Context context, boolean enableChat, int logLevel) {
        mAppId = appID;
        InitLibData.initlib(context, enableChat, logLevel);
    }

    @Override
    public void setAppID(String mAppID) {
        this.mAppId = mAppID;
    }

    public void teardown() {
        mHandler.removeCallbacksAndMessages(null);
        mHandler = null;
        mHandlerThread.interrupt();
        mHandlerThread = null;
        v2ConferenceRequest.clearCalledBack();
        mEnterConfApiImpl = null;
    }

    public void setEnterConfApiCallback(EnterConfApiCallback callback) {
        mCallback = new WeakReference<>(callback);
    }

    public void setServerAddress(String ip, int port) {
        if (ip != null && !ip.isEmpty()) {
            RoomJni.getInstance().setServerAddress(ip, port);
        }
    }

    public void setRoomMode(RoomMode mode) {
        setRoomMode(mode, false);
    }

    public void setRoomMode(RoomMode mode, boolean useServerAudioMixer) {
        mRoomMode = mode;

        boolean requireChair = true;
        boolean createVideomixer = true;
        if (mode == RoomMode.ROOM_MODE_LIVE) {
            requireChair = true;
            createVideomixer = true;
        } else if (mode == RoomMode.ROOM_MODE_COMMUNICATION
                || mode == RoomMode.ROOM_MODE_GAME_COMMAND
                || mode == RoomMode.ROOM_MODE_GAME_FREE) {
            requireChair = false;
            createVideomixer = false;
        } else if (mode == RoomMode.ROOM_MODE_CONFERENCE) {
            requireChair = false;
            createVideomixer = true;
        } else if (mode == RoomMode.ROOM_MODE_MANUAL) {
            // TODO
        }
        PviewLog.d("Room mode , requireChair : " + requireChair + " | createVideomixer : " + createVideomixer);
        RoomJni.getInstance().SetRoomRequireChair(requireChair);
        if(GlobalConfig.mIsEnableVideoMode && createVideomixer) {
            RoomJni.getInstance().SetRoomCreateVideoMixer(true);
        } else {
            RoomJni.getInstance().SetRoomCreateVideoMixer(false);
        }
        RoomJni.getInstance().SetUseAudioServerMixer(useServerAudioMixer);
    }

    public void setUserExitNotifyCallBack(UserExitNotifyCallBack mUserExitNotifyCallBack) {
        this.v2ConferenceRequest.setUserExitNotifyCallBack(mUserExitNotifyCallBack);
    }

    public RoomMode getRoomMode() {
        return mRoomMode;
    }

    public void setRecordMp4Flag(boolean flag) {
        mRecordFlag = flag;
    }

    public void useHighQualityAudio(boolean enable) {
        if (enable) {
            RoomJni.getInstance().SetPreferAudioCodec(RoomJni.AUDIOCODEC_AAC, 96);
        } else {
            RoomJni.getInstance().SetPreferAudioCodec(RoomJni.AUDIOCODEC_DEFAULT, 24);
        }
    }

    public void setVideoMixerParams(int bitrate, int fps, int width, int height) {
        RoomJni.getInstance().SetVideoMixerParams(bitrate, fps, width, height);
    }

    public void setAudioMixerParams(int bitrate, int samplerate, int channels) {
        RoomJni.getInstance().SetAudioMixerParams(bitrate, samplerate, channels);
    }

    public void enableCrossRoom(boolean enable) {
        mCrossRoom = enable;
    }

    private void initRoom(long userId, long sessionId, int userRole, String rtmpUrl) {
        mCurrentRoomUUID = UUID.randomUUID().toString();
        PviewLog.i("initRoom UUID -> " + mCurrentRoomUUID);
        GlobalConfig.trunOnCallback = true;
        mUserId = userId;
        mConfId = sessionId;
        mIsHost = false;
        mExitConf.set(false);
        mOptExitConf = false;
        if (v2ConferenceRequest == null) {
            v2ConferenceRequest = new PviewConferenceRequest(this, handlerThread);
        }

        synchronized (ReportLogger.class) {
            // 释放timer防止内存泄漏数据无限上报
            if (reportLogger != null) {
                reportLogger.Release();
                reportLogger = null;
            }
            reportLogger = new ReportLogger(mUserId, mConfId, mCurrentRoomUUID, mAppId);
            reportLogger.ReportEnterBegin(userRole, rtmpUrl);
        }

        RoomJni.getInstance().RoomSetUUID(mCurrentRoomUUID);
    }

    public boolean enterAudioRoom(final String channelKey, final long userId, final long sessionId, final int userRole, final String rtmpUrl) {
        initRoom(userId, sessionId, userRole, rtmpUrl);
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                v2ConferenceRequest.requestEnterRoom(
                        mAppId, sessionId, userId, userRole, rtmpUrl, false, mHandler);
                PviewLog.d(PviewLog.TAG, "Authentication -> enterAudioRoom mAppId : " + mAppId + " | userId : " + userId + " | sessionId : " + sessionId
                        + " | userRole : " + userRole
                        + " | rtmpUrl : " + rtmpUrl
                        + " | channelKey : " + channelKey);
            }
        };
        Authentication(channelKey, userId, sessionId, runnable);
        return true;
    }

    /**
     * 进入房间
     *
     * @param userId    用户Id
     * @param sessionId sessionId
     * @param userRole  用户角色
     * @param rtmpUrl   推流地址
     * @return 调用exitConf后, 过快再次调用该函数，可能返回false
     */
    public boolean enterRoom(final String channelKey, final long userId, final long sessionId, final int userRole, final String rtmpUrl) {
        initRoom(userId, sessionId, userRole, rtmpUrl);
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                v2ConferenceRequest.requestEnterRoom(
                        mAppId, sessionId, userId, userRole, rtmpUrl, mRecordFlag, mHandler);
                PviewLog.d(PviewLog.TAG, "Authentication -> enterRoom mAppId : " + mAppId + " | userId : " + userId + " | sessionId : " + sessionId
                        + " | userRole : " + userRole
                        + " | rtmpUrl : " + rtmpUrl
                        + " | channelKey : " + channelKey);
            }
        };
        Authentication(channelKey, userId, sessionId, runnable);
        return true;
    }

    /**
     * 通过服务器鉴权
     *
     * @param userId    用户Id
     * @param sessionId sessionId
     * @return 鉴权结果
     */
    public void Authentication(final String channelKey, long userId, long sessionId, final Runnable runnable) {
        if (TextUtils.isEmpty(channelKey)) {
            runnable.run();
        } else {
            String appId = mAppId;
            String url = "http://api.usercenter.wushuangtech.com/verify.php?token=" + channelKey + "&userid=" + userId + "&channelid=" + sessionId + "&appkey=" + appId;
            PviewLog.i("Authentication -> send url : " + url);
            String encodeUrl;
            try {
                encodeUrl = URLEncoder.encode(url, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                JniWorkerThread mJniWorkerThread = GlobalHolder.getInstance().getWorkerThread();
                mJniWorkerThread.sendMessage(JniWorkerThread.JNI_CALL_BACK_ON_ERROR, new Object[]{ERROR_ENTER_ROOM_VERIFY_FAILED});
                return;
            }

            HttpUtil.doGetAsyn(encodeUrl, new HttpUtil.CallBack(mCurrentRoomUUID) {
                @Override
                public void onRequestComplete(String result) {
                    PviewLog.i("Authentication -> json result : " + result);
                    if (result == null) {
                        runnable.run();
                    } else {
                        try {
                            JSONObject obj = new JSONObject(result);
                            int code = obj.getInt("code");
                            if (code == 0) {
                                if (mCurrentRoomUUID.equals(mUUID)) {
                                    runnable.run();
                                    JSONObject childObj = obj.getJSONObject("data");
                                    int remain = childObj.getInt("remain");
                                    checkAuthenticateTime(remain);
                                }
                            } else {
                                PviewLog.i("Authentication -> failed code : " + code);
                                if (mCallback != null && mCallback.get() != null) {
                                    mCallback.get().onEnterRoom(ERROR_ENTER_ROOM_VERIFY_FAILED, mIsHost);
                                }
                                JniWorkerThread mJniWorkerThread = GlobalHolder.getInstance().getWorkerThread();
                                mJniWorkerThread.sendMessage(JniWorkerThread.JNI_CALL_BACK_ON_ERROR, new Object[]{ERROR_ENTER_ROOM_VERIFY_FAILED});
                            }
                        } catch (JSONException e) {
                            PviewLog.i("Authentication -> happend JSONException : " + e.getLocalizedMessage());
                            JniWorkerThread mJniWorkerThread = GlobalHolder.getInstance().getWorkerThread();
                            mJniWorkerThread.sendMessage(JniWorkerThread.JNI_CALL_BACK_ON_ERROR, new Object[]{ERROR_ENTER_ROOM_VERIFY_FAILED});
                        }
                    }
                }

                @Override
                public void onRequestError(String errorCode) {
                    PviewLog.i("Authentication -> error code : " + errorCode);
                    JniWorkerThread mJniWorkerThread = GlobalHolder.getInstance().getWorkerThread();
                    mJniWorkerThread.sendMessage(JniWorkerThread.JNI_CALL_BACK_ON_ERROR, new Object[]{ERROR_ENTER_ROOM_VERIFY_FAILED});
                }
            });
        }
    }

    private Timer mChannelKeyTimer = null;

    private void checkAuthenticateTime(long time) {
        // 如果上次timer正在计时，就取消上次计时重新开始
        synchronized (this) {
            if (mChannelKeyTimer != null) {
                mChannelKeyTimer.cancel();
                mChannelKeyTimer = null;
            }
        }

        mChannelKeyTimer = new Timer();
        TimerTask task = new TimerTask() {

            boolean willKickUser = false;

            @Override
            public void run() {
                // 第一次执行为提醒用户channelKey即将过期，第二次执行表示channelKey过期
                if (!willKickUser) {
                    if (mCallback != null && mCallback.get() != null) {
                        mCallback.get().onRequestChannelKey();
                        GlobalHolder.getInstance().notifyCHRequestChannelKey();
                    }
                    willKickUser = true;
                } else {
                    RoomJni.getInstance().roomExit(mConfId);
                    if (mCallback != null && mCallback.get() != null) {
                        mCallback.get().onKickedOut(mConfId, 0, mUserId, EnterConfApiCallback.CHANNELKEYEXPIRED);
                        GlobalHolder.getInstance().notifyCHChannelKeyExpire();
                    }
                    synchronized (this) {
                        if (mChannelKeyTimer != null) {
                            mChannelKeyTimer.cancel();
                            mChannelKeyTimer = null;
                        }
                    }
                }
            }
        };
        if (time > 60)
            mChannelKeyTimer.schedule(task, (time - 60) * 1000, 60 * 1000);
        else
            mChannelKeyTimer.schedule(task, 0, time * 1000);
    }

    public void renewChannelKey(final String channelKey) {
        synchronized (ReportLogger.class) {
            if (reportLogger != null) {
                reportLogger.ReportRenewKey(channelKey);
            }
        }

        String url = "http://api.usercenter.wushuangtech.com/verify.php?token=" + channelKey + "&userid=" + mUserId + "&channelid=" + mConfId + "&appkey=" + mAppId;
        HttpUtil.doGetAsyn(url, new HttpUtil.CallBack(mCurrentRoomUUID) {
            @Override
            public void onRequestComplete(String result) {
                if (!mExitConf.get()) {
                    try {
                        JSONObject obj = new JSONObject(result);
                        int code = obj.getInt("code");
                        if (code == 0) {
                            JSONObject childObj = obj.getJSONObject("data");
                            int remain = childObj.getInt("remain");
                            checkAuthenticateTime(remain);
                            mCallback.get().onRenewChannelKeyResult(EnterConfApiCallback.RE_NEW_CHANNEL_KEY_SUCCESS);
                        } else {
                            mCallback.get().onRenewChannelKeyResult(EnterConfApiCallback.RE_NEW_CHANNEL_KEY_FAILD);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onRequestError(String errorCode) {
                PviewLog.i("Authentication -> renewChannelKey error code : " + errorCode);
                JniWorkerThread mJniWorkerThread = GlobalHolder.getInstance().getWorkerThread();
                mJniWorkerThread.sendMessage(JniWorkerThread.JNI_CALL_BACK_ON_ERROR, new Object[]{ERROR_ENTER_ROOM_VERIFY_FAILED});
            }
        });
    }

    public void exitRooms() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                exitRoom();
            }
        });
    }

    /**
     * 退出房间
     */
    public void exitRoom() {
        synchronized (ReportLogger.class) {
            if (reportLogger != null) {
                reportLogger.ReportExit();
                reportLogger.Release();
            }
        }

        synchronized (this) {
            if (mChannelKeyTimer != null) {
                mChannelKeyTimer.cancel();
                mChannelKeyTimer = null;
            }
        }

        if (mOptExitConf) {
            return;
        }

        PviewLog.d("exitRoom -> mExitConf : " + mExitConf);
        mOptExitConf = true;
        // 关闭视频
        VideoJni.getInstance().VideoCloseDevice(mUserId, "");
        for (int i = 0; i < remote_user_list.size(); i++) {
            Long longUserID = remote_user_list.get(i);
            UserDeviceConfig userDefaultDevice = GlobalHolder.getInstance().getUserDefaultDevice(longUserID);
            if (userDefaultDevice != null) {
                VideoJni.getInstance().VideoCloseDevice(longUserID, userDefaultDevice.getDeviceID());
            }
        }
        //关闭音频的采集编码
        ExternalAudioModule.getInstance().StopCapture();
        //调用退出房间接口
        RoomJni.getInstance().roomExit(mConfId);
        RoomJni.getInstance().ClearGlobalStatus();
        //清理数据
        GlobalHolder.getInstance().clearDatas();
        GlobalConfig.trunOnCallback = false;
        // 清理所有解码器
        if (v2ConferenceRequest != null && v2ConferenceRequest.getUserExitNotifyCallBack() != null) {
            v2ConferenceRequest.getUserExitNotifyCallBack().userExitRoom("all");
        }
        GlobalConfig.mIsInRoom.set(false);
        // 回调界面
        RtcStats rtcStats = GlobalHolder.getInstance().getRtcStats();
        JniWorkerThread mJniWorkerThread = GlobalHolder.getInstance().getWorkerThread();
        mJniWorkerThread.sendMessage(JniWorkerThread.JNI_CALL_BACK_LEAVE_CHANNEL, new Object[]{rtcStats});
    }

    /**
     * 踢出房间
     * 踢人成功后会受到被踢人的onMemberExit回调, host调用有效
     *
     * @param userId 被踢者userId
     */
    public void kickUser(long userId) {
        if (!mIsHost) {
            return;
        }
        RoomJni.getInstance().RoomKickUser(userId);
    }

    /**
     * 是否传输本地视频，不调用该函数默认不发送
     * 在initConfApiFinish返回0 后可调用
     *
     * @param enable true-上行本地视频流 false-不上行本地视频流
     * @return always true
     */
    public boolean uploadMyVideo(boolean enable) {
        UserDeviceConfig userDefaultDevice = GlobalHolder.getInstance().getUserDefaultDevice(mUserId);
        if (userDefaultDevice != null) {
            PviewLog.d("UploadMyVideo : " + enable + " | " + userDefaultDevice.getDeviceID());
            RoomJni.getInstance().UploadMyVideo(userDefaultDevice.getDeviceID(), enable);
        }
        return true;
    }

    public boolean mixAndSetSubVideoPos(long userId, String devId, boolean enable, VideoPosRation pos) {
        return mixGuestVideo(userId, devId, enable);
    }

    /**
     * 是否混屏guest视频，并且设置混频视频位置
     * 若不调用该函数，默认不混屏guest,
     * 在有guest进入（onMemberEnter）后有效
     *
     * @param userId 用户ID
     * @param enable YES-混屏guest NO-不混屏guest
     * @param poss   混屏数据体集合位置
     * @return true-成功 false-失败
     */
    public boolean mixAndSetSubVideoPos(long userId, String devId, boolean enable, List<VideoPosRation> poss) {
        boolean mixed = mixGuestVideo(userId, devId, enable);

        if (mixed) {
            setSubVideoPosRation(poss);
        }

        return mixed;
    }

    /**
     * 设置h.264 sei内容
     *
     * @param sei json格式
     */
    public void setSei(String sei, String seiExt) {
        VideoJni.getInstance().RtmpSetH264Sei(sei, seiExt != null ? seiExt : "");
    }

    /**
     * 与其他主播连麦
     *
     * @param sessionId 对方房间的sessionId
     * @param userId    对方主播的userId
     */
    public void linkOtherAnchor(long sessionId, long userId) {
        if (!mIsHost) {
            return;
        }

        // report Log link other anchor
        synchronized (ReportLogger.class) {
            if (reportLogger != null) {
                reportLogger.ReportLinkAnchor(sessionId, userId);
            }
        }

        RoomJni.getInstance().LinkAnchor(sessionId, userId);
    }

    /**
     * 结束与其他主播连麦
     *
     * @param sessionId 对方房间的sessionId
     * @param userId    对方主播的userId
     * @param devId     对方的设备ID
     */
    public void unlinkOtherAnchor(long sessionId, long userId, String devId) {
        if (!mIsHost) {
            return;
        }

        // report Log unlink other anchor
        synchronized (ReportLogger.class) {
            if (reportLogger != null) {
                reportLogger.ReportUnlinkAnchor(sessionId, userId);
            }
        }

        RoomJni.getInstance().UnlinkOtherAnchor(sessionId, userId, devId);
    }

    /**
     * 申请/释放发言权限
     *
     * @param enable true:申请 false:释放
     */
    public void applySpeakPermission(boolean enable) {
        if (enable) {
            RoomJni.getInstance().RoomApplyPermission(RoomJni.PERMISSIONTYPE_SPEAK);
        } else {
            RoomJni.getInstance().RoomReleasePermission(RoomJni.PERMISSIONTYPE_SPEAK);
        }
    }

    /**
     * @param userId 用户ID
     */
    public void grantSpeakPermission(long userId) {
        if (mIsHost) {
            RoomJni.getInstance().RoomGrantPermission(userId,
                    RoomJni.PERMISSIONTYPE_SPEAK, RoomJni.PERMISSIONSTATUS_GRANTED);
        }
    }

    /**
     * @param userid   用户ID
     * @param deviceid 设备ID
     */
    public void openDeviceVideo(long userid, String deviceid) {
        if (GlobalConfig.mIsEnableVideoMode) {
            User user = GlobalHolder.getInstance().getUser(userid);
            if (user != null) {
                if (mTTTEnterConfCallBack != null) {
                    mTTTEnterConfCallBack.openUserDevice(userid , deviceid);
                }

                if (user.getVideoSteamType() == Constants.VIDEO_STEAM_TYPE_BIG) {
                    if (user.isOpenBigVideo()) {
                        PviewLog.d("setupRemoteVideo finally open user video failed , big video is already opend !, userid : " + userid
                                + " | deviceid : " + deviceid + " | mIsEnableVideoMode : " + GlobalConfig.mIsEnableVideoMode);
                    } else {
                        if (user.isOpenSmallVideo()) {
                            user.setOpenSmallVideo(false);
                            VideoJni.getInstance().VideoCloseDevice(userid, deviceid + "_dual");
                        }
                        user.setOpenBigVideo(true);
                        VideoJni.getInstance().VideoOpenDevice(userid, deviceid);
                        PviewLog.d("setupRemoteVideo finally open user video success , big video is opend !, userid : " + userid
                                + " | deviceid : " + deviceid + " | mIsEnableVideoMode : " + GlobalConfig.mIsEnableVideoMode);
                    }
                } else {
                    if (user.isOpenSmallVideo()) {
                        PviewLog.d("setupRemoteVideo finally open user video failed , small video is already opend !, userid : " + userid
                                + " | deviceid : " + deviceid + " | mIsEnableVideoMode : " + GlobalConfig.mIsEnableVideoMode);
                    } else {
                        if (user.isOpenBigVideo()) {
                            user.setOpenBigVideo(false);
                            VideoJni.getInstance().VideoCloseDevice(userid, deviceid);
                        }
                        user.setOpenSmallVideo(true);
                        VideoJni.getInstance().VideoOpenDevice(userid, deviceid + "_dual");
                        PviewLog.d("setupRemoteVideo finally open user video success , small video is opend !, userid : " + userid
                                + " | deviceid : " + deviceid + " | mIsEnableVideoMode : " + GlobalConfig.mIsEnableVideoMode);
                    }
                }
            } else {
                PviewLog.d("setupRemoteVideo finally open user video failed , user is null !, userid : " + userid
                        + " | deviceid : " + deviceid + " | mIsEnableVideoMode : " + GlobalConfig.mIsEnableVideoMode);
            }
        }
    }

    /**
     * @param userid   用户ID
     * @param deviceid 设备ID
     */
    public void closeDeviceVideo(long userid, String deviceid) {
        if (deviceid == null) {
            deviceid = "";
        }

        if (GlobalConfig.mIsEnableVideoMode) {
            User user = GlobalHolder.getInstance().getUser(userid);
            if (user != null) {
                if (user.getVideoSteamType() == Constants.VIDEO_STEAM_TYPE_BIG) {
                    VideoJni.getInstance().VideoCloseDevice(userid, deviceid);
                    user.setOpenBigVideo(false);
                } else {
                    VideoJni.getInstance().VideoCloseDevice(userid, deviceid + "_dual");
                    user.setOpenSmallVideo(false);
                }
            } else {
                PviewLog.d("setupRemoteVideo finally close user video failed , user is null !, userid : " + userid
                        + " | deviceid : " + deviceid + " | mIsEnableVideoMode : " + GlobalConfig.mIsEnableVideoMode);
            }
        }
    }

    public void openDualVideo(long userid, String deviceid) {
        if (GlobalConfig.mIsEnableVideoMode) {
            VideoJni.getInstance().VideoOpenDevice(userid, deviceid + "_dual");
        }
    }

    public void closeDualVideo(long userid, String deviceid) {
        if (GlobalConfig.mIsEnableVideoMode) {
            VideoJni.getInstance().VideoCloseDevice(userid, deviceid + "_dual");
        }
    }

    public void muteLocalAudio(boolean mute) {
        synchronized (this) {
            Log.d("zhx", "muteLocalAudio: " + mute);
            RoomJni.getInstance().MuteLocalAudio(mute);
        }
    }

    public void muteLocalVideo(boolean mute) {
        synchronized (this) {
            PviewLog.i("muteLocalVideo... mute : " + mute);
            String myDevID = mUserId + "";
            UserDeviceConfig userDefaultDevice = GlobalHolder.getInstance().getUserDefaultDevice(mUserId);
            if (userDefaultDevice == null) {
                UserDeviceConfig udc = new UserDeviceConfig(mUserId,
                        myDevID, true);
                GlobalHolder.getInstance().updateUserDevice(mUserId, udc);
            }
            RoomJni.getInstance().MuteLocalVideo(mute);
            VideoJni.getInstance().EnableVideoDev("", mute ? 0 : 1);
        }
    }

    public void muteAllRemoteAudio(boolean mute) {
        RoomJni.getInstance().MuteAllRemoteAudio(mute);
    }

    public void muteAllRemoteVideo(boolean mute) {
        RoomJni.getInstance().MuteAllRemoteVideo(mute);
    }

    public void muteRemoteAudio(long userId, boolean mute) {
        RoomJni.getInstance().MuteRemoteAudio(userId, mute);
    }

    /**
     * 调整远端回声参数
     *
     * @param userid 用户ID
     * @param param  参数值 -250 ~ 250之间，通常应该大于0
     */
    public void adjustRemoteAudioParam(long userid, int param) {
        try {
            JSONObject adjParam = new JSONObject();
            adjParam.put("audioAdjParam", param);

            RoomJni.getInstance().SendCustomizedAudioMsg(adjParam.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /*
     * 当aec参数调整好后，可将该参数发送至服务器，用于服务器做数据收集和分析以便进行自动分配
     *
     * @param delayOffset aec参数值
     */
    public void sendAecParam(int delayOffset) {
        RoomJni.getInstance().RoomSendAECParam(Build.MODEL, delayOffset);
    }

    /*
     * 获取默认aec参数值，进入房间成功后调用
     *
     * @return aec参数值
     */
    public int getDefaultAecParam() {
        return mDelayOffset;
    }

    /*
     * 设置音量上报间隔
     *
     * @param interval 毫秒数
     */
    public void setAudioLevelReportInterval(int interval) {
        RoomJni.getInstance().SetAudioLevelReportInterval(interval);
    }

    private boolean mixGuestVideo(long userId, String devID, boolean enable) {
        if (!mIsHost) {
            return false;
        }

        if (mRoomMode == RoomMode.ROOM_MODE_COMMUNICATION) {
            return false;
        }

        // report Log mix user
        synchronized (ReportLogger.class) {
            if (reportLogger != null) {
                reportLogger.ReportMixUser(userId, enable);
            }
        }

        VideoJni.getInstance().RtmpAddVideo(userId, devID, enable ? 1 : -1);

        return true;
    }

    public void setSubVideoPosRation(List<VideoPosRation> poss) {
        int[] encSize = ExternalVideoModule.getInstance().getEncodeSize();
        try {
            JSONObject Sei = new JSONObject();
            Sei.put("mid", String.valueOf(mUserId));
            JSONArray pos = new JSONArray();
            long mBigUserID = mUserId;
            for (int i = 0; i < poss.size(); i++) {
                JSONObject temp = new JSONObject();
                VideoPosRation videoPosRation = poss.get(i);
                if (videoPosRation.z == 0) {
                    mBigUserID = videoPosRation.id;
                    temp.put("id", String.valueOf(mUserId));
                } else {
                    temp.put("id", String.valueOf(videoPosRation.id));
                }
                temp.put("x", videoPosRation.x);
                temp.put("y", videoPosRation.y);
                temp.put("w", videoPosRation.w);
                temp.put("h", videoPosRation.h);
                temp.put("z", 1);
                pos.put(temp);
            }

            JSONObject positem = new JSONObject();
            positem.put("id", String.valueOf(mBigUserID));
            positem.put("x", 0);
            positem.put("y", 0);
            positem.put("w", 1);
            positem.put("h", 1);
            positem.put("z", 0);
            pos.put(positem);
            Sei.put("pos", pos);
            long ts = System.currentTimeMillis();
            Sei.put("ts", ts);
            Sei.put("ver", "20161227");
            JSONObject canvas = new JSONObject();
            canvas.put("w", encSize[0]);
            canvas.put("h", encSize[1]);
            JSONArray bgrgb = new JSONArray();
            bgrgb.put(80).put(80).put(80);
            canvas.put("bgrgb", bgrgb);
            Sei.put("canvas", canvas);

            Log.e("updateSubPos setsei", Sei.toString());
            setSei(Sei.toString(), "");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private class LocalHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            EnterConfApi.getInstance();
            switch (msg.what) {
                case InstantRequest.REQUEST_JOIN_ROOM:
                    GlobalConfig.mIsLogining.set(false);
                    JNIResponse response = (JNIResponse) msg.obj;
                    if (response.getResult() == JNIResponse.Result.SUCCESS) {
                        Conference conf = (Conference) ((JNIResponse) msg.obj).resObj;
                        mIsHost = conf.getUserRole() == CLIENT_ROLE_ANCHOR;
                        synchronized (ReportLogger.class) {
                            if (reportLogger != null) {
                                reportLogger.ReportEnterSuccess(GlobalConfig.mIsLocalHost);
                            }
                        }

                        // 进房间成功后打开音频采集编码
                        ExternalAudioModule.getInstance().StopCapture();
                        ExternalAudioModule.getInstance().StartCapture();

                        GlobalHolder.getInstance().putOrUpdateUser(mUserId);
                        String myDevID = mUserId + "";
                        UserDeviceConfig udc = new UserDeviceConfig(mUserId,
                                myDevID, true);
                        GlobalHolder.getInstance().updateUserDevice(mUserId, udc);

                        GlobalConfig.mIsInRoom.set(true);
                        if (GlobalConfig.mCurrentChannelMode ==
                                Constants.CHANNEL_PROFILE_LIVE_BROADCASTING) {
                            if (GlobalConfig.mIsLocalHost == CLIENT_ROLE_ANCHOR) {
                                if (!GlobalConfig.mIsMuteLocalAudio) {
                                    muteLocalAudio(false);
                                }
                                if (GlobalConfig.mIsEnableVideoMode) {
                                    muteLocalVideo(false);
                                } else {
                                    muteLocalVideo(true);
                                }
                            } else if (GlobalConfig.mIsLocalHost == CLIENT_ROLE_AUDIENCE) {
                                muteLocalAudio(true);
                                muteLocalVideo(true);
                            } else if (GlobalConfig.mIsLocalHost == CLIENT_ROLE_BROADCASTER) {
                                if (!GlobalConfig.mIsMuteLocalAudio) {
                                    muteLocalAudio(false);
                                }
                                if (GlobalConfig.mIsEnableVideoMode) {
                                    muteLocalVideo(false);
                                } else {
                                    muteLocalVideo(true);
                                }
                            }
                        }
                        mHandler.sendEmptyMessageDelayed(REQUEST_UPLOAD_LOCAL_VIDEO, 1000);
                        if (mCallback != null && mCallback.get() != null)
                            mCallback.get().onEnterRoom(0, mIsHost);
                        JniWorkerThread mJniWorkerThread = GlobalHolder.getInstance().getWorkerThread();
                        Message.obtain(mJniWorkerThread.getWorkerHandler(), JniWorkerThread.JNI_CALL_BACK_ENTER_ROOM,
                                new Object[]{String.valueOf(conf.getId()), GlobalConfig.mLocalUserID}).sendToTarget();
                    } else {
                        synchronized (ReportLogger.class) {
                            if (reportLogger != null) {
                                reportLogger.ReportEnterFail(GlobalConfig.mIsLocalHost, response.getResult());
                            }
                        }

                        if (response.getResult() == JNIResponse.Result.VERTIFY_FAILED) {
                            if (mCallback != null && mCallback.get() != null)
                                mCallback.get().onEnterRoom(ERROR_ENTER_ROOM_VERIFY_FAILED, mIsHost);
                        } else if (response.getResult() == JNIResponse.Result.SERVER_REJECT) {
                            if (mCallback != null && mCallback.get() != null)
                                mCallback.get().onEnterRoom(Constants.ERROR_ENTER_ROOM_BAD_VERSION, mIsHost);
                        } else {
                            if (mCallback != null && mCallback.get() != null)
                                mCallback.get().onEnterRoom(Constants.ERROR_ENTER_ROOM_FAILED, mIsHost);
                        }

                        JniWorkerThread mJniWorkerThread = GlobalHolder.getInstance().getWorkerThread();
                        mJniWorkerThread.sendMessage(JniWorkerThread.JNI_CALL_BACK_ON_ERROR, new Object[]{response.arg1});
                    }
                    break;
                case REQUEST_UPLOAD_LOCAL_VIDEO:
                    boolean isInvoke = false;
                    if (GlobalConfig.mCurrentChannelMode !=
                            Constants.CHANNEL_PROFILE_LIVE_BROADCASTING) {
                        isInvoke = true;
                    } else {
                        // FIXME 暂时兼容处理
                        if (GlobalConfig.mIsLocalHost == CLIENT_ROLE_ANCHOR
                                || GlobalConfig.mIsLocalHost == CLIENT_ROLE_BROADCASTER) {
                            if (GlobalConfig.mIsEnableVideoMode) {
                                uploadMyVideo(true);
                                VideoJni.getInstance().RtmpAddVideo(mUserId, String.valueOf(mUserId), 0);
                            } else {
                                uploadMyVideo(false);
                            }
                            isInvoke = true;
                        }
                    }

                    if (isInvoke) {
                        EnterConfApi.getInstance().applySpeakPermission(true);
                    }
                    break;
            }
        }
    }

    public void enableAudio(boolean enable) {
        ExternalAudioModule.getInstance().EnableAudio(enable);
    }

    public void sendCustomizedMsg(long userId, String msg) {
        RoomJni.getInstance().SendCmdMsg(userId, msg);
    }

    public void sendCustomizedAudioMsg(String msg) {
        RoomJni.getInstance().SendCustomizedAudioMsg(msg);
    }

    public void sendCustomizedVideoMsg(String msg) {
        RoomJni.getInstance().SendCustomizedVideoMsg(msg);
    }

    @Override
    public void enableChat() {
        ChatJni.getInstance().enableChat();
    }

    @Override
    public void enableSignal() {
        ChatJni.getInstance().enableSignal();
    }

    @Override
    public void sendChat(long nDstUserID, int type, String sSeqID, String sData) {
        if (TextUtils.isEmpty(sSeqID))
            sSeqID = UUID.randomUUID().toString();
        ChatJni.getInstance().sendChat(mConfId, nDstUserID, type, sSeqID, sData);
    }

    @Override
    public void sendSignal(long nDstUserID, String sSeqID, String sData) {
        String message = Base64.encodeToString(sData.getBytes(), Base64.NO_WRAP);
        ChatJni.getInstance().SendSignal(mConfId, nDstUserID, ChatJni.CHATDATATYPE_SIGNAL, sSeqID, message, message.getBytes().length);
    }

    @Override
    public void startRecordChatAudio() {
        //开始录音
        GlobalHolder instance = GlobalHolder.getInstance();
        if (instance != null) {
            instance.handleChatModule(TTTChatModuleContants.ACTION_MEDIARECORD_START_RECORD , new Object[]{});
        }
    }

    @Override
    public int stopRecordAndSendChatAudio(long nDstUserID, String sSeqID) {
        GlobalHolder instance = GlobalHolder.getInstance();
        if (instance != null) {
            return (int)instance.handleChatModule(TTTChatModuleContants.ACTION_MEDIARECORD_STOP_RECORD , new Object[]{mConfId, nDstUserID, sSeqID});
        }
        return 0;
    }

    @Override
    public void cancleRecordChatAudio() {
        GlobalHolder instance = GlobalHolder.getInstance();
        if (instance != null) {
            instance.handleChatModule(TTTChatModuleContants.ACTION_MEDIARECORD_CANNEL , new Object[]{});
        }
    }

    @Override
    public void playChatAudio(String audioPath) {
        MediaPlayerHelper.playSound(audioPath);
    }

    @Override
    public void stopChatAudio() {
        MediaPlayerHelper.stop();
    }

    @Override
    public boolean isChatAudioPlaying() {
        return false;
    }

    public void reportLocalVideoStats(EnterConfApiCallback.GSVideoStats videoStats) {
        if (!GlobalConfig.mIsMuteLocalAudio && mCallback != null && mCallback.get() != null)
            mCallback.get().onReportLocalVideoStats(videoStats);
    }

    public void reportLocalVideoLossRate(float lossRate) {
        if (!GlobalConfig.mIsMuteLocalVideo && mCallback != null && mCallback.get() != null)
            mCallback.get().onReportLocalVideoLossRate(lossRate);
    }

    public void reportRemoteVideoStats(ArrayList<EnterConfApiCallback.GSVideoStats> videoStats) {
        if (mCallback != null && mCallback.get() != null)
            mCallback.get().onReportRemoteVideoStats(videoStats);
    }

    public void reportRemoteAudioStats(ArrayList<EnterConfApiCallback.GSAudioStats> audioStats) {
        if (mCallback != null && mCallback.get() != null)
            mCallback.get().onReportRemoteAudioStats(audioStats);
    }

    public void setVideoMixerBackgroundImgUrl(String url) {
        if (url != null && !url.isEmpty())
            RoomJni.getInstance().SetVideoMixerBackgroundImgUrl(url);
    }

    public void enableDualVideoStream(boolean enable) {
        GlobalConfig.mIsEnableDual = enable;
        RoomJni.getInstance().EnableDualVideoStream(enable);
    }

    @Override
    public void onMemberEnter(long nConfId, long nUserId, String deviceInfo, int userRole, int speakStatus) {

        // report Log user enter
        synchronized (ReportLogger.class) {
            if (reportLogger != null) {
                reportLogger.ReportMemberEnter(nUserId);
                reportLogger.ReportSpeakPermission(nUserId, speakStatus);
            }
        }

        remote_user_list.add(nUserId);

        /*
        UserDeviceConfig userDefaultDevice = GlobalHolder.getInstance().getUserDefaultDevice(nUserId);
        VideoRequest.getInstance().VideoOpenDevice(nUserId, userDefaultDevice.getDeviceID());
        */

        if (mCallback != null && mCallback.get() != null)
            mCallback.get().onMemberEnter(nConfId, nUserId, deviceInfo, userRole, speakStatus);

        boolean isDualVideoEnabled = false;
        StringReader sr = new StringReader(deviceInfo);
        InputSource is = new InputSource(sr);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(is);
            Element elem = doc.getDocumentElement();
            NodeList nodeList = elem.getElementsByTagName("dual_video");
            int size = nodeList.getLength();
            if (size > 0) {
                isDualVideoEnabled = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        User user = GlobalHolder.getInstance().getUser(nUserId);
        if (user != null) {
            user.setEnableDualVideo(isDualVideoEnabled);
        }

        if (isDualVideoEnabled) {
            if (mCallback != null && mCallback.get() != null) {
                mCallback.get().onVideoaDualStreamEnabled(isDualVideoEnabled, nUserId);
            }
        }
    }

    @Override
    public void onMemberQuit(long nConfId, long nUserId , int reason) {

        // report Log user quit
        synchronized (ReportLogger.class) {
            if (reportLogger != null) {
                reportLogger.ReportMemberQuit(nUserId , reason);
            }
        }

        for (Long longUserID : remote_user_list) {
            if (longUserID == nUserId) {
                remote_user_list.remove(longUserID);
                break;
            }
        }

        UserDeviceConfig userDefaultDevice = GlobalHolder.getInstance().getUserDefaultDevice(nUserId);
        if (userDefaultDevice != null) {
            VideoJni.getInstance().VideoCloseDevice(nUserId, userDefaultDevice.getDeviceID());
        }

        if (mCallback != null && mCallback.get() != null)
            mCallback.get().onMemberExit(nConfId, nUserId , reason);
    }

    @Override
    public void onKickConference(long nConfId, long nSrcUserId, long nDstUserId, int nReason, String uuid) {
        if (!mCurrentRoomUUID.equals(uuid)) {
            PviewLog.e("所收到的ID与当前房间ID不匹配... uuid : " + uuid);
            return;
        }

        synchronized (ReportLogger.class) {
            if (reportLogger != null) {
                reportLogger.ReportKicked(nSrcUserId, nReason);
            }
        }
        mExitConf.set(true);
        if (mCallback != null && mCallback.get() != null) {
            mCallback.get().onKickedOut(nConfId, nSrcUserId, nDstUserId, nReason);
        }
    }

    @Override
    public void onConfDisconnected(String uuid) {
        if (!mCurrentRoomUUID.equals(uuid)) {
            PviewLog.e("所收到的ID与当前房间ID不匹配... uuid : " + uuid);
            return;
        }

        boolean disconnect_overtime;
        disconnect_overtime = !mExitConf.get();

        if (disconnect_overtime) {
            if (mCallback != null && mCallback.get() != null)
                mCallback.get().onDisconnected(0);

            JniWorkerThread mJniWorkerThread = GlobalHolder.getInstance().getWorkerThread();
            mJniWorkerThread.sendMessage(JniWorkerThread.JNI_CALL_BACK_CONNECT_LOST, new Object[]{});
        }
        exitRoom();
    }

    @Override
    public void onSetSei(long operUserId, String sei) {
        if (mCallback != null && mCallback.get() != null) {
            mCallback.get().onSetSei(operUserId, sei);
            try {
                JSONObject jsonObject = new JSONObject(sei);
                JSONArray jsonArray = jsonObject.getJSONArray("pos");
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonobject2 = (JSONObject) jsonArray.get(i);
                    String devid = jsonobject2.getString("id");
                    float x = Float.valueOf(jsonobject2.getString("x"));
                    float y = Float.valueOf(jsonobject2.getString("y"));
                    float w = Float.valueOf(jsonobject2.getString("w"));
                    float h = Float.valueOf(jsonobject2.getString("h"));

                    long userId;
                    int index = devid.indexOf(":");
                    if (index > 0) {
                        userId = Long.parseLong(devid.substring(0, index));
                    } else {
                        userId = Long.parseLong(devid);
                    }

                    if (mCallback != null && mCallback.get() != null) {
                        mCallback.get().onSetSubVideoPosRation(operUserId, userId, devid, x, y, w, h);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        // TODO
    }

    @Override
    public void onUpdateDevParam(String devParam) {
        try {
            JSONTokener jsonParser = new JSONTokener(devParam);
            JSONObject delay = (JSONObject) jsonParser.nextValue();
            int delayInt = delay.getInt("delay");

            mDelayOffset = delayInt;
            ExternalAudioModule.getInstance().setDelayOffsetMS(delayInt);

        } catch (JSONException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void onUpdateRtmpStatus(long nConfId, String rtmpUrl, boolean status) {
        if (mCallback != null && mCallback.get() != null)
            mCallback.get().onUpdateRtmpStatus(nConfId, rtmpUrl, status);
    }

    @Override
    public void onAnchorEnter(long nConfId, long nUserId, String devID, int error) {
        if (mCallback != null && mCallback.get() != null)
            mCallback.get().onAnchorEnter(nConfId, nUserId, devID, error);
    }

    @Override
    public void onAnchorExit(long nConfId, long nUserId) {
        if (mCallback != null && mCallback.get() != null)
            mCallback.get().onAnchorExit(nConfId, nUserId);
    }

    @Override
    public void onAnchorLinkResponse(long nConfId, long nUserId) {
        if (mCallback != null && mCallback.get() != null)
            mCallback.get().onAnchorLinkResponse(nConfId, nUserId);
    }

    @Override
    public void onAnchorUnlinkResponse(long nConfId, long nUserId) {
        if (mCallback != null && mCallback.get() != null)
            mCallback.get().onAnchorUnlinkResponse(nConfId, nUserId);
    }

    @Override
    public void onApplyPermission(long nUserId, int type) {
        if (mCallback != null && mCallback.get() != null) {
            if (type == RoomJni.PERMISSIONTYPE_SPEAK) {
                mCallback.get().onApplySpeakPermission(nUserId);
            }
        }

        // 直播模式下，主播自动同意发言申请
        // 互动模式下，服务器会自动同意发言申请
        if (mRoomMode == RoomMode.ROOM_MODE_LIVE && mIsHost && nUserId != mUserId) {
            grantSpeakPermission(nUserId);
        }
    }

    @Override
    public void onGrantPermission(long nUserId, int type, int status) {
        synchronized (ReportLogger.class) {
            if (reportLogger != null) {
                reportLogger.ReportSpeakPermission(nUserId, status);
            }
        }

        if (mCallback != null && mCallback.get() != null) {
            if (type == RoomJni.PERMISSIONTYPE_SPEAK) {
                mCallback.get().onGrantPermissionCallback(nUserId, type, status);
            }
        }
    }

    @Override
    public void onUpdateReportLogConfig(boolean reportData, boolean reportEvent, int reportInterval) {
        synchronized (ReportLogger.class) {
            if (reportLogger != null) {
                reportLogger.UpdateConfig(reportData, reportEvent, reportInterval);
            }
        }
    }

    @Override
    public void onConfChairChanged(long nConfId, long nUserId) {
        if (mCallback != null && mCallback.get() != null) {
            mCallback.get().onConfChairmanChanged(nConfId, nUserId);
        }
    }

    @Override
    public void onRecvCmdMsg(long nConfId, long nUserId, String msg) {
        try {
            JSONObject jsonObject = new JSONObject(msg);
            int param = jsonObject.getInt("audioAdjParam");

            ExternalAudioModule.getInstance().setDelayOffsetMS(param);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (mCallback != null && mCallback.get() != null) {
            mCallback.get().onRecvCustomizedMsg(nUserId, msg);
        }
    }

    @Override
    public void onReportMediaAddr(String aIp, String vIp) {
        // report Log user quit
        synchronized (ReportLogger.class) {
            if (reportLogger != null) {
                reportLogger.ReportMediaAddr(aIp, vIp);
            }
        }
    }

    @Override
    public void onMediaReconnect(int type) {
        synchronized (ReportLogger.class) {
            if (reportLogger != null) {
                reportLogger.MediaReconnect(type);
            }
        }
    }

    @Override
    public void onAudioLevelReport(long nUserID, int audioLevel, int audioLevelFullRange) {
        if (mCallback != null && mCallback.get() != null) {
            mCallback.get().onAudioLevelReport(nUserID, audioLevel, audioLevelFullRange);
        }
    }

    @Override
    public void onRecvAudioMsg(String msg) {
        try {
            JSONObject jsonObject = new JSONObject(msg);
            int param = jsonObject.getInt("audioAdjParam");

            ExternalAudioModule.getInstance().setDelayOffsetMS(param);
            return;
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (mCallback != null && mCallback.get() != null) {
            mCallback.get().onRecvCustomizedAudioMsg(msg);
        }
    }

    @Override
    public void onRecvVideoMsg(String msg) {
        if (mCallback != null && mCallback.get() != null) {
            mCallback.get().onRecvCustomizedVideoMsg(msg);
        }
    }

    @Override
    public void onUpdateVideoDev(long nUserId, String xml) {
        StringReader sr = new StringReader(xml);
        InputSource is = new InputSource(sr);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(is);
            Element elem = doc.getDocumentElement();
            NodeList nodeList = elem.getElementsByTagName("video");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                NamedNodeMap map = node.getAttributes();
                Node nodeID = map.getNamedItem("id");
                if (nodeID != null) {
                    Node nodeInuse = map.getNamedItem("inuse");
                    if (nodeInuse != null) {
                        boolean muted = nodeInuse.getNodeValue().equals("0");
                        GlobalHolder.getInstance().updateVideoMuted(nUserId, muted);
                        if (mCallback != null && mCallback.get() != null) {
                            mCallback.get().onVideoMuted(muted, nUserId);
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStartSendVideo(boolean bMute, boolean bOpen) {
        synchronized (ReportLogger.class) {
            if (reportLogger != null) {
                reportLogger.ReportStartSendVideo(bMute, bOpen);
            }
        }
    }

    @Override
    public void onStopSendVideo(int reason) {
        synchronized (ReportLogger.class) {
            if (reportLogger != null) {
                reportLogger.ReportStopSendVideo(reason);
            }
        }
    }

    @Override
    public void onStartSendAudio() {
        synchronized (ReportLogger.class) {
            if (reportLogger != null) {
                reportLogger.ReportStartSendAudio();
            }
        }
    }

    @Override
    public void onStopSendAudio() {
        synchronized (ReportLogger.class) {
            if (reportLogger != null) {
                reportLogger.ReportStopSendAudio();
            }
        }
    }

    @Override
    public void onUpdateAudioStatus(long userID, boolean speak, boolean server_mix) {
        synchronized (ReportLogger.class) {
            if (reportLogger != null) {
                reportLogger.ReportUpdateAudioStatus(userID, speak, server_mix);
            }
        }
    }

    @Override
    public void OnRemoteAudioMuted(long userID, boolean muted) {
        GlobalHolder.getInstance().updateAudioMuted(userID, muted);
        if (mCallback != null && mCallback.get() != null) {
            mCallback.get().OnRemoteAudioMuted(userID, muted);
        }
    }

    public void reportAudioRecError(int error) {
        synchronized (ReportLogger.class) {
            if (reportLogger != null) {
                reportLogger.ReportAudioRecErr(error);
            }
        }
    }

    @Override
    public boolean isAudience() {
        return GlobalConfig.mIsLocalHost == Constants.CLIENT_ROLE_AUDIENCE;
    }
}
