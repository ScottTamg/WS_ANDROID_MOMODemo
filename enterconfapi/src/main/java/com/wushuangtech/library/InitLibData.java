package com.wushuangtech.library;

import android.content.Context;

import com.wushuangtech.jni.ChatJni;
import com.wushuangtech.jni.NativeInitializer;
import com.wushuangtech.jni.ReportLogJni;
import com.wushuangtech.jni.RoomJni;
import com.wushuangtech.jni.VideoJni;

public class InitLibData {

    public static boolean initlib(Context context, boolean enableChat, int logLevel) {
        System.loadLibrary("myaudio_so");
        System.loadLibrary("clientcore");

        NativeInitializer.getIntance().initialize(context , enableChat , logLevel);
        VideoJni.getInstance();
        RoomJni.getInstance();
        ReportLogJni.getInstance();
        ChatJni.getInstance();

        return true;
    }
}
