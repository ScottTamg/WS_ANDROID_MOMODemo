package com.wushuangtech.api;

/**
 * Created by apple on 2016/11/24.
 */

/*
 * 该接口用于发送编码后的音频数据
 * ExternalAudioModule、ExternalPublishModule已实现该接口
 */
public interface AudioSender {
    /**
     * 发送编码的音频数据
     * @param data  aac
     */
    void pushEncodedAudioData(byte[] data);
    void sendSRData(byte[] data,int len);
    void sendNACKData(byte[] data,int len,long userid);
}
