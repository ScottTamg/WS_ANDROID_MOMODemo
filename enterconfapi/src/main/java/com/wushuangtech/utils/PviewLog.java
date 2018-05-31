package com.wushuangtech.utils;

import android.util.Log;

import com.wushuangtech.library.GlobalHolder;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class PviewLog {

    private static boolean IS_DEBUG = true;
    public static final String TAG = "WSTECH";
    public static boolean isPrint;
    // callback log tag
    public static final String JNI_CALLBACK = "JNI_CALLBACK";
    public static final String FUN_ERROR = "FUN_ERROR";
    public static final String CHAT_SEND = "CHAT_WATCHER";

    public static void i(String msg) {
        if (!IS_DEBUG) {
            return;
        }
        Log.i(TAG, msg);
    }

    public static void d(String msg) {
        if (!IS_DEBUG) {
            return;
        }
        Log.d(TAG, msg);
    }

    public static void w(String msg) {
        Log.w(TAG, msg);
    }

    public static synchronized void wf(String msg) {
        if (isPrint) {
            Log.d(TAG, msg);
        }
    }

    public static void e(String msg) {
        Log.e(TAG, msg);
    }

    public static void i(String tag, String msg) {
        if (!IS_DEBUG) {
            return;
        }
        Log.i(tag, msg);
    }

    public static void d(String tag, String msg) {
        if (!IS_DEBUG) {
            return;
        }
        Log.d(tag, msg);
    }

    public static void w(String tag, String msg) {
        Log.w(tag, msg);
    }

    public static void e(String tag, String msg) {
        Log.e(tag, msg);
    }

    public static void ecls(String tag, String msg) {
        Log.d(TAG, "Class <" + tag + ">  -> " + msg);
    }

    public static void jniCall(String methodName, String content) {
        Log.d(PviewLog.JNI_CALLBACK, " METHOD = " + methodName + " --> " + content);
    }

    public static void funEmptyError(String funName, String varName, String args) {
        Log.w(FUN_ERROR, "Invoke <" + funName + "> error , the var <" + varName + "> " +
                "is null! args : " + args);
    }

    public static void setIsPrint(boolean print) {
        isPrint = print;
    }

    public static void testPrint(String funName , Object... objs){
        long timeMillis = System.currentTimeMillis();
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss:SSS" , Locale.CHINA);
        String date = format.format(timeMillis);

        StringBuilder sb = new StringBuilder();
        sb.append('[').append(date).append("] ").append(funName).append(" -> ");
        for (int i = 0; i < objs.length; i++) {
            sb.append(objs[i]);
        }

        GlobalHolder instance = GlobalHolder.getInstance();
        if (instance != null) {
            instance.notifyCHTestString(sb.toString());
        }
    }
}
