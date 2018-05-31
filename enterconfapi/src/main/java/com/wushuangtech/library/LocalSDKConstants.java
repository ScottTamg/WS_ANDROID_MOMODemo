package com.wushuangtech.library;

/**
 * 函数调用返回的通用错误码
 */
public class LocalSDKConstants {
    /**
     * 函数调用成功
     */
    public static final int FUNCTION_SUCCESS = 0;
    /**
     * 调用的时候内部发送错误，出现空值，则调用失败，返回-1。
     */
    public static final int ERROR_FUNCTION_ERROR_EMPTY_VALUE = -1;
    /**
     * 函数请求过于频繁，已经在进行中
     */
    public static final int ERROR_FUNCTION_BUSY = -2;
    /**
     * 当前已处于该函数调用后的状态，无需再调用
     */
    public static final int ERROR_FUNCTION_STATED = -3;
    /**
     * 调用该函数的时机不对或前置操作没做
     */
    public static final int ERROR_FUNCTION_INVOKE_ERROR = -4;
    /**
     * 调用函数所传递的参数有问题
     */
    public static final int ERROR_FUNCTION_ERROR_ARGS = -5;

    public static final int ERROR_FUNCTION_ERROR_MODULE_CLOSE= -6;

    // 创建日志文件失败
    /**
     * 创建日志文件发生未知异常
     */
    public static final int LOG_CREATE_ERROR_UNKNOW = 31;
    /**
     * 指定存储的文件不存在或不是文件类型
     */
    public static final int LOG_CREATE_ERROR_NO_FILE = 30;

    // IJK MODULE 相关
    public static final int IJK_INIT_AUDIO_PLAYER = 0;

    public static final int IJK_INIT_VIDEO_PLAYER = 1;

    public static final int AUDIO_MIX_PLAY_FAILED= -10;

    /**
     * 屏幕录制请求码
     */
    public static final int CAPTURE_REQUEST_CODE = 8080;
}
