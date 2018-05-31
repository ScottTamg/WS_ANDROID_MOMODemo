package com.wushuangtech.wstechapi.internal;

import com.wushuangtech.api.ExternalRtmpPublishModule;
import com.wushuangtech.audiocore.MyAudioApi;
import com.wushuangtech.library.GlobalConfig;
import com.wushuangtech.library.LocalSDKConstants;
import com.wushuangtech.wstechapi.model.TTTLocalModuleConfig;
import com.wushuangtech.wstechapi.model.TTTRtmpModuleConstants;

/**
 * Created by wangzhiguo on 18/2/28.
 */

public class TTTRtmpModule {

    private volatile static TTTRtmpModule holder;

    private TTTRtmpModule() {
    }

    public static TTTRtmpModule getInstance() {
        if (holder == null) {
            synchronized (TTTRtmpModule.class) {
                if (holder == null) {
                    holder = new TTTRtmpModule();
                }
            }
        }
        return holder;
    }

    Object receiveRtmpModuleEvent(TTTLocalModuleConfig config) {
        if (config == null || !GlobalConfig.mIsUseRtmpModule) {
            return null;
        }

        switch (config.eventType) {
            case TTTRtmpModuleConstants.RTMP_INIT:
                MyAudioApi instance = MyAudioApi.getInstance();
                if (instance != null) {
                    ExternalRtmpPublishModule.getInstance()
                            .setExternalAudioModuleCallback(MyAudioApi.getInstance());
                    instance.addAudioSender(ExternalRtmpPublishModule.getInstance());
                }
                break;
            case TTTRtmpModuleConstants.RTMP_START_PUSH:
                return ExternalRtmpPublishModule.getInstance().startPublish((String) config.objs[0]);
            case TTTRtmpModuleConstants.RTMP_STOP_PUSH:
                return ExternalRtmpPublishModule.getInstance().stopPublish();
            case TTTRtmpModuleConstants.RTMP_PAUSE:
                ExternalRtmpPublishModule.getInstance().setIsPause(true);
                return LocalSDKConstants.FUNCTION_SUCCESS;
            case TTTRtmpModuleConstants.RTMP_RESUME:
                ExternalRtmpPublishModule.getInstance().setIsPause(false);
                return LocalSDKConstants.FUNCTION_SUCCESS;
        }
        return LocalSDKConstants.ERROR_FUNCTION_ERROR_EMPTY_VALUE;
    }
}
