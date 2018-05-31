package com.wushuangtech.bean;


import com.google.tttgson.annotations.Expose;

/**
 * Created by Administrator on 2017-12-18.
 */

public class ChatInfo {

    @Expose(serialize = false)
    public int chatType;        // 聊天类型
    @Expose(serialize = false)
    public String seqID;        // 唯一标识
    @Expose
    public String chatData;     // 聊天内容
    @Expose
    public int audioDuration;  // 音频时长（单位“秒”，chatType为“AUDIO”）

}
