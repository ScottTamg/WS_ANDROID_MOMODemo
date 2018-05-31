package com.wushuangtech.library;

import com.wushuangtech.api.EnterConfApi;
import com.wushuangtech.api.ExternalVideoModule;
import com.wushuangtech.jni.RoomJni;

/**
 * SDK所使用的常量
 */
public class Constants {

    // 进入房间的错误码
    /**
     * 进入直播,通信或游戏房间超时，10秒未收到服务器返回结果。
     */
    public static final int ERROR_ENTER_ROOM_TIMEOUT = 1;
    /**
     * 进入直播,通信或游戏房间失败，未收到服务应答。
     */
    public static final int ERROR_ENTER_ROOM_FAILED = 2;
    /**
     * 进入直播,通信或游戏房间时验证错误。
     */
    public static final int ERROR_ENTER_ROOM_VERIFY_FAILED = 3;
    /**
     * 进入直播,通信或游戏房间所用的版本是错误的。
     */
    public static final int ERROR_ENTER_ROOM_BAD_VERSION = 4;
    /**
     * 进入直播,通信或游戏房间时，该房间不存在
     */
    public static final int ERROR_ENTER_ROOM_UNKNOW = 6;
    /**
     * 进入直播,通信或游戏房间时所填的房间名不符合数字格式
     */
    public static final int ERROR_ENTER_ROOM_INVALIDCHANNELNAME = 7;

    //DISCONNECT RESOON 会议被踢出原因
    /**
     * 被房主移出房间。
     */
    public static final int ERROR_KICK_BY_HOST = 201;
    /**
     * RTMP推流失败。
     */
    public static final int ERROR_KICK_BY_PUSHRTMPFAILED = 202;
    /**
     * 服务器过载。
     */
    public static final int ERROR_KICK_BY_SERVEROVERLOAD = 203;
    /**
     * 房主已经离开房间。
     */
    public static final int ERROR_KICK_BY_MASTER_EXIT = 204;
    /**
     * 重复登录。
     */
    public static final int ERROR_KICK_BY_RELOGIN = 205;
    /**
     * 长时间没有上行音频数据。
     */
    public static final int ERROR_KICK_BY_NOAUDIODATA = 206;
    /**
     * 长时间没有上行视频数据。
     */
    public static final int ERROR_KICK_BY_NOVIDEODATA = 207;
    /**
     * 其他人以主播身份进入房间。
     */
    public static final int ERROR_KICK_BY_NEWCHAIRENTER = 208;
    /**
     * CHANNEL KEY失效了。
     */
    public static final int ERROR_KICK_BY_CHANNELKEYEXPIRED = 209;
    /**
     * 跨房间连麦超时。
     */
    public static final int ERROR_LINK_OTHER_ANCHOR_TIMEOUT = 211;
    /**
     * 跨房间连麦重复连麦。
     */
    public static final int ERROR_LINK_OTHER_ANCHOR_RELINK = 212;

    //用户角色
    /**
     * 主播角色
     */
    public static final int CLIENT_ROLE_ANCHOR = RoomJni.ROOM_UR_CHAIRMAN;
    /**
     * 副播角色
     */
    public static final int CLIENT_ROLE_BROADCASTER = RoomJni.ROOM_UR_PARTICIPANT;
    /**
     * 观众角色(默认)
     */
    public static final int CLIENT_ROLE_AUDIENCE = RoomJni.ROOM_UR_AUDIANCE;

    //频道模式
    /**
     * 通信频道模式,申请发言服务器自动同意
     */
    public static final int CHANNEL_PROFILE_COMMUNICATION = EnterConfApi.RoomMode.ROOM_MODE_COMMUNICATION.ordinal();
    /**
     * 直播频道模式,申请发言主播需同意
     */
    public static final int CHANNEL_PROFILE_LIVE_BROADCASTING = EnterConfApi.RoomMode.ROOM_MODE_LIVE.ordinal();
    /**
     * 游戏频道模式
     */
    public static final int CHANNEL_PROFILE_GAME_FREE_MODE = EnterConfApi.RoomMode.ROOM_MODE_GAME_FREE.ordinal();

    /**
     * 判断是不是UNITY使用SDK
     */
    public static boolean IS_UNITY = false;


    // 视频的显示模式
    /**
     * 如果视频尺寸与显示视窗尺寸不一致，在保持长宽比的前提下，将视频进行缩放后填满视窗。
     */
    public static final int RENDER_MODE_FIT = 0;
    /**
     * 如果视频尺寸与显示视窗尺寸不一致，则视频流会按照显示视窗的比例进行周边裁剪或图像拉伸后填满视窗。
     */
    public static final int RENDER_MODE_HIDDEN = 1;
    /**
     * 如果自己和对方都是竖屏，或者如果自己和对方都是横屏，使用RENDER_MODE_HIDDEN；
     * 如果对方和自己一个竖屏一个横屏，则使用RENDER_MODE_FIT。
     */
    public static final int RENDER_MODE_ADAPTIVE = 2;

    // 日志过滤等级
    /**
     * 只显示INFO或以上信息
     */
    public static final int LOG_FILTER_INFO = 100;
    /**
     * 只显示DEBUG或以上信息
     */
    public static final int LOG_FILTER_DEBUG = 101;
    /**
     * 只显示WARN或以上信息
     */
    public static final int LOG_FILTER_WARNING = 102;
    /**
     * 只显示ERROR或以上信息
     */
    public static final int LOG_FILTER_ERROR = 103;
    /**
     * 关闭过滤日志等级
     */
    public static final int LOG_FILTER_OFF = 104;

    // 预设的视频质量等级
    /**
     * 160*120,15fps,65kbps
     */
    public static final int VIDEO_PROFILE_120P = 110;
    /**
     * 320x180,15fps,140kbps
     */
    public static final int VIDEO_PROFILE_180P = 111;
    /**
     * 320x240,15fps,200kbps
     */
    public static final int VIDEO_PROFILE_240P = 112;
    /**
     * 640x360,15fps,400kbps
     */
    public static final int VIDEO_PROFILE_360P = 113;
    /**
     * 640x480,15fps,500kbps
     */
    public static final int VIDEO_PROFILE_480P = 114;
    /**
     * 1280x720,15fps,1130kbps
     */
    public static final int VIDEO_PROFILE_720P = 115;
    /**
     * 1920x1080,15fps,2080kbps
     */
    public static final int VIDEO_PROFILE_1080P = 116;
    /**
     * 默认360P
     */
    public static final int VIDEO_PROFILE_DEFAULT = VIDEO_PROFILE_360P;
    // 用户离开频道的原因
    /**
     * 对方主动离开频道
     */
    public static final int USER_OFFLINE_QUIT = 200;
    /**
     * 对方掉线
     */
    public static final int USER_OFFLINE_DROPPED = 201;
    /**
     * 其他人从主播或副播身份转为观众。
     */
    public static final int USER_OFFLINE_BECOMEAUDIENCE = 202;

    // 默认音频路由设置
    /**
     * 默认路由耳机或蓝牙
     */
    public static final int AUDIO_ROUTE_HEADSET = 0;
    /**
     * 默认路由扬声器
     */
    public static final int AUDIO_ROUTE_SPEAKER = 1;
    /**
     * 默认路由手机听筒
     */
    public static final int AUDIO_ROUTE_HEADPHONE = 2;
    /**
     * 远端视频设置为大流，需启用双流功能
     */
    public static final int VIDEO_STEAM_TYPE_BIG = 10;
    /**
     * 远端视频设置为小流，需启用双流功能
     */
    public static final int VIDEO_STEAM_TYPE_SMALL = 11;

    // ----------------------------后来加的-------------------------------------

    /**
     * 画质优先，此项为默认设置
     */
    public static final int VIDEO_ADJ_PREFER_CLARITY = ExternalVideoModule.VideoAdjustmentMode.VIDEO_ADJ_PREFER_CLARITY.ordinal();
    /**
     * 流畅度优先
     */
    public static final int VIDEO_ADJ_PREFER_CONTINUITY = ExternalVideoModule.VideoAdjustmentMode.VIDEO_ADJ_PREFER_CONTINUITY.ordinal();

    /**
     * IjkPlayer发生错误。
     */
    public static final int ERROR_KICK_BY_IJKPLAYER_EEROR = 210;

    // IjkPlayer显示的几种模式
    /**
     * 可能会剪裁,保持原视频的大小，显示在中心,当原视频的大小超过view的大小超过部分裁剪处理
     */
    public static final int AR_ASPECT_FIT_PARENT = 0;
    /**
     * 可能会剪裁,等比例放大视频，直到填满View为止,超过View的部分作裁剪处理
     */
    public static final int AR_ASPECT_FILL_PARENT = 1;
    /**
     * 将视频的内容完整居中显示，如果视频大于view,则按比例缩视频直到完全显示在view中
     */
    public static final int AR_ASPECT_WRAP_CONTENT = 2;
    /**
     * 不剪裁,非等比例拉伸画面填满整个View
     */
    public static final int AR_MATCH_PARENT = 3;
    /**
     * 不剪裁,非等比例拉伸画面到16:9,并完全显示在View中
     */
    public static final int AR_16_9_FIT_PARENT = 4;
    /**
     * 不剪裁,非等比例拉伸画面到4:3,并完全显示在View中
     */
    public static final int AR_4_3_FIT_PARENT = 5;

    // 底层的日志控制级别
    public static final int LEVEL_LOG_OFF = 0;
    public static final int LEVEL_LOG_FATAL = 1;
    public static final int LEVEL_LOG_ERROR = 2;
    public static final int LEVEL_LOG_WARNING = 3;
    public static final int LEVEL_LOG_INFO = 4;
    public static final int LEVEL_LOG_DEBUG = 5;
}
