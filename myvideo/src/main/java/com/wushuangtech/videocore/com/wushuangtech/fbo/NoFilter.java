/*
 *
 * NoFilter.java
 * 
 * Created by Wuwang on 2016/11/19
 * Copyright © 2016年 深圳哎吖科技. All rights reserved.
 */
package com.wushuangtech.videocore.com.wushuangtech.fbo;

import android.content.res.Resources;

import com.wushuangtech.bean.ConfVideoFrame;

/**
 * Description:
 */
public class NoFilter extends AFilter {

    public NoFilter(Resources res) {
        super(res);
    }

    @Override
    protected void onCreate() {
        mFormatType = ConfVideoFrame.FORMAT_TEXTURE_2D;
        createProgramByAssetsFile("shader/base_vertex.sh",
            "shader/base_fragment.sh");
    }

    @Override
    protected void onSizeChanged(int width, int height) {

    }
}
