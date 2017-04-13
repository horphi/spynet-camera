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

import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Implements a renderer for rendering a texture onto a surface using OpenGL ES 2.0.
 */
public class NV21TextureRender {

    private static final int FLOAT_SIZE_BYTES = 4;
    private static final int TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES;
    private static final int TRIANGLE_VERTICES_DATA_POS_OFFSET_BYTES = 0;
    private static final int TRIANGLE_VERTICES_DATA_TEX_OFFSET_BYTES = 3 * FLOAT_SIZE_BYTES;
    private static final float[] mTriangleVerticesData = {
            // Positions        // Texture Coords
            -1.0f, -1.0f, 0.0f, 0.0f, 1.0f,
            +1.0f, -1.0f, 0.0f, 1.0f, 1.0f,
            -1.0f, +1.0f, 0.0f, 0.0f, 0.0f,
            +1.0f, +1.0f, 0.0f, 1.0f, 0.0f
    };
    private static final FloatBuffer mTriangleVertices;
    private static final String VERTEX_SHADER = "" +
            "attribute vec3 aPosition;\n" +
            "attribute vec2 aTextureCoord;\n" +
            "varying vec2 vTextureCoord;\n" +
            "void main() {\n" +
            "  vTextureCoord = aTextureCoord;\n" +
            "  gl_Position = vec4(aPosition, 1.0);\n" +
            "}\n";
    private static final String FRAGMENT_SHADER = "" +
            "precision mediump float;\n" +
            "varying vec2 vTextureCoord;\n" +
            "uniform sampler2D sTextureY;\n" +
            "void main() {\n" +
            "  float y = texture2D(sTextureY, vTextureCoord).r;\n" +
            "  gl_FragColor = vec4(y, 0.0, 0.0, 1.0);\n" +
            "}\n";

    private final Shader mShader;                       // The shader program
    private final int mVBO;                             // The vertex buffer object ID
    private final int mTex;                             // The texture ID
    private final int maPositionLocation;               // The location of the 'aPosition' attribute
    private final int maTextureLocation;                // The location of the 'aTextureCoord' attribute
    private final int msTextureYLocation;

    ByteBuffer yBuff = ByteBuffer.allocateDirect(720 * 480);

    // Initializes static data
    static {
        mTriangleVertices = ByteBuffer.allocateDirect(
                mTriangleVerticesData.length * FLOAT_SIZE_BYTES)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mTriangleVertices.put(mTriangleVerticesData).rewind();
    }

    /**
     * Creates a new TextureRender object.
     */
    public NV21TextureRender() {

        int[] id = new int[1];

        // Create the program
        mShader = new Shader(VERTEX_SHADER, FRAGMENT_SHADER);

        // Store attributes locations
        maPositionLocation = GLES20.glGetAttribLocation(mShader.program, "aPosition");
        checkGLError("glGetAttribLocation(aPosition)");
        if (maPositionLocation == -1)
            throw new RuntimeException("can't get location for aPosition");
        maTextureLocation = GLES20.glGetAttribLocation(mShader.program, "aTextureCoord");
        checkGLError("glGetAttribLocation(aTextureCoord)");
        if (maTextureLocation == -1)
            throw new RuntimeException("can't get location for aTextureCoord");
        msTextureYLocation= GLES20.glGetUniformLocation(mShader.program, "sTextureY");
        checkGLError("glGetUniformLocation(sTextureY)");
        if (msTextureYLocation== -1)
            throw new RuntimeException("can't get location for sTextureY");

        // Create the vertex buffer object and load vertex data
        GLES20.glGenBuffers(1, id, 0);
        mVBO = id[0];
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVBO);
        checkGLError("glBindBuffer");
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER,
                mTriangleVertices.capacity() * FLOAT_SIZE_BYTES,
                mTriangleVertices, GLES20.GL_STATIC_DRAW);
        checkGLError("glBufferData");
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        // Create and configure the texture
        GLES20.glGenTextures(1, id, 0);
        mTex = id[0];
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTex);
        checkGLError("glBindTexture");
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        checkGLError("glTexParameter");
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    /**
     * Draws the current frame.
     */
    public void draw(byte[] data, int width, int height) {

        // Clear the background color
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        // Select the shader program
        mShader.use();

        yBuff.put(data, 0, width * height);
        yBuff.position(0);

        GLES20.glUniform1i(msTextureYLocation, 0);

        // Bind and configure the VBO and the texture
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVBO);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTex);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, width, height, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, yBuff);
        GLES20.glVertexAttribPointer(maPositionLocation, 3, GLES20.GL_FLOAT, false,
                TRIANGLE_VERTICES_DATA_STRIDE_BYTES,
                TRIANGLE_VERTICES_DATA_POS_OFFSET_BYTES);
        GLES20.glEnableVertexAttribArray(maPositionLocation);
        GLES20.glVertexAttribPointer(maTextureLocation, 2, GLES20.GL_FLOAT, false,
                TRIANGLE_VERTICES_DATA_STRIDE_BYTES,
                TRIANGLE_VERTICES_DATA_TEX_OFFSET_BYTES);
        GLES20.glEnableVertexAttribArray(maTextureLocation);

        // Draw the frame
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        // Block until all GL execution is complete
        GLES20.glFinish();

        // Unbind
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }

    /**
     * Check if GL error has been recorded since the last call to this function.
     * When an error occurs, the error flag is set to the appropriate error code value.
     * No other errors are recorded until glGetError is called, the error code is returned
     * and the flag is reset to GL_NO_ERROR.
     * If a call to glGetError returns GL_NO_ERROR, there has been no detectable error
     * since the last call to glGetError, or since the GL was initialized.
     *
     * @param op the name of the operation, use to create the Exception message
     */
    private static void checkGLError(String op) {
        int error = GLES20.glGetError();
        if (error != GLES20.GL_NO_ERROR)
            throw new RuntimeException(op + " error: " + error);
    }
}
