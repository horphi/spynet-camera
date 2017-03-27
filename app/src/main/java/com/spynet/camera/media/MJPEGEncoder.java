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

package com.spynet.camera.media;

import android.util.Log;

import com.spynet.camera.common.Image;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Defines an MJPEG video encoder.
 */
public class MJPEGEncoder implements Closeable {

    private final String TAG = getClass().getSimpleName();

    private static final int QUEUE_CAPACITY = 5;        // Frame queue capacity
    private static final int QUEUE_WRITE_TIMEOUT = 5;   // Timeout to write to the queue in ms
    private static final int QUEUE_READ_TIMEOUT = 1000; // Timeout to read from the queue in ms

    private final BlockingQueue<VideoFrame> mQueue;     // The queue used to pass video data to the MJPEGEncoder
    private final CodecCallback mCodecCallback;         // The CodecCallback implemented by the client
    private Thread mEncoderThread;                      // The encoding thread
    private volatile long mFrameDelay;                  // The delay between frames in microseconds
    private volatile int mQuality;                      // The JPEG compression quality

    /**
     * A client may implement this interface to receive data buffers as they are available.
     */
    public interface CodecCallback {
        /**
         * Called when new data is available.<br>
         * This callback is invoked on the encoder thread.
         *
         * @param encoder   the MJPEGEncoder that called this callback
         * @param data      encoded video data
         * @param width     the desired width for pictures, in pixels
         * @param height    the desired height for pictures, in pixels
         * @param timestamp the timestamp in microseconds
         */
        void onDataAvailable(MJPEGEncoder encoder, byte[] data, int width, int height, long timestamp);
    }

    /**
     * Creates a new MJPEGEncoder object.
     *
     * @param callback the callback to receive the encoded video data
     */
    public MJPEGEncoder(CodecCallback callback) {
        mQueue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
        mCodecCallback = callback;
    }

    /**
     * Pushes a new frame to the encoder queue.<br>
     * If the queue is full, the frame is silently dropped.
     *
     * @param frame the frame to be processed
     * @return true if the frame has been added successfully, false if it was dropped
     * @throws InterruptedException if interrupted while waiting
     */
    public boolean push(VideoFrame frame) throws InterruptedException {
        return mQueue.offer(frame, QUEUE_WRITE_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    /**
     * Pops a frame from the encoder queue.
     *
     * @return the retrieved Frame, null if the operation timed out
     * @throws InterruptedException if interrupted while waiting
     */
    private VideoFrame pop() throws InterruptedException {
        return mQueue.poll(QUEUE_READ_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    /**
     * Starts the video encoder.
     *
     * @param quality   the JPEG quality
     * @param framerate the desired frame rate
     */
    public void open(int quality, double framerate) {
        mFrameDelay = (long) (1000000.0 / framerate);
        mQuality = quality;
        mEncoderThread = new Thread(new Runnable() {
            @Override
            public void run() {
                doEncode();
            }
        });
        mEncoderThread.start();
    }

    @Override
    public void close() {
        if (mEncoderThread != null) {
            mEncoderThread.interrupt();
            try {
                mEncoderThread.join();
            } catch (InterruptedException e) {
                Log.e(TAG, "stop encoding interrupted");
            }
            mEncoderThread = null;
        }
    }

    /**
     * Encodes the incoming video frames.
     */
    private void doEncode() {
        VideoFrame frame;
        long lastTime = 0;
        try {
            Log.v(TAG, "start encoding");
            while (!Thread.currentThread().isInterrupted()) {
                // Get a frame from the queue
                if ((frame = pop()) == null)
                    continue;
                // Control the fps
                if (frame.getTimestamp() < lastTime + mFrameDelay)
                    continue;
                lastTime = frame.getTimestamp();
                // Compress the JPEG image
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                Image.compressToJpeg(frame.getData(), frame.getWidth(), frame.getHeight(),
                        frame.getFormat(), mQuality, out);
                if (mCodecCallback != null) {
                    mCodecCallback.onDataAvailable(this, out.toByteArray(),
                            frame.getWidth(), frame.getHeight(), frame.getTimestamp());
                }
            }
        } catch (InterruptedException e) {
            Log.v(TAG, "encoder interrupted");
        } finally {
            Log.v(TAG, "stop encoding");
        }
    }
}
