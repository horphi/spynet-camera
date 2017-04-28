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

package com.spynet.camera.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.net.InetAddress;

/**
 * Helper class to handle the database where the incoming connections are logged.
 */
public class ConnectionsDbHelper extends SQLiteOpenHelper {

    /**
     * Creates a new ConnectionsDbHelper object
     *
     * @param context the {@link Context} to use to open or create the database
     */
    public ConnectionsDbHelper(Context context) {
        super(context, ConnectionsContract.DATABASE_NAME, null, ConnectionsContract.DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(ConnectionsContract.ConnectionsTable.SQL_CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Recreate the database
        db.execSQL(ConnectionsContract.ConnectionsTable.SQL_DELETE_TABLE);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    /**
     * Creates a new entry in the database.
     *
     * @param host           the remote host IP address or name
     * @param userAgent      the user-agent
     * @param info           additional information
     * @param streamID       the ID of the stream (0 if this is not a stream log)
     * @param startTimestamp the start timestamp
     * @param stopTimestamp  the stop timestamp (0 if not yet available)
     */
    public void log(InetAddress host, String userAgent, String info,
                    long streamID, long startTimestamp, long stopTimestamp) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues entry = new ContentValues();
        entry.put(ConnectionsContract.ConnectionsTable.COLUMN_NAME_HOST_NAME, host.getHostName());
        entry.put(ConnectionsContract.ConnectionsTable.COLUMN_NAME_HOST_IP, host.getHostAddress());
        entry.put(ConnectionsContract.ConnectionsTable.COLUMN_NAME_USERAGENT, userAgent);
        entry.put(ConnectionsContract.ConnectionsTable.COLUMN_NAME_INFO, info);
        entry.put(ConnectionsContract.ConnectionsTable.COLUMN_NAME_STREAM, streamID);
        entry.put(ConnectionsContract.ConnectionsTable.COLUMN_NAME_START, startTimestamp);
        entry.put(ConnectionsContract.ConnectionsTable.COLUMN_NAME_STOP, stopTimestamp);
        db.insert(ConnectionsContract.ConnectionsTable.TABLE_NAME, null, entry);
        db.close();
    }

    /**
     * Adds the stop timestamp to a previously logged entry.
     *
     * @param host          the remote host that started the connection
     * @param streamID      the ID of the stream
     * @param stopTimestamp the stop timestamp
     */
    public void log(InetAddress host, long streamID, long stopTimestamp) {
        if (host == null)
            throw new IllegalArgumentException("host == null");
        if (streamID == 0)
            throw new IllegalArgumentException("streamID == 0");
        if (stopTimestamp == 0)
            throw new IllegalArgumentException("stopTimestamp == 0");
        SQLiteDatabase db = getWritableDatabase();
        ContentValues entry = new ContentValues();
        entry.put(ConnectionsContract.ConnectionsTable.COLUMN_NAME_STOP, stopTimestamp);
        db.update(ConnectionsContract.ConnectionsTable.TABLE_NAME, entry,
                ConnectionsContract.ConnectionsTable.COLUMN_NAME_HOST_IP + " = ? AND " +
                        ConnectionsContract.ConnectionsTable.COLUMN_NAME_STREAM + " = ?",
                new String[]{host.getHostAddress(), String.valueOf(streamID)});
        db.close();
    }

    /**
     * Drops the entries that are older than the specified timestamp.
     *
     * @param timestamp the timestamp
     */
    public void drop(long timestamp) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(ConnectionsContract.ConnectionsTable.TABLE_NAME,
                ConnectionsContract.ConnectionsTable.COLUMN_NAME_START + " < ?",
                new String[]{String.valueOf(timestamp)});
        db.close();
    }
}
