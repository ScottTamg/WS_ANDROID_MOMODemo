package com.wushuangtech.videocore;

import android.content.Context;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.wushuangtech.api.EnterConfApi;
import com.wushuangtech.api.ExternalVideoModuleCallback;
import com.wushuangtech.library.Constants;
import com.wushuangtech.library.GlobalConfig;
import com.wushuangtech.library.GlobalHolder;
import com.wushuangtech.utils.PviewLog;

import java.util.HashMap;

/**
 * Created by wangzhiguo on 18/1/31.
 */

public class VideoNewControl {

    private static VideoNewControl holder;

    public static VideoNewControl getInstance() {
        if (holder == null) {
            synchronized (VideoNewControl.class) {
                if (holder == null) {
                    holder = new VideoNewControl();
                }
            }
        }
        return holder;
    }

    public static void firstDecoder(byte[] data, String devID, long timeStamp, int width, int height, ExternalVideoModuleCallback.VideoFrameType frameType) {
        HashMap<String, Boolean> tempList = GlobalHolder.getInstance().getRemoteFirstDecoders();
        Boolean aBoolean = tempList.get(devID);
        if (aBoolean == null) {
            tempList.put(devID, true);
            GlobalHolder.getInstance().notifyCHFirstRemoteVideoDecoder(devID, width, height, 0);
        }

        // 创建解码器
        boolean isExist = VideoDecoderManager.getInstance().findVideoDecoderByID(devID);
        if (!isExist) {
            VideoDecoderManager.getInstance().createNewVideoDecoder(devID);
        }

        // 解码器开始工作
        RemoteSurfaceView.VideoFrame frame = new RemoteSurfaceView.VideoFrame();
        frame.data = data;
        frame.frameType = frameType;
        frame.timeStamp = timeStamp;
        frame.width = width;
        frame.height = height;
        VideoDecoderManager.getInstance().addVideoData(devID, frame);

        // 查找存在的View，如果存在开始绘制图形
        RemoteSurfaceView view = RemotePlayerManger.getInstance().getRemoteSurfaceView(devID);
        PviewLog.wf("receiveVideoData devID : " + devID + " | width : " + width + " | height ： " + height
                + " | view : " + view);
        if (view != null) {
            view.AddVideoData(frame);
        }
    }

    public static SurfaceView createRemoteSurfaceView(Context mContext) {
        return RemotePlayerManger.getInstance().createRemoteSurfaceView(mContext);
    }

    public static void setupLocalVideo(SurfaceView mView, int direction, WaterMarkPosition position, int showMode) {
        RemoteSurfaceView tempView = (RemoteSurfaceView) mView;
        tempView.setIsLocalCameraView(true);
        init(direction, position, (RemoteSurfaceView) mView);
        boolean isDisplay = LocaSurfaceView.getInstance().setDisplayMode(showMode);
        PviewLog.d("LocalCamera setupLocalVideo .... isDisplay : " + isDisplay);
    }

    public static void setupRemoteVideo(SurfaceView mView, long mUserID, String mDeviceID, int showMode) {
        PviewLog.d("setupRemoteVideo 2 mUserID ID : " + mUserID
                + " | mDeviceID : " + mDeviceID + " | showMode : " + showMode) ;
        RemoteSurfaceView mRemote = (RemoteSurfaceView) mView;
        mRemote.setBindUserID(mUserID);
        mRemote.setDeviceID(mDeviceID);
        mRemote.setIsLocalCameraView(false);
        RemotePlayerManger.getInstance().setViewIDMap(mDeviceID, mRemote);
        mRemote.initRemote(showMode);

    }

    public static void adJustLocalSurfaceView(boolean isEnable) {
        if (isEnable && GlobalConfig.mIsEnableVideoMode) {
            EnterConfApi.getInstance().uploadMyVideo(true);
            MyVideoApi.getInstance().startPreview();
            MyVideoApi.getInstance().startCapture();
        } else {
            EnterConfApi.getInstance().uploadMyVideo(false);
            MyVideoApi.getInstance().stopPreview();
            MyVideoApi.getInstance().stopCapture();
            GlobalHolder.getInstance().notifyCHVideoStop();
        }
    }

    public static void adJustRemoteVideoOpenOrClose(boolean isOpen) {
        RemotePlayerManger.getInstance().adjustAllRemoteViewDisplay(isOpen);
    }


    // ******************************** LocaSurfaceView ***********************************************************************************************
    private static void init(int direction,
                             final WaterMarkPosition waterMarkPosition, RemoteSurfaceView mView) {
        LocaSurfaceView.getInstance().Reset();
        LocaSurfaceView.getInstance().mfastImageProcessingView = mView;
        LocaSurfaceView.getInstance().mActivityDirector = direction;
        LocaSurfaceView.getInstance().mWaterMarkPos = waterMarkPosition;
        LocaSurfaceView.getInstance().mPipeline = mView.getPipeline();
        LocaSurfaceView.getInstance().mfastImageProcessingView.getHolder().addCallback(new SurfaceHolder.Callback() {

            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                PviewLog.d("LocalCamera surfaceCreated.....");
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                PviewLog.d("LocalCamera surfaceDestroyed.....");
                LocaSurfaceView.getInstance().FreeAll();
            }
        });

        PviewLog.d("LocalCamera LocaSurfaceView init .... bcreate : " + LocaSurfaceView.getInstance().bcreate);
        LocaSurfaceView.getInstance().CreateLocalSurfaceView(waterMarkPosition);
        if (Constants.IS_UNITY) {
            LocaSurfaceView.getInstance().createFBOTextureBinder();
        }
    }

}
