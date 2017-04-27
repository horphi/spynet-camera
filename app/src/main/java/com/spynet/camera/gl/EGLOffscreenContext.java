/*
 * This file is part of spyNet Camera, the Android IP camera
 *
 * Copyright (C) 2016-2017 Paolo Dematteis
 *
 * spyNet Camera is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * spyNet Camera is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Paolo Dematteis - spynet314@gmail.com
 */

package com.spynet.camera.gl;

import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.util.Log;

/**
 * Represents an EGL context associated with a pbuffer surface, used for offscreen rendering.<br>
 * The OpenGL ES specification is intentionally vague on how a rendering context
 * (an abstract OpenGL ES state machine) is created. One of the purposes of EGL is
 * to provide a means to create an OpenGL ES context and associate it with a surface.
 */
public class EGLOffscreenContext {

    private static final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;

    private final String TAG = getClass().getSimpleName();

    private final EGLDisplay mDisplay;          // The EGL display
    private final EGLConfig mConfig;            // The EGL configuration
    private final EGLSurface mSurface;          // The EGL surface
    private final EGLContext mContext;          // The EGL context

    /**
     * Attribute list to create a context that supports RGBA pbuffers
     * that can be rendered using OpenGL ES 2.0.
     */
    private static final int[] CONFIG_PIXEL_RGBA_BUFFER = {
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
            EGL14.EGL_NONE
    };

    /**
     * Creates a new EGLOffscreenContext object.
     *
     * @param width  the width in pixel of the offscreen surface
     * @param height the height in pixel of the offscreen surface
     */
    public EGLOffscreenContext(int width, int height) {

        // Initialization (must be performed once for each display)
        mDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (mDisplay == EGL14.EGL_NO_DISPLAY)
            throw new RuntimeException("can't get the default display");
        int[] version = new int[2];
        if (!EGL14.eglInitialize(mDisplay, version, 0, version, 1))
            throw new RuntimeException("can't initialize EGL");
        Log.d(TAG, "EGL version " + version[0] + "." + version[1]);

        // Choose the configuration that better matches the specified attributes
        int[] numConfigs = new int[1];
        EGLConfig[] eglConfig = new EGLConfig[1];
        if (!EGL14.eglChooseConfig(mDisplay, CONFIG_PIXEL_RGBA_BUFFER, 0, eglConfig, 0, 1, numConfigs, 0))
            throw new RuntimeException("can't get a valid configuration");
        mConfig = eglConfig[0];

        // Create the off-screen rendering surface
        int[] surfaceAttribList = new int[]{
                EGL14.EGL_WIDTH, width,
                EGL14.EGL_HEIGHT, height,
                EGL14.EGL_NONE
        };
        mSurface = EGL14.eglCreatePbufferSurface(mDisplay, mConfig, surfaceAttribList, 0);
        if (mSurface == EGL14.EGL_NO_SURFACE)
            throw new RuntimeException("can't create the surface");

        // Create the context
        int[] contextAttribList = new int[]{
                EGL_CONTEXT_CLIENT_VERSION, 2,  // OpenGL ES 2.x context
                EGL14.EGL_NONE
        };
        mContext = EGL14.eglCreateContext(mDisplay, mConfig, EGL14.EGL_NO_CONTEXT, contextAttribList, 0);
        if (mContext == EGL14.EGL_NO_CONTEXT)
            throw new RuntimeException("can't create the context");
    }

    /**
     * Releases all the resources previously allocated.
     */
    public void release() {
        releaseCurrent();
        EGL14.eglDestroyContext(mDisplay, mContext);
        EGL14.eglDestroySurface(mDisplay, mSurface);
        EGL14.eglReleaseThread();
        EGL14.eglTerminate(mDisplay);
    }

    /**
     * Binds the context to the current rendering thread and to the surface.
     */
    public void makeCurrent() {
        if (!EGL14.eglMakeCurrent(mDisplay, mSurface, mSurface, mContext))
            throw new RuntimeException("can't bind the context");
    }

    /**
     * Releases the current context.
     */
    public void releaseCurrent() {
        if (!EGL14.eglMakeCurrent(mDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT))
            throw new RuntimeException("can't release the context");
    }
}
