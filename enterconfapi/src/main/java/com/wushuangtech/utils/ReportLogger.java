package com.wushuangtech.utils;

import android.os.Build;
import android.util.Log;

import com.wushuangtech.api.EnterConfApi;
import com.wushuangtech.api.EnterConfApiCallback;
import com.wushuangtech.api.EnterConfApiImpl;
import com.wushuangtech.api.ExternalAudioModule;
import com.wushuangtech.api.ExternalVideoModule;
import com.wushuangtech.jni.ReportLogJni;
import com.wushuangtech.library.GlobalConfig;
import com.wushuangtech.library.GlobalHolder;
import com.wushuangtech.library.JNIResponse;
import com.wushuangtech.library.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class ReportLogger {

    private final class Report_Statistics {
        // video statistics
        int videoBufferDuration = 0;
        int videoRecvDataSize = 0;
        int videoSentBytes = 0;
        int videoFlowCtrlBytes = 0;
        int videoFlowCtrlFrameCount = 0;
        int videoEncodeDataSize = 0;
        int videoEncodeFrameCount = 0;
        int videoCaptureFrameCount = 0;
        int videoDecodeFrameCount = 0;
        int videoRenderFrameCount = 0;

        // audio statistics
        int audioBufferDuration = 0;
        int audioRecvDataSize = 0;
        int audioSentBytes = 0;
        int audioCaptureDataSize = 0;
        int audioEncodeDataSize = 0;
        int audioEncodeFrameCount = 0;
        int audioDecodeFrameCount = 0;
    }

    private final class ReportLogMsg {
        static final int REPORTMSG_MSG_TYPE_NORMAL = 0;
        static final int REPORTMSG_MSG_TYPE_WARNIING = 1;
        static final int REPORTMSG_MSG_TYPE_ERROR = 2;

        static final int REPORTMSG_TYPE_DATA = 0x01;
        static final int REPORTMSG_TYPE_EVENT = 0x10;

        int logType;
        int msgType;
        String logMsg;
    }

    private int logTypeMask = 0;
    private Timer timer = null;
    private long userId, sessionId;
    private Report_Statistics last_statistics = null;
    private Report_Statistics last_statistics_video = null;
    private String uuid, appid;
    private int audioReconnect, videoReconnect;
    private int logReportInterval, timerTicks;
    private long last_status_timestamp;

    private int last_video_sent_bytes = 0;
    private int last_video_flowctrl_bytes = 0;

    public ReportLogger(long userId, long sessionId, String uuid, String appid) {
        this.userId = userId;
        this.sessionId = sessionId;
        this.uuid = uuid;
        this.appid = appid;

        audioReconnect = 0;
        videoReconnect = 0;

        logReportInterval = 0x0FFFFFFF;
        timerTicks = 0;

        last_status_timestamp = System.currentTimeMillis();
    }

    public void Release() {
        if (System.currentTimeMillis() - last_status_timestamp > 5000) {
            GenerateLogData(null, null);
        }

        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private void SendLogMsg(ReportLogMsg msg) {
        Log.d("ReportLogger", "msg ::: " + msg.logMsg);

        if ((msg.logType & logTypeMask) > 0) {
            ReportLogJni.getInstance().ReportLog(msg.logMsg, msg.msgType);
        }
    }

    public void ReportAuth(String channelKey) {
        StringBuilder strBuilder = new StringBuilder("event=AUTH");
        strBuilder.append(" APPID=");
        strBuilder.append(appid);
        strBuilder.append(" nUserID=");
        strBuilder.append(userId);
        strBuilder.append(" nGroupID=");
        strBuilder.append(sessionId);
        strBuilder.append(" KEY=");
        strBuilder.append(channelKey);
        strBuilder.append(" OS=ANDROID");
        strBuilder.append(" TS=");
        strBuilder.append(System.currentTimeMillis());
        strBuilder.append(" UUID=");
        strBuilder.append(uuid);

        ReportLogMsg msg = new ReportLogMsg();
        msg.logType = ReportLogMsg.REPORTMSG_TYPE_EVENT;
        msg.logMsg = strBuilder.toString();
        msg.msgType = ReportLogMsg.REPORTMSG_MSG_TYPE_NORMAL;
        logTypeMask = msg.logType;
        SendLogMsg(msg);
        logTypeMask = 0;
    }

    public void ReportRenewKey(String channelKey) {
        StringBuilder strBuilder = new StringBuilder("event=RENEWKEY");
        strBuilder.append(" APPID=");
        strBuilder.append(appid);
        strBuilder.append(" nUserID=");
        strBuilder.append(userId);
        strBuilder.append(" nGroupID=");
        strBuilder.append(sessionId);
        strBuilder.append(" KEY=");
        strBuilder.append(channelKey);
        strBuilder.append(" OS=ANDROID");
        strBuilder.append(" TS=");
        strBuilder.append(System.currentTimeMillis());
        strBuilder.append(" UUID=");
        strBuilder.append(uuid);

        ReportLogMsg msg = new ReportLogMsg();
        msg.logType = ReportLogMsg.REPORTMSG_TYPE_EVENT;
        msg.logMsg = strBuilder.toString();
        msg.msgType = ReportLogMsg.REPORTMSG_MSG_TYPE_NORMAL;
        logTypeMask = msg.logType;
        SendLogMsg(msg);
        logTypeMask = 0;
    }

    public void ReportEnterBegin(int userRole, String rtmpUrl) {
        StringBuilder strBuilder = new StringBuilder("event=ENTER_BEGIN");
        strBuilder.append(" APPID=");
        strBuilder.append(appid);
        strBuilder.append(" nUserID=");
        strBuilder.append(userId);
        strBuilder.append(" nGroupID=");
        strBuilder.append(sessionId);
        strBuilder.append(" ROLE=");
        strBuilder.append(userRole);
        strBuilder.append(" RTMP=");
        strBuilder.append(rtmpUrl);
        strBuilder.append(" MODEL=");
        strBuilder.append(Build.MODEL);
        strBuilder.append(" OS_VER=");
        strBuilder.append(Build.VERSION.SDK_INT);
        strBuilder.append(" TS=");
        strBuilder.append(System.currentTimeMillis());
        strBuilder.append(" UUID=");
        strBuilder.append(uuid);

        ReportLogMsg msg = new ReportLogMsg();
        msg.logType = ReportLogMsg.REPORTMSG_TYPE_EVENT;
        msg.logMsg = strBuilder.toString();
        msg.msgType = ReportLogMsg.REPORTMSG_MSG_TYPE_NORMAL;
        logTypeMask = msg.logType;
        SendLogMsg(msg);
        logTypeMask = 0;
    }

    public void ReportExit() {
        StringBuilder strBuilder = new StringBuilder("event=EXIT");
        strBuilder.append(" APPID=");
        strBuilder.append(appid);
        strBuilder.append(" nUserID=");
        strBuilder.append(userId);
        strBuilder.append(" nGroupID=");
        strBuilder.append(sessionId);
        strBuilder.append(" TS=");
        strBuilder.append(System.currentTimeMillis());
        strBuilder.append(" UUID=");
        strBuilder.append(uuid);

        ReportLogMsg msg = new ReportLogMsg();
        msg.logType = ReportLogMsg.REPORTMSG_TYPE_EVENT;
        msg.logMsg = strBuilder.toString();
        msg.msgType = ReportLogMsg.REPORTMSG_MSG_TYPE_NORMAL;

        SendLogMsg(msg);
    }

    public void ReportMixUser(long mixUserId, boolean enable) {
        StringBuilder strBuilder = new StringBuilder("event=MIX_USER");
        strBuilder.append(" APPID=");
        strBuilder.append(appid);
        strBuilder.append(" nUserID=");
        strBuilder.append(userId);
        strBuilder.append(" nGroupID=");
        strBuilder.append(sessionId);
        strBuilder.append(" nMixUserID=");
        strBuilder.append(mixUserId);
        strBuilder.append(" ENABLE=");
        strBuilder.append(enable);
        strBuilder.append(" TS=");
        strBuilder.append(System.currentTimeMillis());
        strBuilder.append(" UUID=");
        strBuilder.append(uuid);

        ReportLogMsg msg = new ReportLogMsg();
        msg.logType = ReportLogMsg.REPORTMSG_TYPE_EVENT;
        msg.logMsg = strBuilder.toString();
        msg.msgType = ReportLogMsg.REPORTMSG_MSG_TYPE_NORMAL;

        SendLogMsg(msg);
    }

    public void ReportLinkAnchor(long anchorId, long anchorSessionId) {
        StringBuilder strBuilder = new StringBuilder("event=LINK_ANCHOR");
        strBuilder.append(" APPID=");
        strBuilder.append(appid);
        strBuilder.append(" nUserID=");
        strBuilder.append(userId);
        strBuilder.append(" nGroupID=");
        strBuilder.append(sessionId);
        strBuilder.append(" nAnchorID=");
        strBuilder.append(anchorId);
        strBuilder.append(" nAnchorGroupID=");
        strBuilder.append(anchorSessionId);
        strBuilder.append(" TS=");
        strBuilder.append(System.currentTimeMillis());
        strBuilder.append(" UUID=");
        strBuilder.append(uuid);

        ReportLogMsg msg = new ReportLogMsg();
        msg.logType = ReportLogMsg.REPORTMSG_TYPE_EVENT;
        msg.logMsg = strBuilder.toString();
        msg.msgType = ReportLogMsg.REPORTMSG_MSG_TYPE_NORMAL;

        SendLogMsg(msg);
    }

    public void ReportUnlinkAnchor(long anchorId, long anchorSessionId) {
        StringBuilder strBuilder = new StringBuilder("event=UNLINK_ANCHOR");
        strBuilder.append(" APPID=");
        strBuilder.append(appid);
        strBuilder.append(" nUserID=");
        strBuilder.append(userId);
        strBuilder.append(" nGroupID=");
        strBuilder.append(sessionId);
        strBuilder.append(" nAnchorID=");
        strBuilder.append(anchorId);
        strBuilder.append(" nAnchorGroupID=");
        strBuilder.append(anchorSessionId);
        strBuilder.append(" TS=");
        strBuilder.append(System.currentTimeMillis());
        strBuilder.append(" UUID=");
        strBuilder.append(uuid);

        ReportLogMsg msg = new ReportLogMsg();
        msg.logType = ReportLogMsg.REPORTMSG_TYPE_EVENT;
        msg.logMsg = strBuilder.toString();
        msg.msgType = ReportLogMsg.REPORTMSG_MSG_TYPE_NORMAL;

        SendLogMsg(msg);
    }

    public void ReportEnterSuccess(int userRole) {
        StringBuilder strBuilder = new StringBuilder("event=ENTER_SUCCESS");
        strBuilder.append(" APPID=");
        strBuilder.append(appid);
        strBuilder.append(" nUserID=");
        strBuilder.append(userId);
        strBuilder.append(" nGroupID=");
        strBuilder.append(sessionId);
        strBuilder.append(" ROLE=");
        strBuilder.append(userRole);
        strBuilder.append(" TS=");
        strBuilder.append(System.currentTimeMillis());
        strBuilder.append(" UUID=");
        strBuilder.append(uuid);

        ReportLogMsg msg = new ReportLogMsg();
        msg.logType = ReportLogMsg.REPORTMSG_TYPE_EVENT;
        msg.logMsg = strBuilder.toString();
        msg.msgType = ReportLogMsg.REPORTMSG_MSG_TYPE_NORMAL;

        SendLogMsg(msg);
    }

    public void ReportEnterFail(int ROLE, JNIResponse.Result result) {
        StringBuilder strBuilder = new StringBuilder("event=ENTER_FAIL");
        strBuilder.append(" APPID=");
        strBuilder.append(appid);
        strBuilder.append(" nUserID=");
        strBuilder.append(userId);
        strBuilder.append(" nGroupID=");
        strBuilder.append(sessionId);
        strBuilder.append(" ROLE=");
        strBuilder.append(ROLE);
        strBuilder.append(" RESULT=");
        strBuilder.append(result);
        strBuilder.append(" TS=");
        strBuilder.append(System.currentTimeMillis());
        strBuilder.append(" UUID=");
        strBuilder.append(uuid);

        ReportLogMsg msg = new ReportLogMsg();
        msg.logType = ReportLogMsg.REPORTMSG_TYPE_EVENT;
        msg.logMsg = strBuilder.toString();
        msg.msgType = ReportLogMsg.REPORTMSG_MSG_TYPE_NORMAL;

        SendLogMsg(msg);
    }

    public void ReportMemberEnter(long memberId) {
        StringBuilder strBuilder = new StringBuilder("event=MEMBER_ENTER");
        strBuilder.append(" APPID=");
        strBuilder.append(appid);
        strBuilder.append(" nUserID=");
        strBuilder.append(userId);
        strBuilder.append(" nGroupID=");
        strBuilder.append(sessionId);
        strBuilder.append(" nMemberID=");
        strBuilder.append(memberId);
        strBuilder.append(" TS=");
        strBuilder.append(System.currentTimeMillis());
        strBuilder.append(" UUID=");
        strBuilder.append(uuid);

        ReportLogMsg msg = new ReportLogMsg();
        msg.logType = ReportLogMsg.REPORTMSG_TYPE_EVENT;
        msg.logMsg = strBuilder.toString();
        msg.msgType = ReportLogMsg.REPORTMSG_MSG_TYPE_NORMAL;

        SendLogMsg(msg);
    }

    public void ReportMemberQuit(long memberId, int reason) {
        StringBuilder strBuilder = new StringBuilder("event=MEMBER_QUIT");
        strBuilder.append(" APPID=");
        strBuilder.append(appid);
        strBuilder.append(" nUserID=");
        strBuilder.append(userId);
        strBuilder.append(" nGroupID=");
        strBuilder.append(sessionId);
        strBuilder.append(" nMemberID=");
        strBuilder.append(memberId);
        strBuilder.append(" reason=");
        strBuilder.append(reason);
        strBuilder.append(" TS=");
        strBuilder.append(System.currentTimeMillis());
        strBuilder.append(" UUID=");
        strBuilder.append(uuid);

        ReportLogMsg msg = new ReportLogMsg();
        msg.logType = ReportLogMsg.REPORTMSG_TYPE_EVENT;
        msg.logMsg = strBuilder.toString();
        msg.msgType = ReportLogMsg.REPORTMSG_MSG_TYPE_NORMAL;

        SendLogMsg(msg);
    }

    public void ReportKicked(long kickedBy, int reason) {
        StringBuilder strBuilder = new StringBuilder("event=KICKED");
        strBuilder.append(" APPID=");
        strBuilder.append(appid);
        strBuilder.append(" nUserID=");
        strBuilder.append(userId);
        strBuilder.append(" nGroupID=");
        strBuilder.append(sessionId);
        strBuilder.append(" KICKED_BY=");
        strBuilder.append(kickedBy);
        strBuilder.append(" KICK_REASON=");
        strBuilder.append(reason);
        strBuilder.append(" TS=");
        strBuilder.append(System.currentTimeMillis());
        strBuilder.append(" UUID=");
        strBuilder.append(uuid);

        ReportLogMsg msg = new ReportLogMsg();
        msg.logType = ReportLogMsg.REPORTMSG_TYPE_EVENT;
        msg.logMsg = strBuilder.toString();
        msg.msgType = ReportLogMsg.REPORTMSG_MSG_TYPE_NORMAL;

        SendLogMsg(msg);
    }

    public void ReportMediaAddr(String aIp, String vIp) {
        StringBuilder strBuilder = new StringBuilder("event=MEDIAADDR");
        strBuilder.append(" APPID=");
        strBuilder.append(appid);
        strBuilder.append(" nUserID=");
        strBuilder.append(userId);
        strBuilder.append(" nGroupID=");
        strBuilder.append(sessionId);
        strBuilder.append(" A_IP=");
        strBuilder.append(aIp);
        strBuilder.append(" V_IP=");
        strBuilder.append(vIp);
        strBuilder.append(" TS=");
        strBuilder.append(System.currentTimeMillis());
        strBuilder.append(" UUID=");
        strBuilder.append(uuid);

        ReportLogMsg msg = new ReportLogMsg();
        msg.logType = ReportLogMsg.REPORTMSG_TYPE_EVENT;
        msg.logMsg = strBuilder.toString();
        msg.msgType = ReportLogMsg.REPORTMSG_MSG_TYPE_NORMAL;

        SendLogMsg(msg);
    }

    public void ReportSpeakPermission(long nUserID, int status) {
        StringBuilder strBuilder = new StringBuilder("event=SPEAKSTATUS");
        strBuilder.append(" APPID=");
        strBuilder.append(appid);
        strBuilder.append(" nUserID=");
        strBuilder.append(userId);
        strBuilder.append(" nGroupID=");
        strBuilder.append(sessionId);
        strBuilder.append(" SPEAKER=");
        strBuilder.append(nUserID);
        strBuilder.append(" STATUS=");
        strBuilder.append(status);
        strBuilder.append(" TS=");
        strBuilder.append(System.currentTimeMillis());
        strBuilder.append(" UUID=");
        strBuilder.append(uuid);

        ReportLogMsg msg = new ReportLogMsg();
        msg.logType = ReportLogMsg.REPORTMSG_TYPE_EVENT;
        msg.logMsg = strBuilder.toString();
        msg.msgType = ReportLogMsg.REPORTMSG_MSG_TYPE_NORMAL;

        SendLogMsg(msg);
    }

    public void ReportStartSendVideo(boolean bMute, boolean bOpen) {
        StringBuilder strBuilder = new StringBuilder("event=STARTSENDVIDEO");
        strBuilder.append(" APPID=");
        strBuilder.append(appid);
        strBuilder.append(" nUserID=");
        strBuilder.append(userId);
        strBuilder.append(" nGroupID=");
        strBuilder.append(sessionId);
        strBuilder.append(" LocalMute=");
        strBuilder.append(bMute);
        strBuilder.append(" Open=");
        strBuilder.append(bOpen);
        strBuilder.append(" TS=");
        strBuilder.append(System.currentTimeMillis());
        strBuilder.append(" UUID=");
        strBuilder.append(uuid);

        ReportLogMsg msg = new ReportLogMsg();
        msg.logType = ReportLogMsg.REPORTMSG_TYPE_EVENT;
        msg.logMsg = strBuilder.toString();
        msg.msgType = ReportLogMsg.REPORTMSG_MSG_TYPE_NORMAL;

        SendLogMsg(msg);
    }

    public void ReportStopSendVideo(int reason) {
        StringBuilder strBuilder = new StringBuilder("event=STOPSENDVIDEO");
        strBuilder.append(" APPID=");
        strBuilder.append(appid);
        strBuilder.append(" nUserID=");
        strBuilder.append(userId);
        strBuilder.append(" nGroupID=");
        strBuilder.append(sessionId);
        strBuilder.append(" reason=");
        strBuilder.append(reason);
        strBuilder.append(" TS=");
        strBuilder.append(System.currentTimeMillis());
        strBuilder.append(" UUID=");
        strBuilder.append(uuid);

        ReportLogMsg msg = new ReportLogMsg();
        msg.logType = ReportLogMsg.REPORTMSG_TYPE_EVENT;
        msg.logMsg = strBuilder.toString();
        msg.msgType = ReportLogMsg.REPORTMSG_MSG_TYPE_NORMAL;

        SendLogMsg(msg);
    }

    public void ReportStartSendAudio() {
        StringBuilder strBuilder = new StringBuilder("event=STARTSENDAUDIO");
        strBuilder.append(" APPID=");
        strBuilder.append(appid);
        strBuilder.append(" nUserID=");
        strBuilder.append(userId);
        strBuilder.append(" nGroupID=");
        strBuilder.append(sessionId);
        strBuilder.append(" TS=");
        strBuilder.append(System.currentTimeMillis());
        strBuilder.append(" UUID=");
        strBuilder.append(uuid);

        ReportLogMsg msg = new ReportLogMsg();
        msg.logType = ReportLogMsg.REPORTMSG_TYPE_EVENT;
        msg.logMsg = strBuilder.toString();
        msg.msgType = ReportLogMsg.REPORTMSG_MSG_TYPE_NORMAL;

        SendLogMsg(msg);
    }

    public void ReportStopSendAudio() {
        StringBuilder strBuilder = new StringBuilder("event=STOPSENDAUDIO");
        strBuilder.append(" APPID=");
        strBuilder.append(appid);
        strBuilder.append(" nUserID=");
        strBuilder.append(userId);
        strBuilder.append(" nGroupID=");
        strBuilder.append(sessionId);
        strBuilder.append(" TS=");
        strBuilder.append(System.currentTimeMillis());
        strBuilder.append(" UUID=");
        strBuilder.append(uuid);

        ReportLogMsg msg = new ReportLogMsg();
        msg.logType = ReportLogMsg.REPORTMSG_TYPE_EVENT;
        msg.logMsg = strBuilder.toString();
        msg.msgType = ReportLogMsg.REPORTMSG_MSG_TYPE_NORMAL;

        SendLogMsg(msg);
    }

    public void ReportUpdateAudioStatus(long nUserID, boolean speak, boolean server_mix) {
        StringBuilder strBuilder = new StringBuilder("event=UPDATEAUDIOSTATUS");
        strBuilder.append(" APPID=");
        strBuilder.append(appid);
        strBuilder.append(" nUserID=");
        strBuilder.append(userId);
        strBuilder.append(" nGroupID=");
        strBuilder.append(sessionId);
        strBuilder.append(" SPEAKER=");
        strBuilder.append(nUserID);
        strBuilder.append(" B_SPEAK=");
        strBuilder.append(speak);
        strBuilder.append(" SERVER_MIX=");
        strBuilder.append(server_mix);
        strBuilder.append(" TS=");
        strBuilder.append(System.currentTimeMillis());
        strBuilder.append(" UUID=");
        strBuilder.append(uuid);

        ReportLogMsg msg = new ReportLogMsg();
        msg.logType = ReportLogMsg.REPORTMSG_TYPE_EVENT;
        msg.logMsg = strBuilder.toString();
        msg.msgType = ReportLogMsg.REPORTMSG_MSG_TYPE_NORMAL;

        SendLogMsg(msg);
    }

    public void ReportUnexpectedExit(int error_code) {
        StringBuilder strBuilder = new StringBuilder("event=UNEXPECTEDEXIT");
        strBuilder.append(" APPID=");
        strBuilder.append(appid);
        strBuilder.append(" nUserID=");
        strBuilder.append(userId);
        strBuilder.append(" nGroupID=");
        strBuilder.append(sessionId);
        strBuilder.append(" ERROR=");
        strBuilder.append(error_code);
        strBuilder.append(" TS=");
        strBuilder.append(System.currentTimeMillis());
        strBuilder.append(" UUID=");
        strBuilder.append(uuid);

        ReportLogMsg msg = new ReportLogMsg();
        msg.logType = ReportLogMsg.REPORTMSG_TYPE_EVENT;
        msg.logMsg = strBuilder.toString();
        msg.msgType = ReportLogMsg.REPORTMSG_MSG_TYPE_NORMAL;

        SendLogMsg(msg);
    }

    public void ReportEnterTimeout() {
        StringBuilder strBuilder = new StringBuilder("event=ENTER_TIMEOUT");
        strBuilder.append(" APPID=");
        strBuilder.append(appid);
        strBuilder.append(" nUserID=");
        strBuilder.append(userId);
        strBuilder.append(" nGroupID=");
        strBuilder.append(sessionId);
        strBuilder.append(" TS=");
        strBuilder.append(System.currentTimeMillis());
        strBuilder.append(" UUID=");
        strBuilder.append(uuid);

        ReportLogMsg msg = new ReportLogMsg();
        msg.logType = ReportLogMsg.REPORTMSG_TYPE_EVENT;
        msg.logMsg = strBuilder.toString();
        msg.msgType = ReportLogMsg.REPORTMSG_MSG_TYPE_NORMAL;

        SendLogMsg(msg);
    }

    public void MediaReconnect(int type) {
        if (type == 0) {
            audioReconnect++;
        } else if (type == 1) {
            videoReconnect++;
        }
    }

    public void UpdateConfig(boolean reportData, boolean reportEvent, int reportInterval) {
        logTypeMask = (reportData ? ReportLogMsg.REPORTMSG_TYPE_DATA : 0) | (reportEvent ? ReportLogMsg.REPORTMSG_TYPE_EVENT : 0);
        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        logReportInterval = reportInterval;

        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                DoReport();
            }
        }, 1000, 1000);
    }

    private void DoReport() {
        timerTicks++;
        android.util.LongSparseArray<ExternalVideoModule.VideoStatistics> videoStats = ExternalVideoModule.getInstance().getVideoStatistics();
        android.util.LongSparseArray<ExternalAudioModule.AudioStatistics> audioStats = ExternalAudioModule.getInstance().getAudioStatistics();

        if (timerTicks % 2 == 0) {
            ReportLocalVideoStats();
            ReportRemoteVideoStats(videoStats);
            ReportRemoteAudioStats(audioStats);
        }

        if (timerTicks % logReportInterval == 0) {
            GenerateLogData(videoStats, audioStats);
        }
    }

    private void ReportLocalVideoStats() {
        ExternalVideoModule videoModule = ExternalVideoModule.getInstance();
        int flowctrlBytes = videoModule.getflowCtrlBytes();
        int sentBytes = videoModule.getTotalSendBytes();

        if (last_video_sent_bytes == 0 && last_video_flowctrl_bytes == 0) {
            last_video_sent_bytes = sentBytes;
            last_video_flowctrl_bytes = flowctrlBytes;
        } else {
            int sent_diff = sentBytes - last_video_sent_bytes;
            int flowctrl_diff = flowctrlBytes - last_video_flowctrl_bytes;

            float drop_rate = (float)flowctrl_diff / (float)(flowctrl_diff + sent_diff);
            ((EnterConfApiImpl)EnterConfApi.getInstance()).reportLocalVideoLossRate(drop_rate);
        }

        Report_Statistics statistics = new Report_Statistics();
        statistics.videoSentBytes = sentBytes;
        statistics.videoEncodeFrameCount = videoModule.getSentFrameCount();

        if (last_statistics_video == null) {
            last_statistics_video = statistics;
            return;
        }

        EnterConfApiCallback.GSVideoStats videoStats = new EnterConfApiCallback.GSVideoStats();
        videoStats.bitrate = (statistics.videoSentBytes - last_statistics_video.videoSentBytes) * 8 / 2;
        videoStats.fps = (statistics.videoEncodeFrameCount - last_statistics_video.videoEncodeFrameCount) / 2;

        if (videoStats.bitrate < 0) {
            videoStats.bitrate = 0;
        }
        if (videoStats.fps < 0) {
            videoStats.fps = 0;
        }

        ((EnterConfApiImpl) EnterConfApi.getInstance()).reportLocalVideoStats(videoStats);

        last_statistics_video = statistics;
    }

    private void ReportRemoteVideoStats(android.util.LongSparseArray<ExternalVideoModule.VideoStatistics> videoStatistics) {
        if (videoStatistics.size() > 0) {

            ArrayList<EnterConfApiCallback.GSVideoStats> videoStatsArray = new ArrayList<>();

            for (int i = 0; i < videoStatistics.size(); i++) {
                EnterConfApiCallback.GSVideoStats videoStats = new EnterConfApiCallback.GSVideoStats();
                ExternalVideoModule.VideoStatistics vs = videoStatistics.valueAt(i);

                videoStats.userId = videoStatistics.keyAt(i);
                videoStats.bitrate = vs.recvBitrate;
                videoStats.fps = vs.recvFramerate;

                videoStatsArray.add(videoStats);
            }

            ((EnterConfApiImpl) EnterConfApi.getInstance()).reportRemoteVideoStats(videoStatsArray);
        }
    }

    private void ReportRemoteAudioStats(android.util.LongSparseArray<ExternalAudioModule.AudioStatistics> audioStatistics) {
        if (audioStatistics.size() > 0) {

            ArrayList<EnterConfApiCallback.GSAudioStats> audioStatsArray = new ArrayList<>();

            for (int i = 0; i < audioStatistics.size(); i++) {
                EnterConfApiCallback.GSAudioStats audioStats = new EnterConfApiCallback.GSAudioStats();


                ExternalAudioModule.AudioStatistics as = audioStatistics.valueAt(i);

                audioStats.userId = audioStatistics.keyAt(i);
                audioStats.lossRate = as.lossRate;

                audioStatsArray.add(audioStats);
            }

            ((EnterConfApiImpl) EnterConfApi.getInstance()).reportRemoteAudioStats(audioStatsArray);
        }
    }

    private void GenerateLogData(android.util.LongSparseArray<ExternalVideoModule.VideoStatistics> videoStatistics,
                                 android.util.LongSparseArray<ExternalAudioModule.AudioStatistics> audioStatistics) {
        Report_Statistics statistics = new Report_Statistics();
        ExternalVideoModule videoModule = ExternalVideoModule.getInstance();
        statistics.videoBufferDuration = videoModule.getBufferDuration();
        statistics.videoRecvDataSize = videoModule.getTotalRecvBytes();
        statistics.videoSentBytes = videoModule.getTotalSendBytes();
        statistics.videoFlowCtrlBytes = videoModule.getflowCtrlBytes();
        statistics.videoFlowCtrlFrameCount = videoModule.getFlowCtrlFrameCount();
        statistics.videoEncodeDataSize = videoModule.getEncodeDataSize();
        statistics.videoEncodeFrameCount = videoModule.getEncodeFrameCount();
        statistics.videoCaptureFrameCount = videoModule.getCaptureFrameCount();
        statistics.videoDecodeFrameCount = videoModule.getDecodeFrameCount();
        statistics.videoRenderFrameCount = videoModule.getRenderFrameCount();

        ExternalAudioModule audioModule = ExternalAudioModule.getInstance();
        statistics.audioBufferDuration = audioModule.getBufferDuration();
        statistics.audioRecvDataSize = audioModule.getTotalRecvBytes();
        statistics.audioSentBytes = audioModule.getTotalSendBytes();
        statistics.audioCaptureDataSize = audioModule.getCaptureDataSzie();
        statistics.audioEncodeDataSize = audioModule.getEncodeDataSize();
        statistics.audioEncodeFrameCount = audioModule.getEncodeFrameCount();
        statistics.audioDecodeFrameCount = audioModule.getDecodeFrameCount();

        CaculateStatistics(statistics);

        StringBuilder strBuilder = new StringBuilder("event=STATUS");
        strBuilder.append(" APPID=");
        strBuilder.append(appid);
        strBuilder.append(" nUserID=");
        strBuilder.append(userId);
        strBuilder.append(" nGroupID=");
        strBuilder.append(sessionId);

        strBuilder.append(" V_BUF=");
        strBuilder.append(statistics.videoBufferDuration);
        strBuilder.append(" V_RECV=");
        strBuilder.append(statistics.videoRecvDataSize);
        strBuilder.append(" V_SENT=");
        strBuilder.append(statistics.videoSentBytes);
        strBuilder.append(" V_DROP=");
        strBuilder.append(statistics.videoFlowCtrlBytes);
        /*
        strBuilder.append(" V_DROP_FRAMES=");
        strBuilder.append(statistics.videoFlowCtrlFrameCount);
        */
        strBuilder.append(" V_ENC=");
        strBuilder.append(statistics.videoEncodeDataSize);
        /*
        strBuilder.append(" V_ENC_FRAMES=");
        strBuilder.append(statistics.videoEncodeFrameCount);
        strBuilder.append(" V_CAP_FRAMES=");
        strBuilder.append(statistics.videoCaptureFrameCount);
        strBuilder.append(" V_DEC_FRAMES=");
        strBuilder.append(statistics.videoDecodeFrameCount);
        strBuilder.append(" V_REN_FRAMES=");
        strBuilder.append(statistics.videoRenderFrameCount);
        */
        strBuilder.append(" V_RTT=");
        strBuilder.append(videoModule.getRTT());
        strBuilder.append(" V_RECVERROR=");
        strBuilder.append(videoModule.getRecvDataErrorTimes());

        if (videoStatistics != null) {
            if (videoStatistics.size() > 0) {
                strBuilder.append(" V_STATS=");
            }
            for (int i = 0; i < videoStatistics.size(); i++) {
                ExternalVideoModule.VideoStatistics vs = videoStatistics.valueAt(i);
                strBuilder.append("[");
                strBuilder.append(videoStatistics.keyAt(i));
                strBuilder.append(", ");
                strBuilder.append(vs.recvSize);
                strBuilder.append(", ");
                strBuilder.append(vs.recvFrames);
                strBuilder.append(", ");
                strBuilder.append(vs.lostFrames);
                strBuilder.append(", ");
                strBuilder.append(GlobalHolder.getInstance().isVideoMuted(videoStatistics.keyAt(i)));
                strBuilder.append("]");
            }
        }

        android.util.LongSparseArray<ExternalVideoModule.VideoRecvLen> vrls = videoModule.getVideoRecvLenStatistics();
        if (vrls.size() > 0) {
            strBuilder.append(" V_LEN_STATS=");
        }
        for (int i = 0; i < vrls.size(); i++) {
            strBuilder.append("[");
            strBuilder.append(vrls.keyAt(i));
            strBuilder.append(", ");
            strBuilder.append(vrls.valueAt(i).recvLen);
            strBuilder.append(", ");
            strBuilder.append(vrls.valueAt(i).udpRecvLen);
            strBuilder.append(", ");
            strBuilder.append(vrls.valueAt(i).fecVecSize);
            strBuilder.append("]");
        }

        strBuilder.append(" A_BUF=");
        strBuilder.append(statistics.audioBufferDuration);
        strBuilder.append(" A_RECV=");
        strBuilder.append(statistics.audioRecvDataSize);
        strBuilder.append(" A_WSENT=");
        strBuilder.append(audioModule.getTotalWannaSendBytes());
        strBuilder.append(" A_SENT=");
        strBuilder.append(statistics.audioSentBytes);
        strBuilder.append(" A_CAP=");
        strBuilder.append(statistics.audioCaptureDataSize);
        strBuilder.append(" A_ENC=");
        strBuilder.append(statistics.audioEncodeDataSize);

        strBuilder.append(" A_MUTED=");
        strBuilder.append(audioModule.IsLocalMuted());

        /*
        strBuilder.append(" A_ENC_FRAMES=");
        strBuilder.append(statistics.audioEncodeFrameCount);
        strBuilder.append(" A_DEC_FRAMES=");
        strBuilder.append(statistics.audioDecodeFrameCount);
        */

        strBuilder.append(" A_RECONNECT=");
        strBuilder.append(audioReconnect);
        strBuilder.append(" V_RECONNECT=");
        strBuilder.append(videoReconnect);

        strBuilder.append(" A_RECVERROR=[");
        strBuilder.append(audioModule.getUserErrorTimes());
        strBuilder.append(",");
        strBuilder.append(audioModule.getDataErrorTimes());
        strBuilder.append("]");

        strBuilder.append(" A_MUTEFRAME=");
        strBuilder.append(audioModule.getSizeOfMuteAudioPlayed());

        strBuilder.append(" A_RTT=");
        strBuilder.append(audioModule.getRTT());

        ExternalAudioModule.AudioRecvLen recvLen = audioModule.getRecvLen();
        strBuilder.append(" A_LEN_STATS=[");
        strBuilder.append(recvLen.recvLen);
        strBuilder.append(", ");
        strBuilder.append(recvLen.udpRecvLen);
        strBuilder.append(", ");
        strBuilder.append(recvLen.fecVecSize);
        strBuilder.append("]");

        List<User> users = GlobalHolder.getInstance().getUsers();
        if (users.size() > 0) {
            strBuilder.append(" A_STATS=");
        }
        for (int i = 0; i < users.size(); i++) {
            long userid = users.get(i).getmUserId();
            if (userid != userId) {
                strBuilder.append("[");
                strBuilder.append(userid);
                strBuilder.append(", ");
                strBuilder.append(audioModule.getRecvBytes(userid));
                strBuilder.append(", ");
                strBuilder.append(GlobalHolder.getInstance().isAudioMuted(userid));
                strBuilder.append("]");
            }
        }

        if (audioStatistics != null) {
            if (audioStatistics.size() > 0) {
                strBuilder.append(" A_NETEQ=");
            }
            for (int i = 0; i < audioStatistics.size(); i++) {
                ExternalAudioModule.AudioStatistics as = audioStatistics.valueAt(i);
                strBuilder.append("[");
                strBuilder.append(audioStatistics.keyAt(i));
                strBuilder.append(", ");
                strBuilder.append(as.lossRate);
                strBuilder.append("]");
            }
        }

        ArrayList<Long> speakers = ExternalAudioModule.getInstance().getSpeakers();
        if (speakers != null) {
            if (speakers.size() > 0) {
                strBuilder.append(" SPEAKERS=[");
            }
            for (Long speakerid : speakers) {
                strBuilder.append(speakerid);
                strBuilder.append(",");
            }
            if (speakers.size() > 0) {
                strBuilder.append("]");
            }
        }

        strBuilder.append(" A_PUSH=[");
        strBuilder.append(audioModule.getPushDataBeginCount());
        strBuilder.append(", ");
        strBuilder.append(audioModule.getPushDataEndCount());
        strBuilder.append("]");

        strBuilder.append(" V_PUSH=[");
        strBuilder.append(videoModule.getPushDataBeginCount());
        strBuilder.append(", ");
        strBuilder.append(videoModule.getPushDataEndCount());
        strBuilder.append("]");

        strBuilder.append(" CAP_STAT=[");
        strBuilder.append(audioModule.isCapturing());
        strBuilder.append(", ");
        strBuilder.append(videoModule.isCapturing());
        strBuilder.append("]");

        strBuilder.append(" A_MUTED=");
        strBuilder.append(GlobalConfig.mIsMuteLocalAudio);
        strBuilder.append(" V_MUTED=");
        strBuilder.append(GlobalConfig.mIsMuteLocalVideo);

        strBuilder.append(" MIC_VOL=");
        strBuilder.append(audioModule.getMicVolumeScale());

        last_status_timestamp = System.currentTimeMillis();
        strBuilder.append(" TS=");
        strBuilder.append(last_status_timestamp);
        strBuilder.append(" UUID=");
        strBuilder.append(uuid);

        ReportLogMsg msg = new ReportLogMsg();
        msg.logType = ReportLogMsg.REPORTMSG_TYPE_EVENT;
        msg.logMsg = strBuilder.toString();
        msg.msgType = ReportLogMsg.REPORTMSG_MSG_TYPE_NORMAL;

        Log.e("Status", strBuilder.toString());

        SendLogMsg(msg);
    }

    private void CaculateStatistics(Report_Statistics statistics) {
        if (last_statistics == null) {
            last_statistics = statistics;
            return;
        }

        float drop_frame_rate = 0.0f;
        float drop_bytes_rate = 0.0f;

        int drop_frames_diff = statistics.videoFlowCtrlFrameCount - last_statistics.videoFlowCtrlFrameCount;
        int enc_frames_diff = statistics.videoEncodeFrameCount - last_statistics.videoEncodeFrameCount;

        int drop_bytes_diff = statistics.videoFlowCtrlBytes - last_statistics.videoFlowCtrlBytes;
        int enc_bytes_diff = statistics.videoEncodeDataSize - last_statistics.videoEncodeDataSize;

        if (enc_frames_diff > 0) {
            drop_frame_rate = (float) drop_frames_diff / (float) enc_frames_diff;
        }
        if (enc_bytes_diff > 0) {
            drop_bytes_rate = (float) drop_bytes_diff / (float) enc_bytes_diff;
        }

        if (drop_frame_rate > 0.25f || drop_bytes_rate > 0.25f) {
            Log.d("STATISTICS", "----- [" + drop_frame_rate + "] ----- [" + drop_bytes_rate + "]");
            StringBuilder strBuilder = new StringBuilder("event=STATISTICS_WARNING");
            strBuilder.append(" APPID=");
            strBuilder.append(appid);
            strBuilder.append(" nUserID=");
            strBuilder.append(userId);
            strBuilder.append(" nGroupID=");
            strBuilder.append(sessionId);

            if (drop_frame_rate > 0.25f) {
                strBuilder.append(" V_DROP_FRAME_RATE=");
                strBuilder.append(drop_frame_rate);
            }
            if (drop_bytes_rate > 0.25f) {
                strBuilder.append(" V_DROP_BYTES_RATE=");
                strBuilder.append(drop_bytes_rate);
            }

            strBuilder.append(" TS=");
            strBuilder.append(System.currentTimeMillis());
            strBuilder.append(" UUID=");
            strBuilder.append(uuid);

            ReportLogMsg msg = new ReportLogMsg();
            msg.logType = ReportLogMsg.REPORTMSG_TYPE_DATA;
            msg.logMsg = strBuilder.toString();
            msg.msgType = ReportLogMsg.REPORTMSG_MSG_TYPE_WARNIING;

            SendLogMsg(msg);
        }

        last_statistics = statistics;
    }

    public void ReportAudioRecErr(int error) {
        StringBuilder strBuilder = new StringBuilder("event=A_REC_ERR");
        strBuilder.append(" APPID=");
        strBuilder.append(appid);
        strBuilder.append(" nUserID=");
        strBuilder.append(userId);
        strBuilder.append(" nGroupID=");
        strBuilder.append(sessionId);
        strBuilder.append(" nError=");
        strBuilder.append(error);
        strBuilder.append(" TS=");
        strBuilder.append(System.currentTimeMillis());
        strBuilder.append(" UUID=");
        strBuilder.append(uuid);

        ReportLogMsg msg = new ReportLogMsg();
        msg.logType = ReportLogMsg.REPORTMSG_TYPE_EVENT;
        msg.logMsg = strBuilder.toString();
        msg.msgType = ReportLogMsg.REPORTMSG_MSG_TYPE_NORMAL;

        SendLogMsg(msg);
    }
}
