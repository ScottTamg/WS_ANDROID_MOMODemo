package com.wushuangtech.library.screenrecorder;

/**
 * Record callback
 */
public interface RecordCallback {

    /**
     * Callback when record successfully
     * @param filePath recorded MP4 file path
     */
    void onRecordSuccess(String filePath, long duration);

    /**
     * Callback when record failed
     * @param e reason why it failed
     */
    void onRecordFailed(Throwable e, long duration);

    /**
     * Record progress changed
     * @param s current record duration in s
     */
    void onRecordedDurationChanged(int s);
}
