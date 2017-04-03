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
 * Sensors are automatically disabled if no new requests are received
 * within a predefined timeout time.
 */
public class SensorsProvider implements Closeable, SensorEventListener {

    // The fine mode timeout, in milliseconds
    private static final int FINE_TIMEOUT = 30 * 1000;

    protected final String TAG = getClass().getSimpleName();

    private final Context mContext;                 // The context that uses the SensorsProvider
    private final SensorManager mSensorManager;     // The sensor manager
    private final Timer mTimeoutTimer;              // The timer used to switch back to coarse mode
    private SensorsCallback mCallback;              // The SensorsCallback implemented by mContext
    private boolean mIsActive;                      // Whether the provider is active
    private int mRequestsNum;                       // The number of requests (since last check)

    /**
     * A client may implement this interface to receive sensors values as they are available.
     */
    public interface SensorsCallback {
        /**
         * Called when a new value is available for the specified sensor.
         *
         * @param type  the type of sensor
         * @param value the sensor value
         */
        void onValueAvailable(int type, float value);
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
        mIsActive = false;
        mTimeoutTimer = new Timer();
        mTimeoutTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                synchronized (SensorsProvider.this) {
                    if (mRequestsNum == 0 && mIsActive) {
                        stop();
                    }
                    mRequestsNum = 0;
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
            mCallback.onValueAvailable(event.sensor.getType(), event.values[0]);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.v(TAG, "sensor accuracy changed: " + sensor.getName() + "=" + accuracy);
    }

    /**
     * Starts the monitor.
     */
    public synchronized void start() {
        synchronized (this) {
            mRequestsNum++;
            if (mIsActive)
                return;
            mIsActive = true;
        }
        Sensor sensor;
        sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        if (sensor != null)
            mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY);
        if (sensor != null)
            mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        if (sensor != null)
            mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        if (sensor != null)
            mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    /**
     * Stops the monitor.
     */
    public void stop() {
        synchronized (this) {
            if (!mIsActive)
                return;
            mIsActive = false;
            mRequestsNum = 0;
        }
        mSensorManager.unregisterListener(this);
    }
}
