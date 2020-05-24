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
package ch.ethz.karajan.lib;

import android.app.Service;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public abstract class BaseMeterService extends Service {
    public static final String TAG = "KarajanMeter/Service";


    /**
     * Method to get the name of the currently running foreground service. Used for callbacks from
     * Method to get the name of the currently running foreground service. Used for callbacks from
     * the native treads.
     */
    public String getTopApp() {
        String topPackageName = "~~~~~~~NOT FOUND~~~~~~~";
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            UsageStatsManager mUsageStatsManager = (UsageStatsManager) this.getSystemService(Context.USAGE_STATS_SERVICE);

            final List<UsageStats> queryUsageStats = mUsageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, System.currentTimeMillis() - 10000, System.currentTimeMillis());
            // Sort the stats by the last time used
            if (queryUsageStats != null) {
                SortedMap<Long, UsageStats> mySortedMap = new TreeMap<Long, UsageStats>();
                for (UsageStats usageStats : queryUsageStats) {
                    mySortedMap.put(usageStats.getLastTimeUsed(), usageStats);
                }
                if (!mySortedMap.isEmpty()) {
                    topPackageName = mySortedMap.get(mySortedMap.lastKey()).getPackageName();
                }
            }
        }
        return topPackageName;
    }


    /**
     * Possible actions performed by the service.
     */
    public static final String ACTION_START   = "ch.ethz.karajan.lib.meter.action.START";
    public static final String ACTION_STOP    = "ch.ethz.karajan.lib.meter.action.STOP";
    public static final String ACTION_INIT    = "ch.ethz.karajan.lib.meter.action.INIT";
    public static final String ACTION_CREATE  = "ch.ethz.karajan.lib.meter.action.CREATE";
    public static final String ACTION_RESET   = "ch.ethz.karajan.lib.meter.action.RESET";
    public static final String ACTION_DESTROY = "ch.ethz.karajan.lib.meter.action.DESTROY";
    public static final String ACTION_QUERY   = "ch.ethz.karajan.lib.meter.action.QUERY";

    /**
     * Actions performed in the 'easy' mode
     */
    public static final String ACTION_NORMAL_START = "ch.ethz.karajan.lib.meter.action.NORMAL_START";
    public static final String ACTION_NORMAL_STOP  = "ch.ethz.karajan.lib.meter.action.NORMAL_STOP";

    /**
     * Enumeration with the status of the Manager.
     */
    public enum ManagerObjectStatus {
        MISSING,
        CREATED,
        INITIALISED,
        RUNNING
    }

    /**
     * Enumeration for choosing between standard and advanced modes.
     */
    public enum Mode {
        ADVANCED,
        NORMAL
    }

    public Mode mMode = Mode.NORMAL;

    /**
     * Variables for managing notifications
     */
    protected NotificationManager mNotificationManager;
    protected Notification.Builder mNotificationBuilder;
    public static CharSequence mChannelName = "ch.ethz.karajan.lib.channel.METER_CHANNEL";
    public static String mChannelTag = "Service status notification";
    public static int mNotificationId = 40390774;

    /**
     * Handler for updating the notification during service runtime
     */
    protected Handler mHandler = new Handler();

    /**
     * Constructor
     */
    public BaseMeterService() {
        super();
    }

    /**
     * Processes service bind events.
     * At the moment binding is not supported, therefore onBind returns null.
     *
     * @param intent The intent
     * @return null
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Processes intents sent via startService().
     *
     * @param intent The supplied intent
     * @param flags Additional data about the request
     * @param startId A unique integer for specific requests to start
     * @return Value indicating how the system should handle the service
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        /* Check if the intent is present */
        if (intent != null) {
            /* Get the action and make sure it is not null */
            final String action = intent.getAction();
            assert action != null;
            Log.i(TAG, "onHandleIntent(): " + action);

            String path = null;
            String uuid = null;

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

                path = extras.getString(Intents.DATA_PATH);
                uuid = extras.getString(Intents.UUID);

                if (path == null) Log.e(TAG, "path is null");
                if (uuid == null) Log.e(TAG, "uuid is null");
            }

            /* Choose how to handle the action */
            switch (action) {
                case ACTION_NORMAL_START:
                    assert(path != null);
                    assert(uuid != null);
                    mMode = Mode.NORMAL;
                    handleActionNormalStart(path, uuid);
                    break;
                case ACTION_NORMAL_STOP:
                    handleActionNormalStop();
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            stopSelf();
                        }
                    }, 1500);
                    break;
                case ACTION_CREATE:
                    assert(path != null);
                    assert(uuid != null);
                    handleActionCreate(path, uuid);
                    break;
                case ACTION_INIT:
                    handleActionInit();
                    break;
                case ACTION_START:
                    mMode = Mode.ADVANCED;
                    handleActionStart();
                    break;
                case ACTION_STOP:
                    handleActionStop();
                    break;
                case ACTION_QUERY:
                    handleActionQuery();
                    break;
                case ACTION_RESET:
                    handleActionReset();
                    break;
                case ACTION_DESTROY:
                    handleActionDestroy();
                    break;
                default:
                    Log.e(TAG, "onHandleIntent(): unknown intent!");
            } 
        } else {
            Log.d(TAG, "onHandleIntent(): null");
        }

        publishStatus(getObjectStatus());
        return START_NOT_STICKY;
    }

    /**
     * Creates the service
     */
    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate()");
        // super.onCreate();

        if (mNotificationManager == null) {
            mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        }

        Toast.makeText(this, TAG + " created!", Toast.LENGTH_SHORT).show();
    }

    /**
     * Destroys the service
     */
    @Override
    public void onDestroy() {
        boolean isStarted = isManagerObjectStarted();
        // super.onDestroy();

        Log.i(TAG, "onDestroy(): isStarted: " + isStarted);

        if (!isStarted) {
            Intent intent = new Intent(Messages.KILLED);
            intent.putExtra(Intents.STATUS, getObjectStatus());
            sendBroadcast(intent);

            Toast.makeText(this, TAG + " destroyed!", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Handles 'regular' startup
     *
     * @param      path  The path where log files are to be saved
     * @param      uuid  The uuid of the running user
     */
    protected void handleActionNormalStart(String path, String uuid) {
        boolean ret = createManagerObject(path, uuid);
        ret &= initManagerObject();
        ret &= startManagerObject();

        if (ret) {
            createNotification();
            Toast.makeText(this, TAG + " started!", Toast.LENGTH_SHORT).show();
        }

        Log.i(TAG, "handleActionNormalStart(): " + ret);
    }

    /**
     * Handler 'regular' stop, destroys the object
     */
    protected void handleActionNormalStop() {
        boolean ret = stopManagerObject();

        if (ret) {
            destroyNotification();
        }

        ret &= destroyManagerObject();

        Log.i(TAG, "handleActionNormalStop(): " + ret);
        Toast.makeText(this, TAG + " stopped!", Toast.LENGTH_SHORT).show();
    }

    /**
     * Handles ACTION_START
     */
    protected void handleActionStart() {
        boolean ret = startManagerObject();

        if (ret) {
            createNotification();
        }

        Log.i(TAG, "handleActionStart(): " + ret);
    }

    /**
     * Handles ACTION_STOP
     */
    protected void handleActionStop() {
        boolean ret = stopManagerObject();

        if (ret) {
            destroyNotification();
        }

        Log.i(TAG, "handleActionStop(): " + ret);
    }

    /**
     * Handles ACTION_CREATE
     */
    protected void handleActionCreate(String path, String uuid) {
        boolean ret = createManagerObject(path, uuid);
        Log.i(TAG, "handleActionCreate(): " + ret);
    }

    /**
     * Handles ACTION_INIT
     */
    protected void handleActionInit() {
        boolean ret = initManagerObject();
        Log.i(TAG, "handleActionInit(): " + ret);
    }

    /**
     * Handles ACTION_RESET
     */
    protected void handleActionReset() {
        boolean started = isManagerObjectStarted();
        boolean ret = resetManagerObject();

        if (ret && started) {
            destroyNotification();
        }

        Log.i(TAG, "handleActionReset(): " + ret);
    }

    /**
     * Handles ACTION_DESTROY
     */
    protected void handleActionDestroy() {
        boolean started = isManagerObjectStarted();
        boolean ret = destroyManagerObject();

        if (ret && started) {
            destroyNotification();
        }

        Log.i(TAG, "handleActionDestroy(): " + ret);
    }

    /**
     * Handles ACTION_QUERY
     */
    protected void handleActionQuery() {
        boolean objectExists = managerObjectExists();

        boolean isStarted = isManagerObjectStarted();
        boolean isInitialised = isManagerObjectInitialised();

        Log.i(TAG, "handleActionQuery(): objectExists: " + objectExists +
                "; isStarted: " + isStarted + "; isInitialised: " + isInitialised);
    }

    /**
     * Publishes the Manager status via a broadcast intent
     * @param status The Manager status
     */
    protected void publishStatus(ManagerObjectStatus status) {
        Intent intent = new Intent(Messages.STATUS);
        intent.putExtra(Intents.STATUS, getObjectStatus());
        sendBroadcast(intent);
    }

    protected void notifyAboutException() {
        Intent intent = new Intent(Messages.EXCEPTION);
        sendBroadcast(intent);
    }

    /**
     * Gets the Manager status
     * @return
     */
    public ManagerObjectStatus getObjectStatus() {
        ManagerObjectStatus managerObjectStatus;

        if (managerObjectExists()) {
            managerObjectStatus = ManagerObjectStatus.CREATED;
        } else {
            managerObjectStatus = ManagerObjectStatus.MISSING;
            return managerObjectStatus;
        }

        if (isManagerObjectInitialised()) {
            managerObjectStatus = ManagerObjectStatus.INITIALISED;

            if (isManagerObjectStarted()) {
                managerObjectStatus = ManagerObjectStatus.RUNNING;
            }
        }

        return managerObjectStatus;
    }

    abstract protected void createNotification();
    abstract protected void destroyNotification();
    abstract protected void updateNotification();

    /**
     * Starts the Manager
     * @return True if started successfully, false if operation was invalid or unsuccessful
     */
    public native boolean startManagerObject();

    /**
     * Stops the Manager
     * @return True if stopped successfully, false if operation was invalid or unsuccessful
     */
    public native boolean stopManagerObject();

    /**
     * Creates the Manager
     * @return True if created successfully, false if operation was invalid or unsuccessful
     */
    public native boolean createManagerObject(String path, String uuid);

    /**
     * Initialises the Manager
     * @return True if initialised successfully, false if operation was invalid or unsuccessful
     */
    public native boolean initManagerObject();

    /**
     * Resets the Manager
     * @return True if reset successfully, false if operation was invalid or unsuccessful
     */
    public native boolean resetManagerObject();

    /**
     * Destroys the Manager
     * @return True if destroyed successfully, false if operation was invalid or unsuccessful
     */
    public native boolean destroyManagerObject();

    /**
     * Queries the Manager started state
     * @return True if the Manager was started, false if not, or when invalid
     */
    protected native boolean isManagerObjectStarted();

    /**
     * Queries the Manager initialisation state
     * @return True if the Manager was initialised, false if not, or when invalid
     */
    protected native boolean isManagerObjectInitialised();

    /**
     * Checks if the Manager exists
     * @return True if exists, false otherwise
     */
    protected native boolean managerObjectExists();

    /**
     * Gets the running time of the Manager
     * @return The formatted running time, 'N/A' if does not exist
     */
    protected native String managerObjectRunningTime();
}

