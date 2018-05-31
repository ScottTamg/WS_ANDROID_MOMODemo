package com.tttrtclive.test;

import android.text.TextUtils;

import com.tttrtclive.LocalConfig;
import com.tttrtclive.utils.MyLog;
import com.wushuangtech.jni.RoomJni;
import com.wushuangtech.library.GlobalConfig;

/**
 * Created by wangzhiguo on 18/5/23.
 */

public class TestUtils {

    public static void setAddressAndPushUrl(boolean mIsEnableH265){
        // 设置服务器地址
        if (!TextUtils.isEmpty(LocalConfig.mIP)) {
            MyLog.d("设置服务器地址 : " + LocalConfig.mIP);
            RoomJni.getInstance().setServerAddress(LocalConfig.mIP, LocalConfig.mPort);
        } else {
            MyLog.d("没有设置服务器地址，使用动态分配");
        }

        // 设置推流地址
        GlobalConfig.mPushUrl = LocalConfig.mPushUrl;
        if (LocalConfig.mPushUrlPrefix.equals(GlobalConfig.mPushUrl)) {
            LocalConfig.mPushUrl = LocalConfig.mPushUrlPrefix + LocalConfig.mLoginRoomID;
            GlobalConfig.mPushUrl = LocalConfig.mPushUrlPrefix + LocalConfig.mLoginRoomID;
            MyLog.d("sdk推流没设置ID，自动填上ID");
        }

        if (mIsEnableH265) {
            GlobalConfig.mPushUrl = LocalConfig.mPushUrlPrefix + LocalConfig.mLoginRoomID + "?trans=1";
        }
    }
}
