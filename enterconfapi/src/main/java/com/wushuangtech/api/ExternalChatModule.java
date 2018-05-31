package com.wushuangtech.api;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.wushuangtech.bean.TTTChatModuleContants;
import com.wushuangtech.library.GlobalConfig;
import com.wushuangtech.library.chatlibrary.AliOss;
import com.wushuangtech.library.chatlibrary.MediaRecorderHelper;
import com.wushuangtech.library.chatlibrary.SpeechRecognition;

import java.io.File;

/**
 * Created by wangzhiguo on 18/2/7.
 */

public class ExternalChatModule {

    private static ExternalChatModule holder;
    private AliOss mAliOss;
    private MediaRecorderHelper mMediaRecorderHelper;
    private boolean mIsLiveCapturing = false;

    private ExternalChatModule() {
    }

    public static ExternalChatModule getInstance() {
        if (holder == null) {
            synchronized (ExternalChatModule.class) {
                if (holder == null) {
                    holder = new ExternalChatModule();
                }
            }
        }
        return holder;
    }

    public Object handleActionEvent(int actionType, Object... objs) {
        switch (actionType) {
            case TTTChatModuleContants.ACTION_ALIOSS_INIT:
                initAliOss((Context) objs[0]);
                initMediaRecorder((Context) objs[0]);
                break;
            case TTTChatModuleContants.ACTION_ALIOSS_DOWNLOAD:
                download((Long) objs[0], (String) objs[1], (String) objs[2]);
                break;
            case TTTChatModuleContants.ACTION_SPEECH_RECOGNITION:
                speechRecognition((Context) objs[0], (String) objs[1]);
                break;
            case TTTChatModuleContants.ACTION_MEDIARECORD_START_RECORD:
                startRecord();
                break;
            case TTTChatModuleContants.ACTION_MEDIARECORD_STOP_RECORD:
                return stopAndRelease((long) objs[0], (long) objs[1], (String) objs[2]);
            case TTTChatModuleContants.ACTION_MEDIARECORD_CANNEL:
                cancel();
                break;
            case TTTChatModuleContants.CHAT_SEND_MESSAGE:
                EnterConfApi.getInstance().sendChat((long)objs[0], (int)objs[1], (String)objs[2], (String)objs[3]);
                break;
            case TTTChatModuleContants.CHAT_SEND_SIGNAL:
                EnterConfApi.getInstance().sendSignal((long)objs[0], (String)objs[1], (String)objs[2]);
                break;
            case TTTChatModuleContants.CHAT_AUDIO_STATE:
                return EnterConfApi.getInstance().isChatAudioPlaying();
            case TTTChatModuleContants.CHAT_CANNEL_RECORD_AUDIO:
                EnterConfApi.getInstance().cancleRecordChatAudio();
                break;
            case TTTChatModuleContants.CHAT_PLAY_CHAT_AUDIO:
                EnterConfApi.getInstance().playChatAudio((String)objs[0]);
                break;
            case TTTChatModuleContants.CHAT_RECORD_AUDIO:
                EnterConfApi.getInstance().startRecordChatAudio();
                break;
            case TTTChatModuleContants.CHAT_STOP_CHAT_AUDIO:
                EnterConfApi.getInstance().stopChatAudio();
                break;
            case TTTChatModuleContants.CHAT_STOP_RECORD_AUDIO_AND_SEND:
                return EnterConfApi.getInstance().stopRecordAndSendChatAudio((long)objs[0], (String)objs[1]);

        }
        return null;
    }

    private void initAliOss(Context context) {
        mAliOss = new AliOss(context);
    }

    private void initMediaRecorder(Context context) {
        GlobalConfig.mChatSendPath = context.getFilesDir() + File.separator + "Send" + File.separator;
        File sendFile = new File(GlobalConfig.mChatSendPath);
        if (!sendFile.exists()) {
            sendFile.mkdirs();
        }

        GlobalConfig.mChatReceivePath = context.getFilesDir() + File.separator + "Receive" + File.separator;
        File receivedFile = new File(GlobalConfig.mChatReceivePath);
        if (!receivedFile.exists()) {
            receivedFile.mkdirs();
        }

        mMediaRecorderHelper = new MediaRecorderHelper(context);
    }

    private void download(long nSrcUserID, String sSeqID, String s) {
        mAliOss.download(nSrcUserID, sSeqID, s);
    }

    private void speechRecognition(Context context, String path) {
        SpeechRecognition speechRecognition = new SpeechRecognition(context);
        speechRecognition.startRecognition(path);
    }

    private void startRecord() {
        mIsLiveCapturing = ExternalAudioModule.getInstance().isCapturing();
        if (mIsLiveCapturing) {
            ExternalAudioModule.getInstance().StopCapture();
        }
        if (mMediaRecorderHelper != null)
            mMediaRecorderHelper.startRecord();
    }

    private int stopAndRelease(long mConfId, long nDstUserID, String sSeqID){
        if (mIsLiveCapturing)
            ExternalAudioModule.getInstance().StartCapture();
        if (mMediaRecorderHelper != null)
            return mMediaRecorderHelper.stopAndRelease(mConfId, nDstUserID, sSeqID);
        return -1;
    }

    private void cancel() {
        if (mMediaRecorderHelper != null)
            mMediaRecorderHelper.cancel();
    }
}
