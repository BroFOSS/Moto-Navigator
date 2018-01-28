package work.technie.motonavigator.utils;
/*
 * Copyright (C) 2017 Anupam Das
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import work.technie.motonavigator.data.MotorContract;


/**
 * Created by anupam on 19/12/15.
 */
public class Utility {

    private static final String WIFI = "WIFI";
    private static final String MOBILE = "MOBILE";

    private static final String[] WAYPOINTS_COLUMNS = {

            MotorContract.Waypoints.TABLE_NAME + "." + MotorContract.Waypoints._ID,
            MotorContract.Waypoints.START_NAME,
            MotorContract.Waypoints.START_LAT,
            MotorContract.Waypoints.START_LONG,
            MotorContract.Waypoints.DEST_NAME,
            MotorContract.Waypoints.DEST_LAT,
            MotorContract.Waypoints.DEST_LONG,
            MotorContract.Waypoints.MODE,
            MotorContract.Waypoints.ROUTE_ID,
            MotorContract.Waypoints.ROUTE_DURATION,
            MotorContract.Waypoints.ROUTE_DISTANCE
    };

    private static final String[] STEPS_COLUMNS = {

            MotorContract.Steps.TABLE_NAME + "." + MotorContract.Waypoints._ID,
            MotorContract.Steps.ROUTE_ID,
            MotorContract.Steps.BEARING_BEFORE,
            MotorContract.Steps.BEARING_AFTER,
            MotorContract.Steps.LOCATION_LAT,
            MotorContract.Steps.LOCATION_LONG,
            MotorContract.Steps.TYPE,
            MotorContract.Steps.INSTRUCTION,
            MotorContract.Steps.MODE,
            MotorContract.Steps.DURATION,
            MotorContract.Steps.NAME,
            MotorContract.Steps.DISTANCE
    };

    public static boolean hasNetworkConnection(Context context) {
        boolean hasConnectedWifi = false;
        boolean hasConnectedMobile = false;

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] netInfo = cm.getAllNetworkInfo();
        for (NetworkInfo ni : netInfo) {
            if (ni.getTypeName().equalsIgnoreCase(WIFI))
                if (ni.isConnected())
                    hasConnectedWifi = true;
            if (ni.getTypeName().equalsIgnoreCase(MOBILE))
                if (ni.isConnected())
                    hasConnectedMobile = true;
        }
        return hasConnectedWifi || hasConnectedMobile;
    }

    public static void backUpData(Context mContext,String uid) {
        Uri waypointUri = MotorContract.Waypoints.buildWaypointUri();
        Cursor waypointCursor = mContext.getContentResolver().query(waypointUri, WAYPOINTS_COLUMNS, null, null, null);

        try {
            putToFirebase(waypointCursor,uid,"WAYPOINTS");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Uri stepsUri = MotorContract.Steps.buildStepUri();
        Cursor stepsCursor = mContext.getContentResolver().query(stepsUri, STEPS_COLUMNS, null, null, null);

        try {
            putToFirebase(stepsCursor,uid,"STEPS");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private static void putToFirebase(Cursor cursor,String uid,String tableName) throws JSONException {

        int columnCount = cursor.getColumnCount();
        cursor.moveToFirst();
        do {
            JSONObject row = new JSONObject();
            for (int index = 1; index < columnCount; index++) {
                row.put(cursor.getColumnName(index), cursor.getString(index));
            }
            new OkHttpHandler().execute(row.toString(),uid,tableName,cursor.getString(0));
        } while (cursor.moveToNext());

        cursor.close();
    }

    private static class OkHttpHandler extends AsyncTask<String, Void, byte[]> {

        private final OkHttpClient client = new OkHttpClient();
        private final MediaType JSON
                = MediaType.parse("application/json; charset=utf-8");

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected byte[] doInBackground(String... params) {

            RequestBody body = RequestBody.create(JSON, params[0]);
            Request request = new Request.Builder()
                    .url("https://moto-navigator-74a33.firebaseio.com/" + params[1] + "/" + params[2]+ "/" + params[3] + ".json")
                    .put(body)
                    .build();
            try {
                Response response = client.newCall(request).execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(byte[] bytes) {
            super.onPostExecute(bytes);
        }
    }
}
