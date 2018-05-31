package com.wushuangtech.videocore;

import android.text.TextUtils;

import com.wushuangtech.api.EnterConfApiImpl;
import com.wushuangtech.inter.UserExitNotifyCallBack;
import com.wushuangtech.utils.PviewLog;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by wangzhiguo on 17/12/14.
 */

public class VideoDecoderManager implements UserExitNotifyCallBack {

    private static VideoDecoderManager holder;
    private ConcurrentHashMap<String, VideoDecoder> mDecodes;

    private VideoDecoderManager() {
        mDecodes = new ConcurrentHashMap<>();
        EnterConfApiImpl.getInstance().setUserExitNotifyCallBack(this);
    }

    public static VideoDecoderManager getInstance() {
        if (holder == null) {
            synchronized (VideoDecoderManager.class) {
                if (holder == null) {
                    holder = new VideoDecoderManager();
                }
            }
        }
        return holder;
    }

    public synchronized boolean findVideoDecoderByID(String devID) {
        if (TextUtils.isEmpty(devID)) {
            return false;
        }

        VideoDecoder videoDecoder = mDecodes.get(devID);
        return videoDecoder != null;
    }

    public synchronized VideoDecoder getVideoDecoderByID(String devID) {
        if (TextUtils.isEmpty(devID)) {
            return null;
        }

        return mDecodes.get(devID);
    }

    public synchronized VideoDecoder createNewVideoDecoder(String devID) {
        PviewLog.d("VideoDecoderManager -> createNewVideoDecoder , device id : " + devID
                + " | mDecodes size : " + mDecodes.size());
        if (TextUtils.isEmpty(devID)) {
            return null;
        }

        VideoDecoder decoder = VideoDecoder.createInstance(true);//false=硬件解码（默认），true=软件解码
        decoder.setBindDevID(devID);
        decoder.start();
        mDecodes.put(devID, decoder);
        return decoder;
    }

    public void addVideoData(String devID, RemoteSurfaceView.VideoFrame frame) {
        VideoDecoder videoDecoder = mDecodes.get(devID);
        videoDecoder.onGetH264Frame(frame);
    }

    @Override
    public void userExitRoom(String devID) {
        if (TextUtils.isEmpty(devID)) {
            return ;
        }

        if (devID.equals("all")) {
            for (int i = 0; i < mDecodes.size(); i++) {
                VideoDecoder videoDecoder = mDecodes.get(i);
                if (videoDecoder != null) {
                    videoDecoder.stop();
                }
            }
            mDecodes.clear();
        } else {
            VideoDecoder remove = mDecodes.remove(devID);
            if (remove != null) {
                remove.stop();
            }
            PviewLog.d("VideoDecoderManager -> receive user exit room , device id : " + devID
                    + " | mDecodes size : " + mDecodes.size());
        }
    }
}
