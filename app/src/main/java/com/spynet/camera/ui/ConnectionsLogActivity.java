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

package com.spynet.camera.ui;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.spynet.camera.R;
import com.spynet.camera.db.ConnectionsContract;
import com.spynet.camera.db.ConnectionsDbHelper;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * The {@link Activity} that shows the logged connections.
 */
public class ConnectionsLogActivity extends AppCompatActivity {

    private final String TAG = getClass().getSimpleName();

    private SQLiteDatabase mDb;     // The connection log database
    private Cursor mCursor;         // The Cursor to browse the log database

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connections_log);

        // Open the database
        mDb = new ConnectionsDbHelper(this).getReadableDatabase();

        // Create the Cursor
        String[] projection = {
                ConnectionsContract.ConnectionsTable._ID,
                ConnectionsContract.ConnectionsTable.COLUMN_NAME_HOST_NAME,
                ConnectionsContract.ConnectionsTable.COLUMN_NAME_HOST_IP,
                ConnectionsContract.ConnectionsTable.COLUMN_NAME_USERAGENT,
                ConnectionsContract.ConnectionsTable.COLUMN_NAME_INFO,
                ConnectionsContract.ConnectionsTable.COLUMN_NAME_START,
                ConnectionsContract.ConnectionsTable.COLUMN_NAME_STOP
        };
        String sortOrder =
                ConnectionsContract.ConnectionsTable.COLUMN_NAME_START + " DESC";
        mCursor = mDb.query(
                ConnectionsContract.ConnectionsTable.TABLE_NAME,
                projection,
                null,
                null,
                null,
                null,
                sortOrder
        );

        // Set the Cursor as the ListView adapter
        ListView listview = (ListView) findViewById(R.id.list);
        listview.setAdapter(new LogCursorAdapter(this, mCursor));
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // TODO: open details Activity
                Log.d(TAG, "onItemClick: " + id);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCursor.close();
        mDb.close();
    }

    /**
     * Defines the {@link CursorAdapter} to be used with the database.
     */
    private class LogCursorAdapter extends CursorAdapter {

        private LogCursorAdapter(Context context, Cursor c) {
            super(context, c, false);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return LayoutInflater.from(context).inflate(R.layout.item_connection, parent, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            // Find fields to populate in inflated template
            TextView host = (TextView) view.findViewById(R.id.host);
            TextView userAgent = (TextView) view.findViewById(R.id.user_agent);
            TextView info = (TextView) view.findViewById(R.id.info);
            TextView start = (TextView) view.findViewById(R.id.start);
            TextView stop = (TextView) view.findViewById(R.id.stop);
            // Extract properties from cursor
            String hostValue = cursor.getString(cursor.getColumnIndexOrThrow(
                    ConnectionsContract.ConnectionsTable.COLUMN_NAME_HOST_NAME));
            String ipValue = cursor.getString(cursor.getColumnIndexOrThrow(
                    ConnectionsContract.ConnectionsTable.COLUMN_NAME_HOST_IP));
            String userAgentValue = cursor.getString(cursor.getColumnIndexOrThrow(
                    ConnectionsContract.ConnectionsTable.COLUMN_NAME_USERAGENT));
            String infoValue = cursor.getString(cursor.getColumnIndexOrThrow(
                    ConnectionsContract.ConnectionsTable.COLUMN_NAME_INFO));
            long startValue = cursor.getLong(cursor.getColumnIndexOrThrow(
                    ConnectionsContract.ConnectionsTable.COLUMN_NAME_START));
            long stopValue = cursor.getLong(cursor.getColumnIndexOrThrow(
                    ConnectionsContract.ConnectionsTable.COLUMN_NAME_STOP));
            // Populate fields with extracted properties
            host.setText(hostValue.equals(ipValue) ? hostValue : hostValue + " (" + ipValue + ")");
            userAgent.setText(userAgentValue);
            info.setText(infoValue);
            start.setText(SimpleDateFormat.getDateTimeInstance().format(new Date(startValue)));
            stop.setText(stopValue == 0 ? "-" :
                    SimpleDateFormat.getDateTimeInstance().format(new Date(stopValue)));
        }
    }
}
