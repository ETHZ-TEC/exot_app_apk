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
package ch.ethz.exot.lib;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.json.JSONObject;
import org.json.JSONException;

import ch.ethz.exot.intents.ExOTApps.*;

public abstract class BaseService extends Service {
    public static final String TAG = "ExOT/BaseService";

    /**
     * Variables for managing notifications
     */
    protected NotificationManager mNotificationManager;
    protected Notification.Builder mNotificationBuilder;
    public static CharSequence mChannelName = "ch.ethz.exot.lib.channel.SERVICE_CHANNEL";
    public static String mChannelTag = "Service status notification";
    public static int mNotificationId = 50390774;

    /**
     * Handler for updating the notification during service runtime
     */
    protected Handler mHandler = new Handler();

    /**
     * Constructor
     */
    public BaseService() {
        super();
    }

    /**
     * Processes service bind events. At the moment binding is not supported,
     * therefore onBind returns null.
     *
     * @param intent The intent`
     * @return null
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Processes intents sent via startService().
     *
     * @param intent  The supplied intent
     * @param flags   Additional data about the request
     * @param startId A unique integer for specific requests to start
     * @return Value indicating how the system should handle the service
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String jsonConfig = null;
        JSONObject jsonConfigObject = null;

        /* Check if the intent is present */
        if (intent != null) {
            /* Get the action and make sure it is not null */
            final String action = intent.getAction();
            Log.i(TAG, "onStartCommand(): " + action);

            if (action == null)
                return START_NOT_STICKY;

            /* Parse and log all provided extras */
            Bundle extras = intent.getExtras();

            if (extras != null) {
                for (String key : extras.keySet()) {
                    Object value = extras.get(key);
                    if (value != null) {
                        Log.d(TAG, String.format("onStartCommand(): k:[%s] v:[%s] (%s)", key, value,
                                value.getClass().getName()));
                    } else {
                        Log.e(TAG, String.format("onStartCommand(): k:[%s] v:[null]!", key));
                    }
                }

                if (action == Actions.CREATE
                        || action == Actions.RESET) {

                    jsonConfig = extras.getString(Keys.CONFIG);

                    if (jsonConfig == null) {
                        Log.w(TAG, "jsonConfig is null...");
                    } else {
                        try {
                            jsonConfigObject = new JSONObject(jsonConfig);
                        } catch (JSONException e) {
                            Log.e(TAG, String.format("onStartCommand(): invalid JSON: %s", jsonConfig));
                            return START_NOT_STICKY;
                        }
                    }
                }
            } else {
                Log.e(TAG, "onStartCommand(): extras == null");
            }

            handleActions(action, jsonConfigObject);
            broadcastStatus();

            IntentFilter filter = new IntentFilter();
            filter.addAction(Actions.START);
            filter.addAction(Actions.STOP);
            filter.addAction(Actions.START);
            filter.addAction(Actions.STOP);
            filter.addAction(Actions.QUERY);
            registerReceiver(receiver, filter);
        } else {
            Log.e(TAG, "onStartCommand(): intent == null");
        }

        return START_NOT_STICKY;
    }

    public void handleActions(String action, JSONObject jsonConfigObject) {
        // TODO: Clean up unnecessary intents when ready...

        Log.i(TAG, "handleActions: action " + action);
        switch (action) {
        case Actions.CREATE:
            handleActionCreate(jsonConfigObject.toString());
            break;
        case Actions.START:
            handleActionStart();
            break;
        case Actions.STOP:
            handleActionStop();
            break;
        case Actions.RESET:
            handleActionReset(jsonConfigObject.toString());
            break;
        case Actions.DESTROY:
            handleActionDestroy();
            break;
        case Actions.QUERY:
            handleActionQuery();
            broadcastStatus();
            break;
        default:
            Log.e(TAG, "handleActions(): unknown action!");
            break;
        }
    }

    protected final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            handleActions(action, null);
        }
    };

    /**
     * Creates the service
     */
    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate()");

        if (mNotificationManager == null) {
            mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        }

        // android.os.Debug.waitForDebugger();

        Toast.makeText(this, TAG + " created!", Toast.LENGTH_SHORT).show();
    }

    private void broadcastStatus() {
        Intent intent = new Intent(Broadcasts.STATUS);
        intent.putExtra(Keys.STATUS, getObjectStatus());
        sendBroadcast(intent);
    }

    /**
     * Destroys the service
     */
    @Override
    public void onDestroy() {
        boolean isStarted = isManagerObjectStarted();

        Log.i(TAG, "onDestroy(): isStarted: " + isStarted);

        if (!isStarted) {
            Intent intent = new Intent(Broadcasts.KILLED);
            intent.putExtra(Keys.STATUS, getObjectStatus());
            sendBroadcast(intent);

            Toast.makeText(this, TAG + " destroyed!", Toast.LENGTH_SHORT).show();
        }

        unregisterReceiver(receiver);
    }

    protected void handleActionCreate(String config) {
        boolean ret = createManagerObject(config);

        if (ret) {
            createNotification();
            Toast.makeText(this, TAG + " started!", Toast.LENGTH_SHORT).show();
        }

        Log.i(TAG, "handleActionCreate(): " + ret);
    }

    protected void handleActionStop() {
        boolean ret = stopManagerObject();

        if (ret) {
            destroyNotification();
        }

        ret &= destroyManagerObject();

        Log.i(TAG, "handleActionStop(): " + ret);
        Toast.makeText(this, TAG + " stopped!", Toast.LENGTH_SHORT).show();
    }

    protected void handleActionStart() {
        boolean ret = startManagerObject();

        if (ret) {
            createNotification();
        }

        Log.i(TAG, "handleActionStart(): " + ret);
    }

    protected void handleActionReset(String config) {
        boolean started = isManagerObjectStarted();
        boolean ret = resetManagerObject(config);

        if (ret && started) {
            destroyNotification();
        }

        Log.i(TAG, "handleActionReset(): " + ret);
    }

    protected void handleActionDestroy() {
        boolean started = isManagerObjectStarted();
        boolean ret = destroyManagerObject();

        if (ret && started) {
            destroyNotification();
        }

        Log.i(TAG, "handleActionDestroy(): " + ret);
    }

    protected void handleActionQuery() {
        String query = queryManagerObjectStatus();

        Log.i(TAG, "handleActionQuery(): " + query);
    }

    protected void notifyAboutException() {
        Intent intent = new Intent(Broadcasts.EXCEPTION);
        sendBroadcast(intent);
    }

    public Status getObjectStatus() {
        Log.d(TAG, "getObjectStatus(): entered");

        Status managerObjectStatus;

        if (managerObjectExists()) {
            managerObjectStatus = Status.CREATED;
        } else {
            managerObjectStatus = Status.MISSING;
            return managerObjectStatus;
        }

        if (isManagerObjectInitialised()) {
            managerObjectStatus = Status.INITIALISED;

            if (isManagerObjectStarted()) {
                managerObjectStatus = Status.RUNNING;
            }
        }

        Log.d(TAG, String.format("getObjectStatus(): status: [%s]", managerObjectStatus));

        return managerObjectStatus;
    }

    protected void createNotification() {
        //
    }

    protected void destroyNotification() {
        //
    }

    protected void updateNotification() {
        //
    }

    /**
     * Starts the Manager
     *
     * @return True if started successfully, false if operation was invalid or
     *         unsuccessful
     */
    public native boolean startManagerObject();

    /**
     * Stops the Manager
     *
     * @return True if stopped successfully, false if operation was invalid or
     *         unsuccessful
     */
    public native boolean stopManagerObject();

    /**
     * Creates the Manager
     *
     * @return True if created successfully, false if operation was invalid or
     *         unsuccessful
     */
    public native boolean createManagerObject(String config);

    /**
     * Resets the Manager
     *
     * @return True if reset successfully, false if operation was invalid or
     *         unsuccessful
     */
    public native boolean resetManagerObject(String config);

    /**
     * Destroys the Manager
     *
     * @return True if destroyed successfully, false if operation was invalid or
     *         unsuccessful
     */
    public native boolean destroyManagerObject();

    /**
     * Queries the Manager started state
     *
     * @return True if the Manager was started, false if not, or when invalid
     */
    protected native boolean isManagerObjectStarted();

    /**
     * Queries the Manager initialisation state
     *
     * @return True if the Manager was initialised, false if not, or when invalid
     */
    protected boolean isManagerObjectInitialised() {
        return managerObjectExists();
    };

    /**
     * Checks if the Manager exists
     *
     * @return True if exists, false otherwise
     */
    protected native boolean managerObjectExists();

    /**
     * Query manager object's status
     *
     * @return
     */
    protected native String queryManagerObjectStatus();

    /**
     * Gets the running time of the Manager
     *
     * @return The formatted running time, 'N/A' if does not exist
     */
    protected native String managerObjectRunningTime();

    /**
     * Method to get the name of the currently running foreground service. Used for
     * callbacks from the native treads.
     */
    public String getTopApp() {
        String topPackageName = "~NOT FOUND~";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            UsageStatsManager mUsageStatsManager = (UsageStatsManager) this
                    .getSystemService(Context.USAGE_STATS_SERVICE);

            final List<UsageStats> queryUsageStats = mUsageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY, System.currentTimeMillis() - 100000, System.currentTimeMillis());
            // Sort the stats by the last time used
            if (queryUsageStats != null) {
                SortedMap<Long, UsageStats> mySortedMap = new TreeMap<Long, UsageStats>();
                for (UsageStats usageStats : queryUsageStats) {
                    mySortedMap.put(usageStats.getLastTimeUsed(), usageStats);
                }
                if (!mySortedMap.isEmpty()) {
                    topPackageName = mySortedMap.get(mySortedMap.lastKey()).getPackageName();
                } else {
                    ActivityManager manager = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
                    List<ActivityManager.RunningAppProcessInfo> tasks = manager.getRunningAppProcesses();
                    topPackageName = tasks.get(0).processName;
                }
            }
        }
        // TODO: else part was missing?
        return topPackageName;
    }
}
