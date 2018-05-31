package com.tttrtclive;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.TextUtils;

import com.tencent.bugly.crashreport.CrashReport;
import com.tttrtclive.callback.MyTTTRtcEngineEventHandler;
import com.wushuangtech.wstechapi.TTTRtcEngine;

import java.util.Random;
import java.util.Vector;

public class MainApplication extends Application {

    public MyTTTRtcEngineEventHandler mMyTTTRtcEngineEventHandler;
    public Vector<String> mTestDatas = new Vector<>();
    private MyLocalBroadcastReceiver mLocalBroadcast;

    @Override
    public void onCreate() {
        super.onCreate();

        Random mRandom = new Random();
        LocalConfig.mLoginUserID = mRandom.nextInt(999999);

        //1.设置SDK的回调接收类
        mMyTTTRtcEngineEventHandler = new MyTTTRtcEngineEventHandler(getApplicationContext());
        //2.创建SDK的实例对象 "a967ac491e3acf92eed5e1b5ba641ab7" test900572e02867fab8131651339518
        TTTRtcEngine mTTTEngine = TTTRtcEngine.create(getApplicationContext(), "a967ac491e3acf92eed5e1b5ba641ab7",
                mMyTTTRtcEngineEventHandler);
        if (mTTTEngine == null) {
            System.exit(0);
        }

        CrashReport.initCrashReport(getApplicationContext(), "5ade4cea78", true);
        initTestBroadcast();
    }

    // sdk的测试代码，请忽略
    private void initTestBroadcast() {
        mLocalBroadcast = new MainApplication.MyLocalBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addCategory("ttt.test.interface");
        filter.addAction("ttt.test.interface.string");
        registerReceiver(mLocalBroadcast, filter);
    }

    // sdk的测试代码，请忽略
    public void unInitTestBroadcast() {
        try {
            unregisterReceiver(mLocalBroadcast);
        } catch (Exception e) {}
    }

    // sdk的测试代码，请忽略
    private class MyLocalBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("ttt.test.interface.string".equals(action)) {
                String testString = intent.getStringExtra("testString");
                if (!TextUtils.isEmpty(testString)) {
                    mTestDatas.add(testString);
                }
            }
        }
    }
}
