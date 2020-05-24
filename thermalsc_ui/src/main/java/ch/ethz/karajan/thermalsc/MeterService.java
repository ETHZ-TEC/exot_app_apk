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
package ch.ethz.karajan.thermalsc;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;

import ch.ethz.karajan.lib.BaseMeterService;
import ch.ethz.karajan.lib.Intents;

public class MeterService extends BaseMeterService {

    static {
        System.loadLibrary("jni_thermal-sc");
    }

    /**
     * Creates a notification when the service is running
     */
    protected void createNotification() {

        /* Create a pending intent to open the main activity */
        Intent mainActivityIntent = new Intent(this, UIactivity.class);
        mainActivityIntent.putExtra(Intents.REOPENED, true);
        PendingIntent mainActivityPendingIntent =
                PendingIntent.getActivity(this, 0,
                        mainActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        /* Create a pending intent to stop the service */
        Intent thisIntent = new Intent(this, this.getClass());
        switch (mMode) {
            case NORMAL:
                thisIntent.setAction(ACTION_NORMAL_STOP);
                break;
            case ADVANCED:
                thisIntent.setAction(ACTION_STOP);
                break;
        }

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
                .setContentTitle(getString(ch.ethz.karajan.lib.R.string.notification_title))
                .setContentText(getString(ch.ethz.karajan.lib.R.string.notification_text))
                .setSubText(null)
                .addAction(mainActivityAction)
                .addAction(thisAction)
                .setAutoCancel(false)
                .setOngoing(true)
                .setSmallIcon(ch.ethz.karajan.lib.R.mipmap.ic_launcher_round);

        /* Starting from Android Oreo it is necessary to create a notification channel */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            String description = getString(ch.ethz.karajan.lib.R.string.notification_channel);
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
     * Updates the notification with running time of the service
     */
    protected void updateNotification() {
        if (isManagerObjectStarted()) {
            CharSequence str = getString(ch.ethz.karajan.lib.R.string.notification_subtext, managerObjectRunningTime());

            mNotificationBuilder.setContentText(str);
            mNotificationManager.notify(mNotificationId, mNotificationBuilder.build());
        }
    }

    /**
     * Destroys the notification
     */
    protected void destroyNotification() {
        mNotificationBuilder.setContentText(getString(ch.ethz.karajan.lib.R.string.notification_cancelled));
        mNotificationManager.notify(mNotificationId, mNotificationBuilder.build());

        /* Stop the updater runnable and post a defer notification cancellation */
        mHandler.removeCallbacks(updateRunnable);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mNotificationManager.cancel(mNotificationId);
            }
        }, 1000);
    }
}
