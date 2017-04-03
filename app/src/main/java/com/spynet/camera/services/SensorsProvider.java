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

package com.spynet.camera.services;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Defines the provider that will notify the client on sensors values changes.<br>
 * It handles two different update modes: coarse (default) and fine.<br>
 * 'coarse' is for power-saving, while 'fine' is for accuracy.<br>
 * It is possible to switch between the two modes, but the coarse mode will be automatically
 * selected if no new requests are received for the fine mode within a predefined timeout time.
 */
public class SensorsProvider implements Closeable, SensorEventListener {

    // Sampling rate in microseconds
    private static final int SENSOR_DELAY_COARSE = 10 * 1000 * 1000;
    private static final int SENSOR_DELAY_FINE = 1000 * 1000;
    // The fine mode timeout, in milliseconds
    private static final int FINE_TIMEOUT = 30 * 1000;

    protected final String TAG = getClass().getSimpleName();

    private final Context mContext;                     // The context that uses the SensorsProvider
    private final SensorManager mSensorManager;         // The sensor manager
    private final Timer mTimeoutTimer;                  // The timer used to switch back to coarse mode
    private SensorsCallback mCallback;                  // The SensorsCallback implemented by mContext
    private volatile boolean mIsFineMode;               // Whether the fine location is active
    private volatile int mFineRequestsNum;              // The number of fine location requests (since last check)

    /**
     * A client may implement this interface to receive sensors values as they are available.
     */
    public interface SensorsCallback {
        /**
         * Called when a new value is available for the specified sensor.
         *
         * @param sensorType the type of sensor
         * @param value      the sensor value
         */
        void onSensorValueAvailable(int sensorType, float value);
    }

    /**
     * Creates a new SensorsProvider object.
     *
     * @param context the context where the SensorsProvider is used; it should implement
     *                {@link SensorsCallback}
     */
    public SensorsProvider(@NotNull Context context) {
        mContext = context;
        if (mContext instanceof SensorsCallback) {
            mCallback = (SensorsCallback) mContext;
        } else {
            Log.w(TAG, "SensorsCallback is not implemented by the specified context");
        }
        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        mIsFineMode = true; // allow requestCoarseUpdates() to execute
        requestCoarseUpdates();
        mTimeoutTimer = new Timer();
        mTimeoutTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                synchronized (SensorsProvider.this) {
                    if (mFineRequestsNum == 0 && mIsFineMode) {
                        requestCoarseUpdates();
                    }
                    mFineRequestsNum = 0;
                }
            }
        }, FINE_TIMEOUT, FINE_TIMEOUT);
    }

    /**
     * Closes the LocationProvider.
     */
    @Override
    public void close() {
        mTimeoutTimer.cancel();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (mCallback != null) {
            mCallback.onSensorValueAvailable(event.sensor.getType(), event.values[0]);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.v(TAG, "sensor accuracy changed: " + sensor.getName() + "=" + accuracy);
    }

    /**
     * Sets location updates for power saving.
     */
    public void requestCoarseUpdates() {
        synchronized (this) {
            if (!mIsFineMode)
                return;
            mIsFineMode = false;
            mFineRequestsNum = 0;
        }
        mSensorManager.unregisterListener(this);
        Sensor sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        if (sensor != null)
            mSensorManager.registerListener(this, sensor, SENSOR_DELAY_COARSE);
    }

    /**
     * Sets location updates for better performance.
     */
    public synchronized void requestFineUpdates() {
        synchronized (this) {
            mFineRequestsNum++;
            if (mIsFineMode)
                return;
            mIsFineMode = true;
        }
        mSensorManager.unregisterListener(this);
        Sensor sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        if (sensor != null)
            mSensorManager.registerListener(this, sensor, SENSOR_DELAY_FINE);
    }
}
