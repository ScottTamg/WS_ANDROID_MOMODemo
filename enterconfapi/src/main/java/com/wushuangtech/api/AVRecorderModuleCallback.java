package com.wushuangtech.api;

/**
 * Created by Administrator on 2017/7/26.
 */
public interface AVRecorderModuleCallback {
    /**
     * 状态更新
     *
     * @param errorType @see RtmpErrorType
     */
    void recordeStatus(int errorType);
}

