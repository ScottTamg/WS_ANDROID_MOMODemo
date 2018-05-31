package com.wushuangtech.wstechapi.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Created by wangzhiguo on 17/9/25.
 */

public class DeviceUtils {
    private static DeviceUtils holder;
    private static final String TAG = "DeviceInfoManager";
    private static ActivityManager mActivityManager;
    private Status mLastTotalStatus = new Status();
    private Status mLastProcessStatus = new Status();
    private long mLastAppStatus;

    private RandomAccessFile procStatFile;
    private RandomAccessFile appStatFile;

    private DeviceUtils() {

    }

    public static DeviceUtils getInstance() {
        if (holder == null) {
            synchronized (DeviceUtils.class) {
                if (holder == null) {
                    holder = new DeviceUtils();
                }
            }
        }
        return holder;
    }

    private synchronized static ActivityManager getActivityManager(Context context) {
        if (mActivityManager == null) {
            mActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        }
        return mActivityManager;
    }

    public void init(){
        getTotalCpuTime(mLastTotalStatus);
        getTotalCpuTime(mLastProcessStatus);
        mLastAppStatus = getAppCpuTime();
    }

    /**
     * 计算已使用内存的百分比，并返回。
     *
     * @param context 可传入应用程序上下文。
     * @return 已使用内存的百分比，以字符串形式返回。
     */
    public static String getUsedPercentValue(Context context) {
        long totalMemorySize = getTotalMemory();
        long availableSize = getAvailableMemory(context) / 1024;
        int percent = (int) ((totalMemorySize - availableSize) / (float) totalMemorySize * 100);
        return percent + "%";
    }

    /**
     * 获取当前可用内存，返回数据以字节为单位。
     *
     * @param context 可传入应用程序上下文。
     * @return 当前可用内存。
     */
    public static long getAvailableMemory(Context context) {
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        getActivityManager(context).getMemoryInfo(mi);
        return mi.availMem;
    }

    /**
     * 获取系统总内存,返回字节单位为KB
     *
     * @return 系统总内存
     */
    public static long getTotalMemory() {
        long totalMemorySize = 0;
        String dir = "/proc/meminfo";
        try {
            FileReader fr = new FileReader(dir);
            BufferedReader br = new BufferedReader(fr, 2048);
            String memoryLine = br.readLine();
            String subMemoryLine = memoryLine.substring(memoryLine.indexOf("MemTotal:"));
            br.close();
            //将非数字的字符替换为空
            totalMemorySize = Integer.parseInt(subMemoryLine.replaceAll("\\D+", ""));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return totalMemorySize;
    }

    /**
     * 获取当前进程的CPU使用率
     *
     * @return CPU的使用率
     */
    public float getCurProcessCpuRate() {
        float lastTotalTime = mLastProcessStatus.getTotalTime();

        Status status = getTotalCpuTime();
        float currentTotalCpuTime = status.getTotalTime();
        long currentProcessCpuTime = getAppCpuTime();
        float cpuRate = 100 * (currentProcessCpuTime - mLastAppStatus)
                / (currentTotalCpuTime - lastTotalTime);
        mLastAppStatus = currentProcessCpuTime;
        mLastProcessStatus.copyValues(status);
        return cpuRate;
    }

    /**
     * 获取总的CPU使用率
     *
     * @return CPU使用率
     */
    public float getTotalCpuRate() {
        Status status = getTotalCpuTime();

        float currentTotalTime = status.getTotalTime();
        float currentTotalUsedCpuTime = currentTotalTime - status.idletime;

        float lastTotalTime = mLastTotalStatus.getTotalTime();
        float lastTotalUsedCpuTime = lastTotalTime - mLastTotalStatus.idletime;

        float cpuRate = 100 * (currentTotalUsedCpuTime - lastTotalUsedCpuTime)
                / (currentTotalTime - lastTotalTime);

        mLastTotalStatus.copyValues(status);
        return cpuRate;
    }

    /**
     * 获取系统总CPU使用时间
     *
     * @return 系统CPU总的使用时间
     */
    private Status getTotalCpuTime() {
        String[] cpuInfos = null;
        try {
            if (procStatFile == null) {
                procStatFile = new RandomAccessFile("/proc/stat", "r");
            } else {
                procStatFile.seek(0L);
            }

            String load = procStatFile.readLine();
            cpuInfos = load.split(" ");
        } catch (IOException e) {
            Log.i(TAG, " getTotalCpuTime --> get crash : " + e.getLocalizedMessage());
        }

        Status sStatus = new Status();
        if (cpuInfos != null) {
            sStatus.usertime = Long.parseLong(cpuInfos[2]);
            sStatus.nicetime = Long.parseLong(cpuInfos[3]);
            sStatus.systemtime = Long.parseLong(cpuInfos[4]);
            sStatus.idletime = Long.parseLong(cpuInfos[5]);
            sStatus.iowaittime = Long.parseLong(cpuInfos[6]);
            sStatus.irqtime = Long.parseLong(cpuInfos[7]);
            sStatus.softirqtime = Long.parseLong(cpuInfos[8]);
        }
        return sStatus;
    }

    private Status getTotalCpuTime(Status sStatus) {
        String[] cpuInfos = null;
        try {
            if (procStatFile == null) {
                procStatFile = new RandomAccessFile("/proc/stat", "r");
            } else {
                procStatFile.seek(0L);
            }

            String load = procStatFile.readLine();
            cpuInfos = load.split(" ");
        } catch (IOException e) {
            Log.i(TAG, " getTotalCpuTime --> get crash : " + e.getLocalizedMessage());
        }

        if (cpuInfos != null) {
            sStatus.usertime = Long.parseLong(cpuInfos[2]);
            sStatus.nicetime = Long.parseLong(cpuInfos[3]);
            sStatus.systemtime = Long.parseLong(cpuInfos[4]);
            sStatus.idletime = Long.parseLong(cpuInfos[5]);
            sStatus.iowaittime = Long.parseLong(cpuInfos[6]);
            sStatus.irqtime = Long.parseLong(cpuInfos[7]);
            sStatus.softirqtime = Long.parseLong(cpuInfos[8]);
        }
        return sStatus;
    }

    /**
     * 获取当前进程的CPU使用时间
     *
     * @return 当前进程的CPU使用时间
     */
    private long getAppCpuTime() {
        // 获取应用占用的CPU时间
        String[] cpuInfos = null;
        try {
            if (appStatFile == null) {
                appStatFile = new RandomAccessFile("/proc/" + android.os.Process.myPid()
                        + "/stat", "r");
            } else {
                appStatFile.seek(0L);
            }
            String load = appStatFile.readLine();
            cpuInfos = load.split(" ");
        } catch (IOException e) {
            Log.e(TAG, " getAppCpuTime --> get crash : " + e.getLocalizedMessage());
        }

        long appCpuTime = 0;
        if (cpuInfos != null) {
            appCpuTime = Long.parseLong(cpuInfos[13])
                    + Long.parseLong(cpuInfos[14]) + Long.parseLong(cpuInfos[15])
                    + Long.parseLong(cpuInfos[16]);
        }
        return appCpuTime;
    }

    public void closeRandomAccessFile(){
        if (procStatFile != null) {
            try {
                procStatFile.close();
            } catch (IOException e) {
                Log.e(TAG, " closeRandomAccessFile --> get crash : " + e.getLocalizedMessage());
            }
        }

        if (appStatFile != null) {
            try {
                appStatFile.close();
            } catch (IOException e) {
                Log.e(TAG, " closeRandomAccessFile --> get crash : " + e.getLocalizedMessage());
            }
        }
    }

    private static class Status {
        long usertime;
        long nicetime;
        long systemtime;
        long idletime;
        long iowaittime;
        long irqtime;
        long softirqtime;

        void copyValues(Status mStatus){
            this.usertime = mStatus.usertime;
            this.nicetime = mStatus.nicetime;
            this.systemtime = mStatus.systemtime;
            this.idletime = mStatus.idletime;
            this.iowaittime = mStatus.iowaittime;
            this.irqtime = mStatus.irqtime;
            this.softirqtime = mStatus.softirqtime;
        }

        long getTotalTime() {
            return (usertime + nicetime + systemtime + idletime + iowaittime
                    + irqtime + softirqtime);
        }
    }
}
