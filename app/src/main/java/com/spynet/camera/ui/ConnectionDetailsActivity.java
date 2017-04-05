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

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.spynet.camera.R;
import com.spynet.camera.db.ConnectionsContract;
import com.spynet.camera.db.ConnectionsDbHelper;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * The {@link Activity} that shows a logged connection details.
 */
public class ConnectionDetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection_details);

        // Read the connection ID
        Intent intent = getIntent();
        long id = intent.getLongExtra("ID", -1);

        if (id != -1) {

            // Open the database
            SQLiteDatabase db = new ConnectionsDbHelper(this).getReadableDatabase();

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
            String selection = ConnectionsContract.ConnectionsTable._ID + " = ?";
            String[] selArgs = new String[]{String.valueOf(id)};
            Cursor cursor = db.query(
                    ConnectionsContract.ConnectionsTable.TABLE_NAME,
                    projection,
                    selection,
                    selArgs,
                    null,
                    null,
                    null
            );

            if (cursor.moveToNext()) {

                // Find fields to populate in inflated template
                TextView host = (TextView) findViewById(R.id.host);
                TextView ip = (TextView) findViewById(R.id.ip);
                TextView userAgent = (TextView) findViewById(R.id.user_agent);
                TextView info = (TextView) findViewById(R.id.info);
                TextView start = (TextView) findViewById(R.id.start);
                TextView stop = (TextView) findViewById(R.id.stop);

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
                host.setText(hostValue);
                ip.setText(ipValue);
                userAgent.setText(userAgentValue);
                info.setText(infoValue);
                start.setText(SimpleDateFormat.getDateTimeInstance().format(new Date(startValue)));
                stop.setText(stopValue == 0 ? "-" :
                        SimpleDateFormat.getDateTimeInstance().format(new Date(stopValue)));
            }

            // Close the database
            cursor.close();
            db.close();
        }
    }
}
