package com.wushuangtech.api;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.wushuangtech.utils.PviewLog;

/**
 * Created by wangzhiguo on 17/11/13.
 */

public class LogWorkerThread extends Thread{

    private static final int ACTION_WORKER_THREAD_QUIT = 0X1010; // quit this thread

    private static final int ACTION_WORKER_LOG_STATUS = 1;
    private static final long INTERVAL_TIME = 3000;

    private LogWorkerThread.WorkerThreadHandler mWorkerHandler;

    private boolean mReady;

    public final void waitForReady() {
        while (!mReady) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            PviewLog.i("wait for " + LogWorkerThread.class.getSimpleName());
        }
    }

    @Override
    public void run() {
        PviewLog.i("LogWorkerThread start to run");
        Looper.prepare();

        mWorkerHandler = new LogWorkerThread.WorkerThreadHandler(this);

        mReady = true;
        mWorkerHandler.sendEmptyMessageDelayed(ACTION_WORKER_LOG_STATUS, INTERVAL_TIME);
        // enter thread looper
        Looper.loop();
    }

    private static final class WorkerThreadHandler extends Handler {

        private LogWorkerThread mWorkerThread;

        WorkerThreadHandler(LogWorkerThread thread) {
            this.mWorkerThread = thread;
        }

        public void release() {
            mWorkerThread = null;
        }

        @Override
        public void handleMessage(Message msg) {
            if (this.mWorkerThread == null) {
                PviewLog.w("LogWorkerThread handler is already released! " + msg.what);
                return;
            }

            switch (msg.what) {
                case ACTION_WORKER_LOG_STATUS:
                    PviewLog.setIsPrint(true);
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    PviewLog.setIsPrint(false);
                    sendEmptyMessageDelayed(ACTION_WORKER_LOG_STATUS, INTERVAL_TIME);
                    break;
            }
        }

    }

    public LogWorkerThread.WorkerThreadHandler getWorkerHandler() {
        return mWorkerHandler;
    }

    public void sendMessage(int what, Object[] objs) {
        Message msg = new Message();
        msg.what = what;
        msg.obj = objs;
        msg.arg1 = what;
        msg.setTarget(mWorkerHandler);
        msg.sendToTarget();
    }

    /**
     * call this method to exit
     * should ONLY call this method when this thread is running
     */
    public final void exit() {
        if (Thread.currentThread() != this) {
            PviewLog.w("exit() - exit app thread asynchronously");
            mWorkerHandler.sendEmptyMessage(ACTION_WORKER_THREAD_QUIT);
            return;
        }

        mReady = false;

        // TODO should remove all pending(read) messages

        PviewLog.i("exit() > start");
        mWorkerHandler.removeMessages(ACTION_WORKER_LOG_STATUS);
        // exit thread looper
        Looper.myLooper().quit();

        mWorkerHandler.release();

        PviewLog.i("exit() > end");
    }
}
