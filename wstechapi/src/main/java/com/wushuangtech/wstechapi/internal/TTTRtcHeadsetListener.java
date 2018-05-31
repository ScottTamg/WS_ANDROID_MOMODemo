package com.wushuangtech.wstechapi.internal;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.SystemClock;

import com.wushuangtech.library.Constants;
import com.wushuangtech.library.GlobalConfig;
import com.wushuangtech.library.GlobalHolder;
import com.wushuangtech.utils.PviewLog;
import com.wushuangtech.wstechapi.TTTRtcEngine;

import static com.wushuangtech.library.Constants.AUDIO_ROUTE_SPEAKER;

/**
 * 用于管理SDK音频路由通道
 */
public class TTTRtcHeadsetListener {

    private static final String TAG = TTTRtcHeadsetListener.class.getSimpleName();
    private HeadsetListenerBroadcast mHeadsetListenerBroadcast;
    private boolean isBluetoothHeadsetConnected;
    private Context mContext;
    private TTTRtcEngine mTTTRtcEngine;
    private int mLastAudioRoute;
    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    public TTTRtcHeadsetListener(Context mContext, TTTRtcEngine mTTTRtcEngine) {
        this.mContext = mContext;
        this.mTTTRtcEngine = mTTTRtcEngine;
        mLastAudioRoute = AUDIO_ROUTE_SPEAKER;
        recordLastAudioRoute();
    }

    public void registerReceiver() {
        mHeadsetListenerBroadcast = new HeadsetListenerBroadcast();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_HEADSET_PLUG);
        filter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
        mContext.registerReceiver(mHeadsetListenerBroadcast, filter);
    }

    public void unregisterReceiver() {
        try {
            if (mHeadsetListenerBroadcast != null) {
                mContext.unregisterReceiver(mHeadsetListenerBroadcast);
            }
        } catch (Exception e) {
            PviewLog.w(TAG, "unregisterReceiver -- unregist failed!! trace : " + e.getLocalizedMessage());
        }
    }

    public void headsetAndBluetoothHeadsetHandle() {
        AudioManager audioManager = (AudioManager) mContext.getSystemService(
                Context.AUDIO_SERVICE);
        if (audioManager.isWiredHeadsetOn()) {
            recordLastAudioRoute();
            if (!GlobalConfig.mIsHeadsetPriority && GlobalConfig.mIsSpeakerphoneEnabled) {
                audioManager.setSpeakerphoneOn(true);
                GlobalHolder.getInstance().notifyCHAudioRouteChanged(Constants.AUDIO_ROUTE_SPEAKER);
                PviewLog.i(TAG, "切换到了有线耳机，但强制扬声器输出");
            } else {
                audioManager.setSpeakerphoneOn(false);
                GlobalHolder.getInstance().notifyCHAudioRouteChanged(Constants.AUDIO_ROUTE_HEADSET);
                PviewLog.i(TAG, "切换到了有线耳机");

            }
        } else if (isBluetoothHeadsetConnected && !audioManager.isBluetoothA2dpOn()) {
            SystemClock.sleep(500);
            recordLastAudioRoute();
            if (!GlobalConfig.mIsHeadsetPriority && GlobalConfig.mIsSpeakerphoneEnabled) {
                audioManager.setSpeakerphoneOn(true);
                GlobalHolder.getInstance().notifyCHAudioRouteChanged(Constants.AUDIO_ROUTE_SPEAKER);
                PviewLog.i(TAG, "切换到SCO链路蓝牙耳机，但强制扬声器输出");
            } else {
                if (audioManager.isSpeakerphoneOn()) {
                    audioManager.setSpeakerphoneOn(false);
                    PviewLog.i(TAG, "切换到SCO链路蓝牙耳机 ， 扬声器关闭");
                } else {
                    PviewLog.i(TAG, "切换到SCO链路蓝牙耳机 ， 扬声器已经关闭");
                }
                audioManager.startBluetoothSco();
                audioManager.setBluetoothScoOn(true);
                GlobalHolder.getInstance().notifyCHAudioRouteChanged(Constants.AUDIO_ROUTE_HEADSET);
            }
        } else if (isBluetoothHeadsetConnected && audioManager.isBluetoothA2dpOn()) {
            recordLastAudioRoute();
            if (!GlobalConfig.mIsHeadsetPriority && GlobalConfig.mIsSpeakerphoneEnabled) {
                audioManager.setSpeakerphoneOn(true);
                GlobalHolder.getInstance().notifyCHAudioRouteChanged(Constants.AUDIO_ROUTE_SPEAKER);
                PviewLog.i(TAG, "切换到了ACL链路的A2DP蓝牙耳机，但强制扬声器输出");
            } else {
                if (audioManager.isSpeakerphoneOn()) {
                    audioManager.setSpeakerphoneOn(false);
                    PviewLog.i(TAG, "切换到了ACL链路的A2DP蓝牙耳机 ， 扬声器关闭");
                } else {
                    PviewLog.i(TAG, "切换到了ACL链路的A2DP蓝牙耳机 ， 扬声器已经关闭");
                }
                GlobalHolder.getInstance().notifyCHAudioRouteChanged(Constants.AUDIO_ROUTE_HEADSET);
            }
        } else {
            if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
                int headset = mBluetoothAdapter.getProfileConnectionState(BluetoothProfile.HEADSET);
                if (headset == BluetoothProfile.STATE_CONNECTED) {
                    recordLastAudioRoute();
                    if (audioManager.isSpeakerphoneOn()) {
                        audioManager.setSpeakerphoneOn(false);
                    }
                    PviewLog.i(TAG, "切换到SCO链路蓝牙耳机 ， 扬声器关闭");
                    audioManager.startBluetoothSco();
                    audioManager.setBluetoothScoOn(true);
                    return ;
                }
            }

            PviewLog.i("TTTRtcHeadsetListener else audio mBluetoothAdapter : "
                    + mBluetoothAdapter + " | isBluetoothHeadsetConnected : " + isBluetoothHeadsetConnected);

            if (isBluetoothHeadsetConnected && !audioManager.isBluetoothA2dpOn()) {
                audioManager.stopBluetoothSco();
                audioManager.setBluetoothScoOn(false);
            }
            if (mLastAudioRoute == Constants.AUDIO_ROUTE_SPEAKER) {
                mTTTRtcEngine.setEnableSpeakerphone(true);
                GlobalHolder.getInstance().notifyCHAudioRouteChanged(Constants.AUDIO_ROUTE_SPEAKER);
                PviewLog.i("TTTRtcHeadsetListener audio -> 默认音频路由扬声器!");
            } else {
                PviewLog.i("TTTRtcHeadsetListener audio -> 默认音频路由听筒!");
                mTTTRtcEngine.setEnableSpeakerphone(false);
                GlobalHolder.getInstance().notifyCHAudioRouteChanged(Constants.AUDIO_ROUTE_HEADPHONE);
            }
            mLastAudioRoute = GlobalConfig.mDefaultAudioRoute;
        }
    }

    private void recordLastAudioRoute(){
        if (GlobalConfig.mIsSpeakerphoneEnabled) {
            mLastAudioRoute = Constants.AUDIO_ROUTE_SPEAKER;
            PviewLog.i("recordLastAudioRoute audio -> AUDIO_ROUTE_SPEAKER!");
        } else {
            mLastAudioRoute = Constants.AUDIO_ROUTE_HEADPHONE;
            PviewLog.i("recordLastAudioRoute audio -> AUDIO_ROUTE_HEADPHONE!");
        }
    }


    class HeadsetListenerBroadcast extends BroadcastReceiver {


        @Override

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_HEADSET_PLUG.equals(action)) {
                if (intent.hasExtra("state")) {
                    int state = intent.getIntExtra("state", 0);
                    if (state == 1) {
                        PviewLog.i(TAG, "插入耳机");
                        headsetAndBluetoothHeadsetHandle();
                    } else if (state == 0) {
                        PviewLog.i(TAG, "拔出耳机");
                        headsetAndBluetoothHeadsetHandle();
                    }
                }

            } else if (BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1);
                if (state == BluetoothProfile.STATE_CONNECTED) {
                    isBluetoothHeadsetConnected = true;
                    PviewLog.i(TAG, "蓝牙耳机已连接");
                    headsetAndBluetoothHeadsetHandle();
                } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                    PviewLog.i(TAG, "蓝牙耳机已断开");
                    isBluetoothHeadsetConnected = false;
                    headsetAndBluetoothHeadsetHandle();
                }
            }
        }
    }
}
