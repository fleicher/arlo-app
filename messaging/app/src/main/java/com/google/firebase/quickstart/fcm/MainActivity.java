/**
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.firebase.quickstart.fcm;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.NetworkOnMainThreadException;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    HashMap<String, String> sentValues = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        System.out.println("starting activity");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create channel to show notifications.
            String channelId  = getString(R.string.default_notification_channel_id);
            String channelName = getString(R.string.default_notification_channel_name);
            NotificationManager notificationManager =
                    getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(new NotificationChannel(channelId,
                    channelName, NotificationManager.IMPORTANCE_LOW));
        }

        // If a notification message is tapped, any data accompanying the notification
        // message is available in the intent extras. In this sample the launcher
        // intent is fired when the notification is tapped, so any accompanying data would
        // be handled here. If you want a different intent fired, set the click_action
        // field of the notification message to the desired intent. The launcher intent
        // is used when no click_action is specified.
        //
        // Handle possible data accompanying notification message.
        // [START handle_data_extras]
        /*
        if (getIntent().getExtras() != null) {
            System.out.println("Extras not null");
            for (String key : getIntent().getExtras().keySet()) {
                String value = (String) getIntent().getExtras().get(key);
                Log.d(TAG, "Key: " + key + " Value: " + value);
                sentValues.put(key, value);
            }
        }
        else{
            System.out.println("Extras null");
        }
        // [END handle_data_extras]
        sentValues.put("image", "http://res.cloudinary.com/arlo/image/upload/v1521292110/last.png");
        */

        final SharedPreferences soundPrefs = getSharedPreferences("sound_prefs", 0);
        Spinner soundDropdownFront = findViewById(R.id.spinnerFront);
        Spinner soundDropdownRest = findViewById(R.id.spinnerRest);
        String[] items = new String[]{"song", "knock", "vibrate", "silent", "ignore"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items);
        soundDropdownFront.setAdapter(adapter);
        soundDropdownRest.setAdapter(adapter);
        soundDropdownFront.setSelection(getIndex(soundDropdownFront, soundPrefs.getString("rest", "knock")));
        soundDropdownFront.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                SharedPreferences.Editor soundPrefsEditor = soundPrefs.edit();
                soundPrefsEditor.putString("front", (String) parent.getItemAtPosition(position));
                soundPrefsEditor.apply();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        soundDropdownRest.setSelection(getIndex(soundDropdownRest, soundPrefs.getString("rest", "knock")));
        soundDropdownRest.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                SharedPreferences.Editor soundPrefsEditor = soundPrefs.edit();
                soundPrefsEditor.putString("rest", (String) parent.getItemAtPosition(position));
                soundPrefsEditor.apply();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });


        final SharedPreferences sharedPrefs = getSharedPreferences("my_prefs", 0);
        final Button videoURLButton = findViewById(R.id.videoButton);

        //videoURLButton.setText("Platzhalter");
        final TextView tw = findViewById(R.id.informationTextView);

        if (sharedPrefs.getLong("stamp", 0L) + 1000000L > System.currentTimeMillis()) {
            new DownloadInfo().execute();

            String info_text = sharedPrefs.getString("name", "No Camera Name") + " " +
                    sharedPrefs.getString("date", "No Recording Date") + " @ " +
                    sharedPrefs.getString("time", "-");
            tw.setText(info_text);
            System.out.println("j" + sharedPrefs.getString("name", "nothing"));


            //Button videoURLButton = findViewById(R.id.videoButton);
            videoURLButton.setText("Download Video");
            videoURLButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String url = sharedPrefs.getString("url", "https://google.com");
                    Uri uri = Uri.parse(url); // missing 'http://' will cause crashed
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(intent);

                    // Get token
                    /*
                    String token = FirebaseInstanceId.getInstance().getToken();

                    // Log and toast
                    String msg = "02 " + getString(R.string.msg_token_fmt, token);
                    Log.d(TAG, msg);
                    Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                    */
                }
            });

        }else{
            //Button videoURLButton = findViewById(R.id.videoButton);
            //videoURLButton.setText("Video Expired");
        }

        final Button subscribeButton = findViewById(R.id.subscribeButton);
        subscribeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // [START subscribe_topics]
                FirebaseMessaging.getInstance().subscribeToTopic("news");
                // [END subscribe_topics]

                // Log and toast
                String msg = getString(R.string.msg_subscribed);
                Log.d(TAG, msg);
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });

        Button falseAlarmButton = findViewById(R.id.deleteButton);
        falseAlarmButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
            sharedPrefs.edit().clear().apply();
            videoURLButton.setOnClickListener(null);
            videoURLButton.setText("No Video");
            tw.setText("marked as false Alarm");
            }
        });
    }
    private int getIndex(Spinner spinner, String myString)
    {
        int index = 0;

        for (int i=0;i<spinner.getCount();i++){
            if (spinner.getItemAtPosition(i).toString().equalsIgnoreCase(myString)){
                index = i;
                break;
            }
        }
        return index;
    }

    private class DownloadInfo extends AsyncTask<Void, Void, Bitmap>{

        @Override
        protected Bitmap doInBackground(Void... params){
            SharedPreferences sharedPrefs = getSharedPreferences("my_prefs", 0);
            System.out.println("called in Bg");
            if (sharedPrefs.getLong("stamp", 0L)
                    + 1000000L>System.currentTimeMillis() ) {
                try {
                    System.out.println("Starting image download " +
                            sharedPrefs.getString("image", null));
                    URL u = new URL(sharedPrefs.getString("image", null));
                    InputStream inp = u.openStream();
                    Bitmap bitmap = BitmapFactory.decodeStream(inp);
                    System.out.println("Stopping image download");
                    return bitmap;
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (NetworkOnMainThreadException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    Writer writer = new StringWriter();
                    e.printStackTrace(new PrintWriter(writer));
                    String s = writer.toString();
                    e.printStackTrace();
                    System.out.println("Error: " + s);
                }
            }
            else{
                System.out.println("last occurence too old");
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap result){
            ImageView i = findViewById(R.id.camera);
            i.setImageBitmap(result);
        }

    }
}

