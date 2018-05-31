package com.wushuangtech.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.wushuangtech.utils.PviewLog;

/**
 * Created by Administrator on 2017-09-25.
 */


/* 使用时调用一下代码进行注册
IntentFilter filter = new IntentFilter();
filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
registerReceiver(new NetWorkReceiver(),filter);*/

public class NetWorkReceiver extends BroadcastReceiver {

    private static final String TAG = NetWorkReceiver.class.getSimpleName();
    private int mCurrentNetState = -1;
    private Context mContext = null;

    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context;

        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
            ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = manager.getActiveNetworkInfo();

            if (activeNetwork != null && activeNetwork.isConnected()) {

                // 防止多次广播执行多次代码
                if (mCurrentNetState != activeNetwork.getType())
                    mCurrentNetState = activeNetwork.getType();
                else
                    return;

                if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                    mCurrentNetState = ConnectivityManager.TYPE_WIFI;
                    PviewLog.i(TAG, "当前WiFi连接可用 ");
                } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                    mCurrentNetState = ConnectivityManager.TYPE_MOBILE;
                    PviewLog.i(TAG, "当前移动网络连接可用 ");
                }
            } else {
                // WIFI切移动网络的时候会调用此处一次，还没有方法解决
                mCurrentNetState = -1;
                PviewLog.i(TAG, "当前没有网络连接，请确保你已经 打开网络 ");
            }
        }
    }

    //判断WiFi是否打开
    public static boolean isWiFiEnabled(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
            return true;
        }
        return false;
    }

    //判断移动数据是否打开
    public static boolean isMobileEnabled(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
            return true;
        }
        return false;
    }

}
