// Copyright (c) 2015-2020, Swiss Federal Institute of Technology (ETH Zurich)
// All rights reserved.
// 
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
// 
// * Redistributions of source code must retain the above copyright notice, this
//   list of conditions and the following disclaimer.
// 
// * Redistributions in binary form must reproduce the above copyright notice,
//   this list of conditions and the following disclaimer in the documentation
//   and/or other materials provided with the distribution.
// 
// * Neither the name of the copyright holder nor the names of its
//   contributors may be used to endorse or promote products derived from
//   this software without specific prior written permission.
// 
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
// FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
// DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
// CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
// OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
// 
package ch.ethz.exot.thermalscui;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.widget.Toast;


import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

import ch.ethz.exot.lib.BaseService;
import ch.ethz.exot.intents.ExOTApps.*;

public class MeterService extends BaseService {
    String path = null;
    String uuid = null;
    JSONObject config = null;

    public Modes mMode = Modes.NORMAL;

    static {
        System.loadLibrary("exot-jni-thermalscui");
    }

    /**
     * Processes intents sent via startService().
     *
     * @param intent The supplied intent
     * @param flags Additional data about the request
     * @param startId A unique integer for specific requests to start
     * @return Value indicating how the system should handle the service
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        /* Check if the intent is present */
        if (intent != null) {
            /* Get the action and make sure it is not null */
            final String action = intent.getAction();
            assert action != null;
            Log.i(TAG, "onHandleIntent(): " + action);

            /* Parse and log all provided extras */
            Bundle extras = intent.getExtras();
            if (extras != null) {
                for (String key : extras.keySet()) {
                    Object value = extras.get(key);
                    if (value != null) {
                        Log.d(TAG, String.format("onHandleIntent(): k:[%s] v:[%s] (%s)",
                                key, value, value.getClass().getName()));
                    } else {
                        Log.e(TAG, String.format("onHandleIntent(): k:[%s] v:[null]!", key));
                    }
                }

                String str_config = extras.getString(Keys.CONFIG);

                if (str_config != null) {
                    try {
                        config = new JSONObject(str_config);
                    } catch (JSONException e) {
                        Log.e(TAG, String.format("onHandleIntent(): Unable to create JSON config with error %s!", e));
                    }
                }

                if (config == null) Log.e(TAG, "config is null");
            }

            handleActions(action, config);
        } else {
            Log.d(TAG, "onHandleIntent(): null");
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(ch.ethz.exot.intents.ExOTApps.Actions.START);
        filter.addAction(ch.ethz.exot.intents.ExOTApps.Actions.STOP);
        filter.addAction(ch.ethz.exot.intents.ExOTApps.Actions.STATUS);
        registerReceiver(receiver, filter);

        publishStatus(getObjectStatus());
        return START_NOT_STICKY;
    }

    public void handleActions(String action, JSONObject jsonConfigObject) {

        /* Choose how to handle the action */
        switch (action) {
            case Actions.START:
                assert (path != null);
                assert (uuid != null);
                handleActionCreate(config.toString());
                mMode = Modes.NORMAL;
                handleActionStart();
                break;
            case Actions.STOP:
                handleActionStop();
                mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            stopSelf();
                        }
                    }, 1500);
                break;
            case Actions.QUERY:
                handleActionQuery();
                break;
            case Actions.RESET:
                handleActionReset(config.toString());
                break;
            case Actions.DESTROY:
                handleActionDestroy();
                break;
            case Actions.STATUS: // TODO
                Log.d(TAG, "onHandleIntent(): not handled action STATUS");
                break;
            case Actions.KILLED: // TODO
                Log.d(TAG, "onHandleIntent(): not handled action KILLED");
                break;
            default:
                Log.e(TAG, "onHandleIntent(): unknown intent!");
        }
    }

    /**
     * Creates a notification when the service is running
     */
    protected void createNotification() {

        /* Create a pending intent to open the main activity */
        Intent mainActivityIntent = new Intent(this, UIactivity.class);
        mainActivityIntent.putExtra(Keys.REOPENED, true);
        PendingIntent mainActivityPendingIntent =
                PendingIntent.getActivity(this, 0,
                        mainActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        /* Create a pending intent to stop the service */
        Intent thisIntent = new Intent(this, this.getClass());
        thisIntent.setAction(Actions.STOP);
        thisIntent.putExtra(Keys.MODE, mMode);

        PendingIntent thisPendingIntent =
                PendingIntent.getService(this, 0,
                        thisIntent, PendingIntent.FLAG_ONE_SHOT);

        /* Bind the pending intents to notification actions */
        Notification.Action mainActivityAction =
                new Notification.Action(0, "Open settings", mainActivityPendingIntent);
        Notification.Action thisAction =
                new Notification.Action(0, "Stop service", thisPendingIntent);

        /* Construct the notification */
        mNotificationBuilder = new Notification.Builder(this)
                .setContentTitle(getString(ch.ethz.exot.thermalscui.R.string.notification_title))
                .setContentText(getString(ch.ethz.exot.thermalscui.R.string.notification_text))
                .setSubText(null)
                .addAction(mainActivityAction)
                .addAction(thisAction)
                .setAutoCancel(false)
                .setOngoing(true)
                .setSmallIcon(ch.ethz.exot.thermalscui.R.mipmap.ic_launcher_round);

        /* Starting from Android Oreo it is necessary to create a notification channel */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            //String description = getString(ch.ethz.exot.lib.R.string.notification_channel);
            String description = "Test";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(mChannelTag, mChannelName, importance);
            channel.setDescription(description);
            mNotificationManager.createNotificationChannel(channel);
            mNotificationBuilder.setChannelId(mChannelTag);
        }

        /* Display the notification */
        startForeground(mNotificationId, mNotificationBuilder.build());

        /* Initiate deferred notification update */
        mHandler.postDelayed(updateRunnable, 1000);
    }

    /**
     * Runnable to update the notification
     */
    protected final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            updateNotification();
            mHandler.postDelayed(this, 5000);
        }
    };

        /**
     * Publishes the Manager status via a broadcast intent
     * @param status The Manager status
     */
    protected void publishStatus(Status status) {
        Intent intent = new Intent(Actions.STATUS);
        intent.putExtra(Keys.STATUS, status);
        sendBroadcast(intent);
    }

}
