/*
 *  Copyright (c) 2015 The WebRTC project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package org.webrtc.voiceengine;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.System;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;
import android.os.Build;
import android.os.Process;
import android.util.Log;

import com.wushuangtech.audiocore.MyAudioApi;

public class  WebRtcAudioRecord {
    public static boolean isCapturing = false;
    public static int capturedDataSize = 0;

    static int audio_record_sessionId = 0;

    private static final boolean DEBUG = false;

    private static final String TAG = "WebRtcAudioRecord";

    // Default audio data format is PCM 16 bit per sample.
    // Guaranteed to be supported by all devices.
    private static final int BITS_PER_SAMPLE = 16;

    // Requested size of each recorded buffer provided to the client.
    private static final int CALLBACK_BUFFER_SIZE_MS = 10;

    // Average number of callbacks per second.
    private static final int BUFFERS_PER_SECOND = 1000 / CALLBACK_BUFFER_SIZE_MS;

    // We ask for a native buffer size of BUFFER_SIZE_FACTOR * (minimum required
    // buffer size). The extra space is allocated to guard against glitches under
    // high load.
    private static final int BUFFER_SIZE_FACTOR = 2;

    private final long nativeAudioRecord;
    private final Context context;

    private WebRtcAudioEffects effects;

    private ByteBuffer byteBuffer;

    private AudioRecord audioRecord = null;
    private AudioRecordThread audioThread = null;

    private static int count = 0;

    /**
     * Audio thread which keeps calling ByteBuffer.read() waiting for audio
     * to be recorded. Feeds recorded data to the native counterpart as a
     * periodic sequence of callbacks using DataIsRecorded().
     * This thread uses a Process.THREAD_PRIORITY_URGENT_AUDIO priority.
     */
    private class AudioRecordThread extends Thread {
        private volatile boolean keepAlive = true;

        AudioRecordThread(String name) {
            super(name);
        }

        @Override
        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);
            Log.d(TAG, "AudioRecordThread" + WebRtcAudioUtils.getThreadInfo());
            assertTrue(audioRecord.getRecordingState()
                    == AudioRecord.RECORDSTATE_RECORDING);

            long lastTime = System.nanoTime();

            while (keepAlive) {
                int bytesRead = audioRecord.read(byteBuffer, byteBuffer.capacity());
                if (bytesRead == byteBuffer.capacity()) {
                    capturedDataSize += bytesRead;
                    nativeDataIsRecorded(bytesRead, nativeAudioRecord);
                } else {
                    Log.e(TAG,"AudioRecord.read failed: " + bytesRead);
                    if (count % 100 == 0) {
                        MyAudioApi.getInstance(context).onError(6);
                        count++;
                    }
                    if (bytesRead == AudioRecord.ERROR_INVALID_OPERATION) {
                        keepAlive = false;
                    }
                }

                if (DEBUG) {
                    long nowTime = System.nanoTime();
                    long durationInMs =
                            TimeUnit.NANOSECONDS.toMillis((nowTime - lastTime));
                    lastTime = nowTime;
                    Log.d(TAG, "bytesRead[" + durationInMs + "] " + bytesRead);
                }
            }

            try {
                audioRecord.stop();
            } catch (IllegalStateException e) {
                Log.e(TAG,"AudioRecord.stop failed: " + e.getMessage());
                MyAudioApi.getInstance(context).onError(7);
            }
        }

        void joinThread() {
            keepAlive = false;
            while (isAlive()) {
                try {
                    join();
                } catch (InterruptedException e) {
                    MyAudioApi.getInstance(context).onError(8);
                    // Ignore.
                }
            }
        }
    }

    WebRtcAudioRecord(Context context, long nativeAudioRecord) {
        Log.d(TAG, "ctor" + WebRtcAudioUtils.getThreadInfo());
        this.context = context;
        this.nativeAudioRecord = nativeAudioRecord;
        if (DEBUG) {
            WebRtcAudioUtils.logDeviceInfo(TAG);
        }
        effects = WebRtcAudioEffects.create();
    }

    private boolean enableBuiltInAEC(boolean enable) {
    /*
    Log.d(TAG, "enableBuiltInAEC(" + enable + ')');
    if (effects == null) {
      Log.e(TAG,"Built-in AEC is not supported on this platform");
      return false;
    }
    return effects.setAEC(enable);
    */
        return true;
    }

    private boolean enableBuiltInAGC(boolean enable) {
        Log.d(TAG, "enableBuiltInAGC(" + enable + ')');
        if (effects == null) {
            Log.e(TAG,"Built-in AGC is not supported on this platform");
            return false;
        }
        return effects.setAGC(enable);
    }

    private boolean enableBuiltInNS(boolean enable) {
        Log.d(TAG, "enableBuiltInNS(" + enable + ')');
        if (effects == null) {
            Log.e(TAG,"Built-in NS is not supported on this platform");
            return false;
        }
        return effects.setNS(enable);
    }

    private int initRecording(int sampleRate, int channels, boolean use_voip) {
        Log.d(TAG, "initRecording(sampleRate=" + sampleRate + ", channels=" +
                channels + ", use_voip=" + use_voip + ")");
        if (!WebRtcAudioUtils.hasPermission(
                context, android.Manifest.permission.RECORD_AUDIO)) {
            Log.e(TAG,"RECORD_AUDIO permission is missing");
            return -1;
        }
        if (audioRecord != null) {
            Log.e(TAG,"InitRecording() called twice without StopRecording()");
            MyAudioApi.getInstance(context).onError(1);
            releaseAudioResources();
            return -1;
        }
        final int bytesPerFrame = channels * (BITS_PER_SAMPLE / 8);
        final int framesPerBuffer = sampleRate / BUFFERS_PER_SECOND;
        byteBuffer = ByteBuffer.allocateDirect(bytesPerFrame * framesPerBuffer);
        Log.d(TAG, "byteBuffer.capacity: " + byteBuffer.capacity());
        // Rather than passing the ByteBuffer with every callback (requiring
        // the potentially expensive GetDirectBufferAddress) we simply have the
        // the native class cache the address to the memory once.
        nativeCacheDirectBufferAddress(byteBuffer, nativeAudioRecord);

        // Get the minimum buffer size required for the successful creation of
        // an AudioRecord object, in byte units.
        // Note that this size doesn't guarantee a smooth recording under load.
        int minBufferSize = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        if (minBufferSize == AudioRecord.ERROR
                || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            Log.e(TAG, "AudioRecord.getMinBufferSize failed: " + minBufferSize);
            MyAudioApi.getInstance(context).onError(2);
            return -1;
        }
        Log.d(TAG, "AudioRecord.getMinBufferSize: " + minBufferSize);

        // Use a larger buffer size than the minimum required when creating the
        // AudioRecord instance to ensure smooth recording under load. It has been
        // verified that it does not increase the actual recording latency.
        int bufferSizeInBytes =
                Math.max(BUFFER_SIZE_FACTOR * minBufferSize, byteBuffer.capacity());
        Log.d(TAG, "bufferSizeInBytes: " + bufferSizeInBytes);
        try {

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT
                    && use_voip
                    && !WebRtcAudioUtils.isBlackListedVoiceCommunication()) {

                audioRecord = new AudioRecord(AudioSource.VOICE_COMMUNICATION,
                        sampleRate,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSizeInBytes);
            } else {
                audioRecord = new AudioRecord(AudioSource.DEFAULT,
                        sampleRate,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSizeInBytes);
            }
        } catch (IllegalArgumentException e) {
            Log.e(TAG,e.getMessage());
            releaseAudioResources();
            return -1;
        }
        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG,"Failed to create a new AudioRecord instance");
            MyAudioApi.getInstance(context).onError(3);
            releaseAudioResources();
            return -1;
        }
        audio_record_sessionId = audioRecord.getAudioSessionId();
        Log.d(TAG, "AudioRecord "
                + "session ID: " + audioRecord.getAudioSessionId() + ", "
                + "audio format: " + audioRecord.getAudioFormat() + ", "
                + "channels: " + audioRecord.getChannelCount() + ", "
                + "sample rate: " + audioRecord.getSampleRate());
        if (effects != null) {
            effects.enable(audioRecord.getAudioSessionId());
        }

        return framesPerBuffer;
    }

    private boolean startRecording() {
        Log.d(TAG, "startRecording");
        assertTrue(audioRecord != null);
        assertTrue(audioThread == null);

        isCapturing = true;

        try {
            audioRecord.startRecording();
        } catch (IllegalStateException e) {
            Log.e(TAG,"AudioRecord.startRecording failed: " + e.getMessage());
            MyAudioApi.getInstance(context).onError(4);
            releaseAudioResources();
            return false;
        }

        // Verify the recording state up to two times (with a sleep in between)
        // before returning false and reporting an error.
        int numberOfStateChecks = 0;
        while (audioRecord.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING &&
                ++numberOfStateChecks < 2) {
            threadSleep(200);
        }
        if (audioRecord.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
            Log.e(TAG,"AudioRecord.startRecording failed");
            MyAudioApi.getInstance(context).onError(5);
            releaseAudioResources();
            return false;
        }
        audioThread = new AudioRecordThread("AudioRecordJavaThread");
        audioThread.start();
        return true;
    }

    private boolean stopRecording() {
        Log.d(TAG, "stopRecording");
        assertTrue(audioThread != null);

        isCapturing = false;

        audioThread.joinThread();
        audioThread = null;
        releaseAudioResources();
        audio_record_sessionId = 0;
        return true;
    }

    // Helper method which throws an exception  when an assertion has failed.
    private static void assertTrue(boolean condition) {
        if (!condition) {
            throw new AssertionError("Expected condition to be true");
        }
    }

    // Releases the native AudioRecord resources.
    private void releaseAudioResources () {
        if (effects != null) {
            effects.release();
        }

        if (audioRecord != null) {
            audioRecord.release();
            audioRecord = null;
        }
    }

    // Causes the currently executing thread to sleep for the specified number
    // of milliseconds.
    private void threadSleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Log.e(TAG, "Thread.sleep failed: " + e.getMessage());
        }
    }

    private native void nativeCacheDirectBufferAddress(
            ByteBuffer byteBuffer, long nativeAudioRecord);

    private native void nativeDataIsRecorded(int bytes, long nativeAudioRecord);
}
