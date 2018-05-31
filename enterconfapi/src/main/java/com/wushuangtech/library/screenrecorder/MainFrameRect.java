/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wushuangtech.library.screenrecorder;


import com.wushuangtech.library.screenrecorder.gles.Drawable2d;
import com.wushuangtech.library.screenrecorder.gles.GlUtil;
import com.wushuangtech.library.screenrecorder.gles.Texture2dProgram;

/**
 * This class is used to cut the top and bottom area of the screen and
 * just keep the center main part.
 *
 * In this demo, we'll cut the status bar and navigation bar of the screen
 */
public class MainFrameRect {
    private final CroppedDrawable2d mRectDrawable;
    private Texture2dProgram mProgram;

    /**
     * Prepares the object.
     *
     * @param program The program to use.  FullFrameRect takes ownership, and will release
     *     the program when no longer needed.
     */
    public MainFrameRect(Texture2dProgram program) {
        mRectDrawable = new CroppedDrawable2d(Drawable2d.Prefab.FULL_RECTANGLE);
        mProgram = program;
    }

    /**
     * Releases resources.
     * <p>
     * This must be called with the appropriate EGL context current (i.e. the one that was
     * current when the constructor was called).  If we're about to destroy the EGL context,
     * there's no value in having the caller make it current just to do this cleanup, so you
     * can pass a flag that will tell this function to skip any EGL-context-specific cleanup.
     */
    public void release(boolean doEglCleanup) {
        if (mProgram != null) {
            if (doEglCleanup) {
                mProgram.release();
            }
            mProgram = null;
        }
    }

    /**
     * Returns the program currently in use.
     */
    public Texture2dProgram getProgram() {
        return mProgram;
    }

    /**
     * Changes the program.  The previous program will be released.
     * <p>
     * The appropriate EGL context must be current.
     */
    public void changeProgram(Texture2dProgram program) {
        mProgram.release();
        mProgram = program;
    }

    /**
     * @param bottomCropped defines the bottom area to be cut. from 0f-1f.
     */
    public void setBottomCropped(float bottomCropped) {
        mRectDrawable.setBottomCropped(bottomCropped);
    }

    /**
     * @param topCropped defines the top area to be cut. from 0f-1f.
     */
    public void setTopCropped(float topCropped) {
        mRectDrawable.setTopCropped(topCropped);
    }

    /**
     * Creates a texture object suitable for use with drawFrame().
     */
    public int createTextureObject() {
        return mProgram.createTextureObject();
    }

    /**
     * Draws a viewport-filling rect, texturing it with the specified texture object.
     */
    public void drawFrame(int textureId, float[] texMatrix) {
        // Use the identity matrix for MVP so our 2x2 FULL_RECTANGLE covers the viewport.
        mProgram.draw(GlUtil.IDENTITY_MATRIX, mRectDrawable.getVertexArray(), 0,
                mRectDrawable.getVertexCount(), mRectDrawable.getCoordsPerVertex(),
                mRectDrawable.getVertexStride(),
                texMatrix, mRectDrawable.getTexCoordArray(), textureId,
                mRectDrawable.getTexCoordStride());
    }
}
