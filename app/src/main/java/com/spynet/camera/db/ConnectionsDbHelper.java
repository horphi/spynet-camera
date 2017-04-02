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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

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
}
