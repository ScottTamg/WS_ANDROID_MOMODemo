package com.wushuangtech.audiocore;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.util.LongSparseArray;

import com.wushuangtech.api.AudioSender;
import com.wushuangtech.api.EnterConfApi;
import com.wushuangtech.api.ExternalAudioModule;
import com.wushuangtech.api.ExternalAudioModuleCallback;
import com.wushuangtech.library.GlobalConfig;
import com.wushuangtech.library.GlobalHolder;

import org.webrtc.voiceengine.WebRtcAudioRecord;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class MyAudioApi implements ExternalAudioModuleCallback {

    private static final int MYAUDIO_INITIALIZE = 0;
    private static final int MYAUDIO_START_CAPTURE = 1;
    private static final int MYAUDIO_STOP_CAPTURE = 2;
    private static final int MYAUDIO_START_PLAY = 3;
    private static final int MYAUDIO_STOP_PLAY = 4;
    private static final int MYAUDIO_PAUSE = 5;
    private static final int MYAUDIO_RESUME = 6;
    private static final int MYAUDIO_SET_HEADSET_STATUS = 7;
    private static final int MYAUDIO_SET_DELAY_OFFSET = 8;
    private static final int MYAUDIO_PAUSE_RECORD = 9;
    private static final int MYAUDIO_RESUME_RECORD = 10;
    private static final int MYAUDIO_MUTE_LOCAL = 11;
    private static final int MYAUDIO_RESTARTPLAYBACK = 12;


    private static final int REVERSED_USERID_FOR_FILE_PLAY = 0xFFFFFFFF;
    private static final int PREFERED_OUTPUT_BYTES_UNCHANGE = -1;
    private static final int PREFERED_SAMPLE_RATE_UNCHANGE = -1;


    private static MyAudioApi mAudioApi;

    private Context mContext;

    private WeakReference<ExternalAudioProcessCallback> mProcessCallback;

    private List<WeakReference<AudioSender>> mAudioSenders = new ArrayList<WeakReference<AudioSender>>();

    private ArrayList<Long> mSpeakers = new ArrayList<>();

    private  Handler threadHandler;

    private int encodeDataSize = 0;
    private int encodeFrameSize = 0;
    private int decodeFarmeSize = 0;

    private boolean recordMixing = false;
    private boolean playMixing = false;
    private ByteBuffer recordBuffer = null;
    private ByteBuffer playBuffer = null;
    private boolean audioFileMixing = false;
    private float audioFileVolumeScale = 0.3f;
    private WeakReference<AudioDecoderModule> audioFileMixer;

    private boolean earsBackEnabled = false;
    private boolean localAudioMuted = false;
    private boolean aecDelayAgnosticEnabled = false;

    private float micVolumeScale = 1.0f;

    private LongSparseArray<ExternalAudioModule.AudioStatistics> mAudioStatistics = new LongSparseArray<>();

    private MyAudioApi(Context ctx) {
        System.loadLibrary("myaudio_so");

        mContext = ctx;
        //mContext = ctx.getApplicationContext();

        HandlerThread handlerThread = new HandlerThread("myaudioapi");
        handlerThread.start();

        threadHandler = new Handler(handlerThread.getLooper()) {

            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case MYAUDIO_INITIALIZE:
                        Initialize(mAudioApi, mContext);
                        nativeCachDirectBufferAddress();
//                        enableAecDelayAgnostic(true);
                        break;
                    case MYAUDIO_START_CAPTURE:
                        encodeDataSize = 0;
                        encodeFrameSize = 0;
                        WebRtcAudioRecord.capturedDataSize = 0;
                        GlobalConfig.mIsMuteAudioCapture = false;
                        StartCapture();
                        break;
                    case MYAUDIO_STOP_CAPTURE:
                        GlobalConfig.mIsMuteAudioCapture = true;
                        StopCapture();
                        break;
                    case MYAUDIO_START_PLAY:
                        StartPlay((Long)msg.obj);
                        synchronized (this) {
                            if (!mSpeakers.contains(msg.obj))
                                mSpeakers.add((Long) msg.obj);
                        }
                        break;
                    case MYAUDIO_STOP_PLAY:
                        StopPlay((Long)msg.obj);
                        synchronized (this) {
                            mSpeakers.remove(msg.obj);
                        }
                        break;
                    case MYAUDIO_PAUSE:
                        PauseAudio();
                        break;
                    case MYAUDIO_RESUME:
                        ResumeAudio();
                        break;
                    case MYAUDIO_SET_HEADSET_STATUS:
                        SetHeadSetPlugStatus(msg.arg1 == 1);
                        break;
                    case MYAUDIO_SET_DELAY_OFFSET:
                        SetDelayOffsetMS(msg.arg1);
                        break;
                    case MYAUDIO_PAUSE_RECORD:
                        PauseRecordOnly(true);
                        break;
                    case MYAUDIO_RESUME_RECORD:
                        PauseRecordOnly(false);
                        break;
                    case MYAUDIO_MUTE_LOCAL:
                        MuteLocal(msg.arg1 == 1);
                        break;
                    case MYAUDIO_RESTARTPLAYBACK:
                        RestartPlayUseVoip(msg.arg1 == 1);
                        break;
                    default:
                        break;
                }
            }
        };
    };

    public static synchronized MyAudioApi getInstance(Context ctx) {
        if (mAudioApi == null) {
            synchronized (MyAudioApi.class) {
                if (mAudioApi == null) {
                    mAudioApi = new MyAudioApi(ctx);
                    mAudioApi.threadHandler.sendEmptyMessage(MYAUDIO_INITIALIZE);
                }
            }
        }
        return mAudioApi;
    }

    public static synchronized MyAudioApi getInstance() {
        return mAudioApi;
    }

    public void setExternalAudioProcessCallback(ExternalAudioProcessCallback callback)
    {
        mProcessCallback = new WeakReference<>(callback);
    }

    public void addAudioSender(AudioSender sender)
    {
        boolean alreadyExist = false;

        for (WeakReference<AudioSender> audioSender : mAudioSenders)
        {
            if (audioSender.get() == sender)
            {
                alreadyExist = true;
                break;
            }
        }

        if (!alreadyExist) {
            WeakReference<AudioSender> audioSender = new WeakReference<>(sender);
            mAudioSenders.add(audioSender);
        }
    }

    public void removeAudioSender(AudioSender sender)
    {
        for (WeakReference<AudioSender> audioSender : mAudioSenders)
        {
            if (audioSender.get() == sender)
            {
                mAudioSenders.remove(audioSender);
                break;
            }
        }
    }

    public boolean startCapture()
    {
        threadHandler.sendEmptyMessage(MYAUDIO_START_CAPTURE);
        return true;
    }

    public boolean stopCapture()
    {
        threadHandler.sendEmptyMessage(MYAUDIO_STOP_CAPTURE);
        return true;
    }

    public boolean startPlay(long userid)
    {
        Message msg = new Message();
        msg.what = MYAUDIO_START_PLAY;
        msg.obj = userid;
        threadHandler.sendMessage(msg);

        return true;
    }

    public boolean stopPlay(long userid)
    {
        Message msg = new Message();
        msg.what = MYAUDIO_STOP_PLAY;
        msg.obj = userid;
        threadHandler.sendMessage(msg);

        return true;
    }

    public boolean pauseAudio()
    {
        threadHandler.sendEmptyMessage(MYAUDIO_PAUSE);
        return true;
    }

    public boolean resumeAudio()
    {
        threadHandler.sendEmptyMessage(MYAUDIO_RESUME);
        return true;
    }

    private void operaFileData(byte[] by, int offset, int count) {
        String fileName = "/storage/emulated/0/mixed_ori_1.pcm";
        FileOutputStream fileout = null;
        File file = new File(fileName);
        try {
            fileout = new FileOutputStream(file, true);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            fileout.write(by, offset, count);
            fileout.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                fileout.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean receiveAudioData(long userid, ByteBuffer buffer, int len)
    {
        decodeFarmeSize++;
        return ReceiveAudioData(userid, buffer, len);
    }

    @Override
    public boolean receiveRTCPMessage(byte[] rtcp_package, int len) {
        return ReceiveRTCPMessage(rtcp_package,len);
    }

    public void setDelayoffset(int offset) {
        if (!aecDelayAgnosticEnabled) {
            Message msg = new Message();
            msg.what = MYAUDIO_SET_DELAY_OFFSET;
            msg.arg1 = offset;
            threadHandler.sendMessage(msg);
        }
    }

    public void setHeadsetStatus(boolean pluged) {
        Message msg = new Message();
        msg.what = MYAUDIO_SET_HEADSET_STATUS;
        msg.arg1 = pluged?1:0;
        threadHandler.sendMessage(msg);
    }

    public void muteLocal(boolean mute) {
        localAudioMuted = mute;
        Message msg = new Message();
        msg.what = MYAUDIO_MUTE_LOCAL;
        msg.arg1 = mute?1:0;
        threadHandler.sendMessage(msg);
    }


    public boolean isLocalMuted() {
        return localAudioMuted;
        //return IsLocalMute();
    }


    public void remoteAudioMuted(boolean muted, long userid) {
        RemoteAudioMuted(userid, muted);
    }

    public void restartPlayUseVoip(boolean useVoip) {
        Message msg = new Message();
        msg.what = MYAUDIO_RESTARTPLAYBACK;
        msg.arg1 = useVoip?1:0;
        threadHandler.sendMessage(msg);
    }

    public int getCaptureDataSize() {
        return WebRtcAudioRecord.capturedDataSize;
    }

    public int getEncodeDataSize() {
        return encodeDataSize;
    }
    public int getEncodeFrameCount() { return encodeFrameSize; }
    public int getDecodeFrameCount() { return decodeFarmeSize; }

    public void setAudioFileMixCallback(AudioFileMixCallback callback)
    {
        if (audioFileMixer == null) {
            audioFileMixer = new WeakReference(AudioDecoderModule.createInstance());
        }

        if (audioFileMixer.get() != null) {
            audioFileMixer.get().setAudioDecoderModuleCallback(callback);
        }
    }

    public boolean startAudioFileMixing(String filename, boolean loopback, int loopTimes) {
        stopAudioFileMixing();

        if (loopTimes < 0) {
            return false;
        }

        if (audioFileMixer == null) {
            audioFileMixer = new WeakReference(AudioDecoderModule.createInstance());
        }

        if (audioFileMixer.get() != null) {
            audioFileMixing =  audioFileMixer.get().startDecode(filename , 1, 32000, loopback, loopTimes);

            if (audioFileMixing) {
                startPlay(REVERSED_USERID_FOR_FILE_PLAY);
                EnableRecordMix(true, PREFERED_OUTPUT_BYTES_UNCHANGE, PREFERED_SAMPLE_RATE_UNCHANGE);
                EnablePlayMix(true, PREFERED_OUTPUT_BYTES_UNCHANGE, PREFERED_SAMPLE_RATE_UNCHANGE);
            }
        }

        return audioFileMixing;
    }

    public void stopAudioFileMixing() {
        if (audioFileMixing) {
            if (audioFileMixer != null && audioFileMixer.get() != null) {
                audioFileMixer.get().stopDecode();
            }
            audioFileMixing = false;

            if (!recordMixing) {
                EnableRecordMix(false, PREFERED_OUTPUT_BYTES_UNCHANGE, PREFERED_SAMPLE_RATE_UNCHANGE);
            }
            if (!playMixing) {
                EnablePlayMix(false, PREFERED_OUTPUT_BYTES_UNCHANGE, PREFERED_SAMPLE_RATE_UNCHANGE);
            }

            boolean needPlayback = playMixing || audioFileMixing || earsBackEnabled;
            if (!needPlayback)
            {
                stopPlay(REVERSED_USERID_FOR_FILE_PLAY);
            }
        }
    }

    public void pauseAudioFileMix() {
        if (audioFileMixer != null && audioFileMixer.get() != null) {
            audioFileMixer.get().pause();
        }
    }

    public void resumeAudioFileMix() {
        if (audioFileMixer != null && audioFileMixer.get() != null) {
            audioFileMixer.get().resume();
        }
    }

    public void seekAudioFileTo(int seconds) {
        if (audioFileMixer != null && audioFileMixer.get() != null) {
            audioFileMixer.get().seekTo(seconds);
        }
    }

    public boolean audioFileMixing() {
        return audioFileMixer != null && audioFileMixer.get() != null && audioFileMixer.get().mixing();
    }

    public float audioSoloVolumeScale() {
        return micVolumeScale;
    }

    public void adjustAudioSoloVolumeScale(float scale) {
        if (scale < 0.0f || scale > 3.0f) {
            return;
        }
        micVolumeScale = scale;
        AdjustMicVolumeScale(scale);
    }

    public float audioFileVolumeScale() {
        return audioFileVolumeScale;
    }

    public void adjustAudioFileVolumeScale(float scale) {
        if (scale < 0.0f || scale > 1.0f) {
            return;
        }
        audioFileVolumeScale = scale;
    }

    public boolean startRecordMix(int preferedOutputBytes, int preferedSampleRate) {
        recordMixing = true;
        EnableRecordMix(true, preferedOutputBytes, preferedSampleRate);
        return true;
    }

    public boolean stopRecordMix() {
        recordMixing = false;
        if (!audioFileMixing) {
            EnableRecordMix(false, PREFERED_OUTPUT_BYTES_UNCHANGE, PREFERED_SAMPLE_RATE_UNCHANGE);
        }
        return true;
    }

    public boolean startPlayMix(int preferedOutputBytes, int preferedSampleRate) {
        playMixing = true;
        startPlay(REVERSED_USERID_FOR_FILE_PLAY);
        EnablePlayMix(playMixing, preferedOutputBytes, preferedSampleRate);
        return true;
    }

    public boolean stopPlayMix() {
        playMixing = false;
        if (!audioFileMixing) {
            EnablePlayMix(false, PREFERED_OUTPUT_BYTES_UNCHANGE, PREFERED_SAMPLE_RATE_UNCHANGE);
        }

        boolean needPlayback = playMixing || audioFileMixing || earsBackEnabled;
        if (!needPlayback)
        {
            stopPlay(REVERSED_USERID_FOR_FILE_PLAY);
        }

        return true;
    }

    public void setSendCodec(int type, int bitrate)
    {
        SetSendCodec(type, bitrate);
    }

    public int getDelayEstimate(long userid)
    {
        return GetDelayEstimate(userid);
    }

    public void setSleepMS(long userid, int ts)
    {
        SetSleepMS(userid, ts);
    }

    public int getSizeOfMuteAudioPlayed()
    {
        return GetAudioErrorTimes();
    }

    public int getSpeechInputLevel() {
        return GetSpeechInputLevel();
    }

    public int getSpeechInputLevelFullRange() {
        return GetSpeechInputLevelFullRange();
    }

    public int getSpeechOutputLevel(long userid)
    {
        return GetSpeechOutputLevel(userid);
    }

    public int getSpeechOutputLevelFullRange(long userid)
    {
        return GetSpeechOutputLevelFullRange(userid);
    }

    public int getRecvBytes(long userid)
    {
        return GetRecvBytes(userid);
    }

    public void enableEarsBack(boolean enable) {
        earsBackEnabled = enable;
        EnableEarBack(earsBackEnabled);
        if (earsBackEnabled) {
            startPlay(REVERSED_USERID_FOR_FILE_PLAY);
        }

        boolean needPlayback = playMixing || audioFileMixing || earsBackEnabled;
        if (!needPlayback)
        {
            stopPlay(REVERSED_USERID_FOR_FILE_PLAY);
        }
    }

    public ArrayList<Long> getSpeakers() {
        ArrayList<Long> ret = new ArrayList<>();
        synchronized (this) {
            ret.addAll(mSpeakers);
        }

        return ret;
    }

    public void pauseRecordOnly(boolean pause) {
        if (pause) {
            threadHandler.sendEmptyMessage(MYAUDIO_PAUSE_RECORD);
        } else {
            threadHandler.sendEmptyMessage(MYAUDIO_RESUME_RECORD);
        }
    }

    public boolean isCapturing() {
        return WebRtcAudioRecord.isCapturing;
    }

    public void enableAecDelayAgnostic(boolean enable) {
        aecDelayAgnosticEnabled = enable;
        EnableAecDelayAgnostic(enable);
    }

    public LongSparseArray<ExternalAudioModule.AudioStatistics> getRemoteAudioStatistics() {
        mAudioStatistics.clear();
        GetAudioStatistics();
        return mAudioStatistics;
    }

    private void nativeCachDirectBufferAddress() {
        recordBuffer = ByteBuffer.allocateDirect(4096); // enough buffer for 48khz, 10ms, stereo
        playBuffer = ByteBuffer.allocateDirect(4096); // enough buffer for 48khz, 10ms, stereo
        NativeCachDirectBufferAddress(recordBuffer, playBuffer);
    }

    private native boolean Initialize(MyAudioApi api, Context ctx);
    private native boolean StartCapture();
    private native boolean StopCapture();
    private native boolean StartPlay(long userid);
    private native boolean StopPlay(long userid);
    private native boolean PauseAudio();
    private native boolean ResumeAudio();
    private native boolean ReceiveAudioData(long userid, ByteBuffer buffer, int len);
    private native void SetDelayOffsetMS(int offset);
    private native void SetHeadSetPlugStatus(boolean isPluged);
    private native void SetSendCodec(int type, int bitrate);
    private native int GetDelayEstimate(long userid);
    private native void SetSleepMS(long userid, int ts);
    private native int GetAudioErrorTimes();
    private native int GetRecvBytes(long userid);

    private native int GetSpeechInputLevel();
    private native int GetSpeechInputLevelFullRange();
    private native int GetSpeechOutputLevel(long userid);
    private native int GetSpeechOutputLevelFullRange(long userid);

    private native void NativeCachDirectBufferAddress(ByteBuffer recordByffer, ByteBuffer playBuffer);
    private native void EnableRecordMix(boolean enable, int preferedOutputBytes, int preferedSampleRate);
    private native void EnablePlayMix(boolean enable, int preferedOutputBytes, int preferedSampleRate);
    private native void EnableEarBack(boolean enable);
    private native void PauseRecordOnly(boolean pause);
    private native void MuteLocal(boolean mute);
    private native boolean IsLocalMute();
    private native void RecordPCMDataProccessed();
    private native void RemoteAudioMuted(long userid, boolean muted);
    private native void RestartPlayUseVoip(boolean useVoip);

    private native void AdjustMicVolumeScale(double volumeScale);

    private native void EnableAecDelayAgnostic(boolean enable);
    private native boolean ReceiveRTCPMessage(byte[] rtcp_message, int len);
    private native void GetAudioStatistics();

    private void OnRecordPCMData(int sizeInBytes, int samplingFreq, boolean isStereo)
    {
        if (recordMixing) {
            if (mProcessCallback != null && mProcessCallback.get() != null) {
                mProcessCallback.get().onRecordPCMData(recordBuffer.array(), recordBuffer.arrayOffset(), sizeInBytes, samplingFreq, isStereo);
            }
        }

        RecordPCMDataProccessed();

        if (audioFileMixing) {
            if (audioFileMixer != null && audioFileMixer.get() != null) {
                audioFileMixer.get().mixPCM(recordBuffer, sizeInBytes, 1.0f, audioFileVolumeScale, false, samplingFreq);
            }
        }

        //operaFileData(recordBuffer.array(), recordBuffer.arrayOffset(), sizeInBytes);

        recordBuffer.rewind();
    }

    private void OnPlaybackPCMData(int sizeInBytes, int samplingFreq, boolean isStereo) {
        if (audioFileMixing) {
            if (audioFileMixer != null && audioFileMixer.get() != null) {
                audioFileMixer.get().mixPCM(playBuffer, sizeInBytes, 1.0f, audioFileVolumeScale, true, samplingFreq);
            }
        }

        if (playMixing) {
            if (mProcessCallback != null && mProcessCallback.get() != null) {
                mProcessCallback.get().onPlaybackPCMData(playBuffer.array(), playBuffer.arrayOffset(), sizeInBytes, samplingFreq, isStereo);
            }
        }

//        operaFileData(playBuffer.array(), playBuffer.arrayOffset(), sizeInBytes);

        playBuffer.rewind();
    }

    private void EncodedAudio(byte[] data)
    {
        encodeFrameSize++;
        encodeDataSize += data.length;
        GlobalHolder.getInstance().notifyAudioDataToWrite(data);
        for (WeakReference<AudioSender> audioSender : mAudioSenders)
        {
            if (audioSender.get() != null)
            {
                audioSender.get().pushEncodedAudioData(data);
            }
        }
    }

    private void OnSendNACKData(byte[] data,int len ,long userid)
    {
        for (WeakReference<AudioSender> audioSender : mAudioSenders)
        {
            if (audioSender.get() != null)
            {
                audioSender.get().sendNACKData(data,len,userid);
            }
        }
    }

    private void OnSendSRData(byte[] data,int len)
    {
        for (WeakReference<AudioSender> audioSender : mAudioSenders)
        {
            if (audioSender.get() != null)
            {
                audioSender.get().sendSRData(data,len);
            }
        }
    }

    public void onError(int code) {
        EnterConfApi.getInstance().reportAudioRecError(code);
    }

    private void OnGetAudioSatistics(long nUserID, int lossRate) {
        ExternalAudioModule.AudioStatistics as = new ExternalAudioModule.AudioStatistics();
        as.lossRate = lossRate;
        mAudioStatistics.append(nUserID, as);
    }
}
