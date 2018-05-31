package com.wushuangtech.library.screenrecorder;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;

import java.lang.ref.WeakReference;

import static android.content.Context.MEDIA_PROJECTION_SERVICE;
import static com.wushuangtech.library.LocalSDKConstants.CAPTURE_REQUEST_CODE;


/**
 * Screen capture
 * process：
 * 1，request for capture permission，
 * 2，start projection， (running)
 * 3，attach encoder， (recording)
 * 4，detach encoder when finish，
 * 5，close projection and destroy
 */
public class ScreenCapture {

    private static final String TAG = "ScreenCapture";
    private final WeakReference<Activity> mActivity; // Prevent memory leak
    private final int mScreenDensity;
    private MediaProjectionManager projectionManager;
    private TextureMovieEncoder mRecorder;
    private int width = 720; // Width of the recorded video
    private int height = 1280; // Height of the recorded video

    private boolean running; // true if it is projecting screen
    private boolean recording; // true if it is recording screen

    private VirtualDisplay virtualDisplay;
    private MediaProjection mediaProjection;
    private OnMediaProjectionReadyListener mMediaProjectionReadyListener;

    public ScreenCapture(Activity activity) {
        mActivity = new WeakReference<>(activity);
        Context context = activity.getApplicationContext();
        projectionManager = (MediaProjectionManager) context.getSystemService(MEDIA_PROJECTION_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mScreenDensity = metrics.densityDpi;
        mRecorder = new TextureMovieEncoder();
    }

    /**
     * @return true when projecting
     */
    public boolean isProjecting() {
        return running;
    }

    /**
     * @return retrun true when recording screen
     */
    public boolean isRecording() {
        return recording;
    }

    public void setRecordCallback(RecordCallback recordCallback) {
        mRecorder.setRecordCallback(recordCallback);
    }

    public void setMediaProjectionReadyListener(OnMediaProjectionReadyListener mediaProjectionReadyListener) {
        this.mMediaProjectionReadyListener = mediaProjectionReadyListener;
    }

    /**
     * Step 1: request permission
     */
    public void requestScreenCapture() {
        Log.d(TAG, "Start requestScreenCapture");
        Intent captureIntent = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            captureIntent = projectionManager.createScreenCaptureIntent();
        }
        mActivity.get().startActivityForResult(captureIntent, CAPTURE_REQUEST_CODE);
    }

    /**
     * Step 2，Init MediaProjection
     *
     * @param data data returned from onActivityResult
     * @return true if success
     */
    public synchronized boolean startProjection(Intent data , EncoderConfig mEncoderConfig) {
        Log.d(TAG, "Start startProjection");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mediaProjection = projectionManager.getMediaProjection(Activity.RESULT_OK, data);
            if (mediaProjection == null) {
                return false;
            }

            if (mMediaProjectionReadyListener != null) {
                mMediaProjectionReadyListener.onMediaProjectionReady(mediaProjection);
            }

            width = mEncoderConfig.mWidth;
            height = mEncoderConfig.mHeight;
            virtualDisplay = mediaProjection.createVirtualDisplay(
                    "LiveScreen",
                    width,
                    height,
                    mScreenDensity,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    null, // we don't need to display by now
                    null, null);
            running = true;
            return true;
        }
        return false;
    }

    /**
     * Step 3，attach encoder to the virtual screen and start to record
     *
     * @return true if attach success
     */
    public synchronized boolean attachRecorder(EncoderConfig mEncoderConfig) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Log.d(TAG, "Start attachRecorder");
            if (!running) {
                // if not projecting screen or already recording return false
                requestScreenCapture();
                return false;
            }
            if (recording) {
                return false;
            }

            mRecorder.startRecording(mEncoderConfig);
            mRecorder.setCallback(new TextureMovieEncoder.Callback() {
                @Override
                public void onInputSurfacePrepared(Surface surface) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
                        if (virtualDisplay != null) {
                            virtualDisplay.setSurface(surface);
                        }
                    }
                }
            });

            recording = true;
            return true;
        }
        return false;
    }

    /**
     * Step 4，detach encoder from virtual screen and stop recoding.
     *
     * @return true if success
     */
    private synchronized boolean detachRecorder() {
        Log.d(TAG, "Start detachRecorder");
        if (!running || !recording) {
            // if not projecting or not recording return false
            return false;
        }
        recording = false;
        mRecorder.stopRecording();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            virtualDisplay.setSurface(null);
        }

        return true;
    }

    /**
     * Step 5：stop projection and destroy
     */
    public boolean stopProjection() {
        Log.d(TAG, "Start stopProjection");
        if (!running) {
            return false;
        }
        if (recording) {
            detachRecorder();
        }
        running = false;
        if (virtualDisplay != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                virtualDisplay.release();
            }
            virtualDisplay = null;
        }
        if (mediaProjection != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mediaProjection.stop();
            }
            mediaProjection = null;
        }

        return true;
    }

    private boolean isCurrentActivity(Activity activity) {
        return mActivity.get() == activity;
    }

    public interface OnMediaProjectionReadyListener {
        void onMediaProjectionReady(MediaProjection mediaProjection);
    }

    static {
        System.loadLibrary("avrecoder");
    }
}
