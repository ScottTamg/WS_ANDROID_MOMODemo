package com.wushuangtech.library.chatlibrary;

import android.content.Context;
import android.media.MediaRecorder;
import android.text.TextUtils;
import android.util.Log;

import com.wushuangtech.api.WavRecorder;
import com.wushuangtech.library.GlobalConfig;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

/**
 * Created by _SOLID
 * Date:2016/3/22
 * Time:16:31
 */
public class MediaRecorderHelper {

    private Context mContext;
    private MediaRecorder mMediaRecorder;
    private WavRecorder mWavRecorder;
    private String mCurrentFilePath;
    private AliOss mAliOss = null;
    private String mFileName = null;
    private Timer mAudioRecorderTimer;
    private int mAudioTime = 0;

    public MediaRecorderHelper(Context context) {
        mContext = context;
        mAliOss = new AliOss(context);

        mWavRecorder = WavRecorder.getInstance();
    }


    /**
     * 开始录音
     */
    public void startRecord() {
        try {
            mFileName = generateFileName();
            mWavRecorder.createDefaultAudio(mContext, mFileName);
            mWavRecorder.startRecord();
            mCurrentFilePath = mWavRecorder.getWavPath();
            /*mMediaRecorder = new MediaRecorder();
            mFileName = generateFileName();
            File file = new File(mSavePath, mFileName + ".wav");
            mCurrentFilePath = file.getAbsolutePath();
            // 设置录音文件的保存位置
            mMediaRecorder.setOutputFile(mCurrentFilePath);
            // 设置录音的来源（从哪里录音）
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            // 设置录音的保存格式
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
            // 设置录音的编码
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mMediaRecorder.prepare();
            mMediaRecorder.start();*/
            mAudioTime = 0;
            mAudioRecorderTimer = new Timer();
            mAudioRecorderTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    mAudioTime ++;
                    if (mAudioTime >= 60)
                        stopAndRelease();
                }
            }, 0,1000);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 停止录音
     */
    public void stopAndRelease() {
        /*if (mMediaRecorder == null) return;
        mMediaRecorder.stop();
        mMediaRecorder.release();
        mMediaRecorder = null;*/
        if (mWavRecorder != null)
            mWavRecorder.stopRecord();

        mAudioRecorderTimer.cancel();
    }

    public int stopAndRelease(long nGroupID, long nDstUserID, String sSeqID) {
        if (mWavRecorder == null) return 0;
        stopAndRelease();

        if (TextUtils.isEmpty(sSeqID))
            sSeqID = UUID.randomUUID().toString();

        // 停止录音开始上传到阿里云OSS
        if (mCurrentFilePath != null && mFileName != null)
            mAliOss.upload(nGroupID, nDstUserID, sSeqID, mFileName, mCurrentFilePath, mAudioTime);

        return mAudioTime;
    }

    /***
     * 取消本次录音操作
     */
    public void cancel() {
        mWavRecorder.canel();
        if (mCurrentFilePath != null) {
            File file = new File(mCurrentFilePath);
            file.delete();
            mCurrentFilePath = null;
        }
    }

    private String generateFileName() {
//        return "audio.amr";
        return UUID.randomUUID().toString();
    }

    /**
     * 得到录音文件的路径
     *
     * @return
     */
    public String getCurrentFilePath() {
        return mCurrentFilePath;
    }

}
