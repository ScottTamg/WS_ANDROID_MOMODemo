package com.wushuangtech.audiocore;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import android.util.Log;

import com.wushuangtech.library.GlobalConfig;

import static android.R.attr.duration;

class AudioDecoderModule{
    private  long pJniModule;
    private WeakReference<AudioFileMixCallback> mCallback;

    /**
     *  获取api对象实例，singleton
     *
     *  @return 对象实例
     */
    static synchronized AudioDecoderModule createInstance() {
        AudioDecoderModule pAudioDecoderModule = new AudioDecoderModule();
        pAudioDecoderModule.pJniModule = pAudioDecoderModule.Initialize(pAudioDecoderModule);
        return pAudioDecoderModule;
    }

    void setAudioDecoderModuleCallback(AudioFileMixCallback callback)
    {
        mCallback = new WeakReference<AudioFileMixCallback>(callback);
    }

    boolean startDecode(String filename, int ch, int samplerate, boolean loopback, int loopTimes)
    {
        if( StartDecode(pJniModule, filename, ch, loopback, loopTimes))
        {
            return true;
        }
        else {
            return false;
        }
    }

    boolean stopDecode()
    {
        return StopDecode(pJniModule);
    }

    //混音接口，buf中输入原音，返回true后，buf中为加入伴奏后的声音。f为伴奏权重，0.0-1.0。
    //f为0.0时，混音后还是原音； 为1.0时，混音后声音全部是伴奏音。
    boolean mixPCM(ByteBuffer buf, int len, double f1, double f2, boolean loopback, int samplerate)
    {
        return MixPCM(pJniModule, buf, len, f1, f2, loopback, samplerate);
    }

    void pause()
    {
        Pause(pJniModule);
    }

    void resume()
    {
        Resume(pJniModule);
    }

    void seekTo(int seconds)
    {
        SeekTo(pJniModule, seconds);
    }

    boolean mixing()
    {
        return Mixing(pJniModule);
    }

    private native long Initialize(AudioDecoderModule module);
    private native void Uninitialize(long p);
    private native boolean StartDecode(long p, String filename, int ch, boolean loopback, int loopTimes);
    private native boolean StopDecode(long p);
    private native boolean MixPCM(long p, ByteBuffer buf, int len, double f1, double f2, boolean loopback, int samplerate);
    private native void Pause(long p);
    private native void Resume(long p);
    private native void SeekTo(long p, int seconds);
    private native boolean Mixing(long p);
    private native int PtsMiliSencod(long p);

    private void OnAudioDecoderStatus( int status)
    {
        if (mCallback != null && mCallback.get() != null)
        {
            AudioFileMixCallback.AudioFileMixStatus statustype = AudioFileMixCallback.AudioFileMixStatus.AudioFileMixStatus_eof;
            mCallback.get().OnAudioDecoderStatus(statustype);
        }
    }

    private void OnReportFileDuration(int duration)
    {
        GlobalConfig.mCurrentAudioMixingDuration = duration;
        if (mCallback != null && mCallback.get() != null)
        {
            mCallback.get().OnReportFileDuration(duration);
        }
    }

    private void OnReportFileDurationMs(int durationMs)
    {

    }

    private void OnReportPlayoutSeconds(int seconds)
    {
        GlobalConfig.mCurrentAudioMixingPosition = seconds;
        if (mCallback != null && mCallback.get() != null)
        {
            mCallback.get().OnReportPlayoutSeconds(seconds);
        }
    }

    static {
        System.loadLibrary("AudioDecoder");
    }
}
