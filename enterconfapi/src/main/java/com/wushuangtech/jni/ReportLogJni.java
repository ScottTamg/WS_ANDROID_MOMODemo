package com.wushuangtech.jni;

import com.wushuangtech.library.PviewConferenceRequest;
import com.wushuangtech.utils.PviewLog;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class ReportLogJni {
    private static ReportLogJni mReportLogJni;
    private List<WeakReference<PviewConferenceRequest>> mCallBacks;

    private ReportLogJni() {
        mCallBacks = new ArrayList<>();
    }

    public static synchronized ReportLogJni getInstance() {
        if (mReportLogJni == null) {
            synchronized (ReportLogJni.class) {
                if (mReportLogJni == null) {
                    mReportLogJni = new ReportLogJni();
                    if (!mReportLogJni.initialize(mReportLogJni)) {
                        throw new RuntimeException("can't initilaize ReportLogJni");
                    }
                }
            }
        }
        return mReportLogJni;
    }

    /**
     * 添加自定义的回调，监听接收到的服务信令
     *
     * @param callback 回调对象
     */
    public void addCallback(PviewConferenceRequest callback) {
        this.mCallBacks.add(new WeakReference<PviewConferenceRequest>(callback));
    }

    /**
     * 移除自定义添加的回调
     *
     * @param callback 回调对象
     */
    public void removeCallback(PviewConferenceRequest callback) {
        for (int i = 0; i < mCallBacks.size(); i++) {
            WeakReference<PviewConferenceRequest> wf = mCallBacks.get(i);
            if (wf != null && wf.get() != null) {
                if (wf.get() == callback) {
                    mCallBacks.remove(wf);
                    return;
                }
            }
        }
    }

    public native boolean initialize(ReportLogJni request);

    public native void unInitialize();

    public native void ReportLog(String logMsg, int msgType);

    private void OnUpdateReportLogConfig(boolean reportData, boolean reportEvent, int reportInterval) {
        PviewLog.jniCall("OnUpdateReportLogConfig", "Begin");
        for (int i = 0; i < mCallBacks.size(); i++) {
            WeakReference<PviewConferenceRequest> wf = mCallBacks.get(i);
            if (wf != null && wf.get() != null) {
                wf.get().OnUpdateReportLogConfig(reportData, reportEvent, reportInterval);
            }
        }
        PviewLog.jniCall("OnUpdateReportLogConfig", "End");
    }
}
