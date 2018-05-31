package com.wushuangtech.wstechapi.internal;

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import com.wushuangtech.utils.PviewLog;

public class TTTRtcPhoneListener extends PhoneStateListener {
    private Context mContext;

    public TTTRtcPhoneListener(Context mContext) {
        this.mContext = mContext;
    }

    @Override
    public void onCallStateChanged(int state, String incomingNumber) {
        try {
            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING:   //来电
                    PviewLog.d("WebRtcAudioRecord 来电话了 : " + incomingNumber);
                    break;
                case TelephonyManager.CALL_STATE_IDLE:  //挂掉电话
                    PviewLog.d("WebRtcAudioRecord 挂掉电话 : " + incomingNumber);
                    break;
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }
}