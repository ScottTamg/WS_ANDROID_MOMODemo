package com.wushuangtech.api;

import android.util.LongSparseArray;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.ArrayList;


public class ExternalAudioModule implements AudioSender {

    public static class AudioRecvLen {
        public int recvLen;
        public int udpRecvLen;
        public int fecVecSize;
    }

    public static class AudioStatistics {
        public int lossRate;
    }

    private static ExternalAudioModule mExternalAudioModule;
    private WeakReference<ExternalAudioModuleCallback> mCallback;
    private AudioRecvLen audioRecvLen = new AudioRecvLen();

    private ByteBuffer receivedBuffer = null;

    /**
     *  获取api对象实例，singleton
     *
     *  @return 对象实例
     */
    public static synchronized ExternalAudioModule getInstance() {
        if (mExternalAudioModule == null) {
            synchronized (ExternalAudioModule.class) {
                if (mExternalAudioModule == null) {
                    mExternalAudioModule = new ExternalAudioModule();
                    mExternalAudioModule.Initialize(mExternalAudioModule);
                    mExternalAudioModule.nativeCachDirectBufferAddress();
                }
            }
        }
        return mExternalAudioModule;
    }

    /**
     * 设置音频模块
     *
     * @param callback  音频模块实例
     */
    public void setExternalAudioModuleCallback(ExternalAudioModuleCallback callback)
    {
        mCallback = new WeakReference<>(callback);
    }

    /**
     * 获取音频时间戳
     *
     * @return  时间戳
     */
    public long getAudioTimestamp(long userid)
    {
        return GetAudioTimestamp(userid);
    }

    /**
     * 设置回声消除参数
     *
     * @param delayOffset   延迟参数
     */
    public void setDelayOffsetMS(int delayOffset)
    {
        if (mCallback != null && mCallback.get() != null)
        {
            mCallback.get().setDelayoffset(delayOffset);
        }
    }

    /**
     * 获取音频发送缓冲区大小
     *
     * @return  缓冲区大小，单位毫秒
     */
    public int getBufferDuration() { return GetBufferDuration(); }

    /**
     * 后去发送字节数
     *
     * @return  字节数
     */
    public int getTotalSendBytes()
    {
        return GetTotalSendBytes();
    }

    /**
     * 获取接收字节数
     *
     * @return  字节数
     */
    public int getTotalRecvBytes() { return GetTotalRecvBytes(); }

    /**
     * 获取接收字节数
     *
     */
    public int getRecvBytes(long userid) {
        if (mCallback != null && mCallback.get() != null) {
            return mCallback.get().getRecvBytes(userid);
        }
        return 0;
    }

    /**
     * 获取采集字节数
     *
     * @return  字节数
     */
    public int getCaptureDataSzie() {
        if (mCallback != null && mCallback.get() != null) {
            return mCallback.get().getCaptureDataSize();
        }
        return 0;
    }

    /**
     * 获取编码字节数
     *
     * @return  字节数
     */
    public int getEncodeDataSize() {
        if (mCallback != null && mCallback.get() != null) {
            return mCallback.get().getEncodeDataSize();
        }
        return 0;
    }

    /**
     * 获取编码帧数
     *
     * @return  帧数
     */
    public int getEncodeFrameCount() {
        if (mCallback != null && mCallback.get() != null) {
            return mCallback.get().getEncodeFrameCount();
        }
        return 0;
    }

    /**
     * 获取解码帧数
     *
     * @return  帧数
     */
    public int getDecodeFrameCount() {
        if (mCallback != null && mCallback.get() != null) {
            return mCallback.get().getDecodeFrameCount();
        }
        return 0;
    }

    /*
    public void saveDelayOffset(int delayOffset) {
        if (mCallback != null && mCallback.get() != null) {
            mCallback.get().saveDelayOffset(delayOffset);
        }
    }
    */

    public int getSizeOfMuteAudioPlayed() {
        if (mCallback != null && mCallback.get() != null) {
            return mCallback.get().getSizeOfMuteAudioPlayed();
        }
        return 0;
    }

    public int getRTT() {
        return GetRTT();
    }

    public int getTotalWannaSendBytes () { return GetTotalWannaSendBytes(); }

    // for debug
    public AudioRecvLen getRecvLen() {
        GetAudioStats();
        return audioRecvLen;
    }

    public int getUserErrorTimes() {
        return GetUserErrorTimes();
    }
    public int getDataErrorTimes() {
        return GetDataErrorTimes();
    }

    // for debug
    private int pushDataBegin_count = 0;
    private int pushDataEnd_count = 0;
    public int getPushDataBeginCount() {
        return pushDataBegin_count;
    }
    public int getPushDataEndCount() {
        return pushDataEnd_count;
    }
    public boolean isCapturing() {
        return mCallback != null && mCallback.get() != null && mCallback.get().isCapturing();
    }

    /**
     * 由音频模块调用，发送音频数据
     *
     * @param data  aac
     */
    public void pushEncodedAudioData(byte[] data)
    {
        pushDataBegin_count++;
        PushEncodedAudioData(data, data.length);
        pushDataEnd_count++;
    }

    public LongSparseArray<AudioStatistics> getAudioStatistics() {
        if (mCallback != null && mCallback.get() != null) {
            return mCallback.get().getRemoteAudioStatistics();
        } else {
            return null;
        }
    }

    @Override
    public void sendSRData(byte[] data, int len) {
        SendSRData(data,len);
    }

    @Override
    public void sendNACKData(byte[] data, int len, long userid) {
        //Log.e("--------","sendNACKData");
        SendNACKData(data,len,userid);
    }

    private void nativeCachDirectBufferAddress()
    {
        receivedBuffer = ByteBuffer.allocateDirect(960); // enough buffer for 48khz, 10ms, stereo
        NativeCachDirectBufferAddress(receivedBuffer);
    }

    private native boolean Initialize(ExternalAudioModule module);
    private native void Uninitialize();
    private native long GetAudioTimestamp(long userid);
    private native int GetTotalSendBytes();
    private native int GetTotalRecvBytes();
    private native int GetBufferDuration();
    private native void PushEncodedAudioData(byte[] data, int len);
    private native void SendNACKData(byte[] data,int len ,long userid);
    private native void SendSRData(byte[] data, int len);
    private native int GetRTT();
    private native int GetTotalWannaSendBytes();
    private native void GetAudioStats();
    private native void NativeCachDirectBufferAddress(ByteBuffer receivedBuffer);
    private native int GetUserErrorTimes();
    private native int GetDataErrorTimes();

    public void unInitialize(){
        Uninitialize();
    }

    boolean StartCapture() {
        return mCallback != null && mCallback.get() != null && mCallback.get().startCapture();
    }

    boolean StopCapture() {
        return mCallback != null && mCallback.get() != null && mCallback.get().stopCapture();
    }

    void EnableAudio(boolean enable) {
        if (mCallback != null && mCallback.get() != null) {
            mCallback.get().pauseRecordOnly(!enable);
        }
    }

    private boolean StartPlay(long userid) {
        return mCallback != null && mCallback.get() != null && mCallback.get().startPlay(userid);
    }

    private boolean StopPlay(long userid) {
        return mCallback != null && mCallback.get() != null && mCallback.get().stopPlay(userid);
    }

    private boolean ReceiveAudioData(long userid, int len) {
        /*
        if (stream == null) {
            String sdStatus = Environment.getExternalStorageState();
            if (!sdStatus.equals(Environment.MEDIA_MOUNTED)) {
                return;
            }
            try {
                String pathName = "/sdcard/test/";
                String fileName = "audio.aac";
                File path = new File(pathName);
                File file = new File(pathName + fileName);
                if (!path.exists()) {
                    path.mkdir();
                }
                if (!file.exists()) {
                    file.createNewFile();
                }
                stream = new FileOutputStream(file);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        */

        //int seq = 0 + (data[2] << 8) + data[3];
        //long ts = (data[4] << 24) + (data[5] << 16) + (data[6] << 8) + data[7];
        //Log.e("ReceiveAudioData", "type is [" + (data[1] & 0x7F) + "] seq is [" + seq + "] ts is [" + ts + "] len is [" + data.length + "]");

        /*
        byte[] bs = new byte[data.length -12];
        for (int i = 0; i < data.length - 12; i++)
        {
            bs[i] = data[i+12];
        }

        try {
            stream.write(bs);
        } catch (Exception e) {
            e.printStackTrace();
        }
        */

        return mCallback != null && mCallback.get() != null && mCallback.get().receiveAudioData(userid, receivedBuffer, len);
    }

    private boolean RecvRTCPMessage(byte[] rtcp_package, int len)
    {
        return mCallback != null &&
                mCallback.get() != null &&
                mCallback.get().receiveRTCPMessage(rtcp_package, len);
    }

    private void SetSendCodec(int type, int bitrate)
    {
        if (mCallback != null && mCallback.get() != null) {
            mCallback.get().setSendCodec(type, bitrate);
        }
    }

    private int GetDelayEstimate(long userid)
    {
        if (mCallback != null && mCallback.get() != null) {
            return mCallback.get().getDelayEstimate(userid);
        }
        return 0;
    }

    private void SetSleepMS(long userid, int ts)
    {
        if (mCallback != null && mCallback.get() != null) {
            mCallback.get().setSleepMS(userid, ts);
        }
    }

    private int GetSpeechInputAudioLevel()
    {
        if (mCallback != null && mCallback.get() != null) {
            return mCallback.get().getSpeechInputLevel();
        }
        return 0;
    }

    private int GetSpeechInputAudioLevelFullRange()
    {
        if (mCallback != null && mCallback.get() != null) {
            return mCallback.get().getSpeechInputLevelFullRange();
        }
        return 0;
    }

    private int GetSpeechOutputAudioLevel(long userid)
    {
        if (mCallback != null && mCallback.get() != null) {
            return mCallback.get().getSpeechOutputLevel(userid);
        }
        return 0;
    }

    private int GetSpeechOutputAudioLevelFullRange(long userid)
    {
        if (mCallback != null && mCallback.get() != null) {
            return mCallback.get().getSpeechOutputLevelFullRange(userid);
        }
        return 0;
    }

    private  void OnReportAudioStats(int recvLen, int udpRecvLen, int fecVecSize) {
        audioRecvLen.recvLen = recvLen;
        audioRecvLen.udpRecvLen = udpRecvLen;
        audioRecvLen.fecVecSize = fecVecSize;
    }

    private void MuteLocal(boolean mute)
    {
        if (mCallback != null && mCallback.get() != null) {
            mCallback.get().muteLocal(mute);
        }
    }

    public boolean IsLocalMuted() {
        return mCallback != null && mCallback.get() != null && mCallback.get().isLocalMuted();
    }

    private void RemoteAudioMuted(boolean muted, long userid)
    {
        if (mCallback != null && mCallback.get() != null) {
            mCallback.get().remoteAudioMuted(muted, userid);
        }
    }

    private void ReplayUsingVoip(boolean useVoip) {
        if (mCallback != null && mCallback.get() != null) {
            mCallback.get().restartPlayUseVoip(useVoip);
        }
    }

    public ArrayList<Long> getSpeakers() {
        if (mCallback != null && mCallback.get() != null) {
            return mCallback.get().getSpeakers();
        }

        return null;
    }

    public float getMicVolumeScale() {
        if (mCallback != null && mCallback.get() != null) {
            return mCallback.get().audioSoloVolumeScale();
        }

        return 1.0f;
    }
}
