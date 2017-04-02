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

import android.provider.BaseColumns;

/**
 * Defines the database used to log the incoming connections.
 */
public final class ConnectionsContract {

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "Connections.db";

    /**
     * Hidden constructor, the class cannot be instantiated.
     */
    private ConnectionsContract() {
    }

    /**
     * Describes the table where to log the connections.
     */
    public static class ConnectionsTable implements BaseColumns {

        public static final String TABLE_NAME = "connections";
        public static final String COLUMN_NAME_HOST = "host";   // The remote host IP address or name
        public static final String COLUMN_NAME_START = "start"; // The start timestamp
        public static final String COLUMN_NAME_STOP = "stop";   // The stop timestamp
        public static final String COLUMN_NAME_INFO = "info";   // Extra information

        static final String SQL_CREATE_TABLE =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        _ID + " INTEGER PRIMARY KEY," +
                        COLUMN_NAME_HOST + " TEXT," +
                        COLUMN_NAME_START + " INTEGER," +
                        COLUMN_NAME_STOP + " INTEGER, " +
                        COLUMN_NAME_INFO + " TEXT)";

        static final String SQL_DELETE_TABLE =
                "DROP TABLE IF EXISTS " + TABLE_NAME;
    }
}
