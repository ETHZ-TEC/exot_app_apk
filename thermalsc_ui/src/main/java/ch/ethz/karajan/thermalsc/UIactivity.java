package ch.ethz.karajan.thermalsc;


import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.ActivityManager;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.TextViewCompat;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import ch.ethz.karajan.lib.Intents;
import ch.ethz.karajan.lib.Messages;


/**
 * Class for main activity.
 */
public class UIactivity extends Activity {
    private static final String TAG = "KARAJAN";

    private String mDeviceId;

    /* UI elements */
    private Button createButton;
    private Button initButton;
    private Button startButton;
    private Button stopButton;
    private Button resetButton;
    private Button destroyButton;
    private Button queryButton;
    private Button forceButton;

    private Spinner mSpinner;
    private Switch mSwitch;

    private boolean mUsingAdvancedButtons = false;

    /* The path where log files are saved. */
    private File mDataPath;

    /* Filter and receiver for broadcast intents back from the service. */
    private IntentFilter mIntentFilter;
    private BroadcastReceiver mBroadcastReceiver;
    private MeterService.ManagerObjectStatus mStatus = null;

    private NotificationManager mNotificationManager;

    /**
     * Enables UI buttons based on Manager object status and
     * whether the 'advanced' options were enabled
     *
     * @param      status  The status
     */
    private void enableButtons(MeterService.ManagerObjectStatus status) {
        boolean[] enablers;

        switch (status) {
            case MISSING:
                enablers = mUsingAdvancedButtons ?
                        new boolean[]{true,  false, false, false, false, false,  true,  true} :
                        new boolean[]{false, false,  true, false, false, false,  true,  true};
                break;
            case CREATED:
                enablers = mUsingAdvancedButtons ?
                        new boolean[]{false,  true, false, false,  true,  true, false, false} :
                        new boolean[]{false, false, false, false, false,  true, false, false};
                break;
            case INITIALISED:
                enablers = mUsingAdvancedButtons ?
                        new boolean[]{false, false,  true, false,  true,  true, false, false} :
                        new boolean[]{false, false, false, false, false,  true, false, false};
                break;
            case RUNNING:
                enablers = mUsingAdvancedButtons ?
                        new boolean[]{false, false, false,  true,  true,  true, false, false} :
                        new boolean[]{false, false, false,  true, false, false, false, false};
                break;
            default:
                enablers = mUsingAdvancedButtons ?
                        new boolean[]{false, false, false, false, false, false, false, false} :
                        new boolean[]{false, false, false, false, false, false, false, false};
                break;
        }

        createButton.setEnabled(enablers[0]);
        initButton.setEnabled(enablers[1]);
        startButton.setEnabled(enablers[2]);
        stopButton.setEnabled(enablers[3]);
        resetButton.setEnabled(enablers[4]);
        destroyButton.setEnabled(enablers[5]);
        mSpinner.setEnabled(enablers[6]);
        mSwitch.setClickable(enablers[7]); /* since actions may vary depending on 'mode',
                                            * the mode selector should not be clickable
                                            * when the service is running/configured.
                                            */
    }

    /**
     * Constructs the activity
     *
     * @param      savedInstanceState  The saved instance state
     */
    @SuppressLint("HardwareIds")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Activity thisActivity = this;

        if (mNotificationManager == null) {
            mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        }

        createButton = findViewById(R.id.createButton);
        initButton = findViewById(R.id.initButton);
        startButton = findViewById(R.id.startButton);
        stopButton = findViewById(R.id.stopButton);
        resetButton = findViewById(R.id.resetButton);
        destroyButton = findViewById(R.id.destroyButton);
        queryButton = findViewById(R.id.queryButton);
        forceButton = findViewById(R.id.forceButton);

        /* declared final to use in the overriden member function below */
        final LinearLayout linearLayout = (LinearLayout) findViewById(R.id.ll);
        linearLayout.setVisibility(View.VISIBLE);

        /* Handle switch changes, enables 'advanced' buttons. */
        mSwitch = findViewById(R.id.advancedOptionsSwitch);
        mSwitch.setVisibility(View.INVISIBLE);

        {
            mDeviceId = Settings.Secure.getString(
                    getApplicationContext().getContentResolver(),
                    Settings.Secure.ANDROID_ID);

            if (mDeviceId == null) {
                mDeviceId = UUID.randomUUID().toString();
            }

            TextView tv = findViewById(R.id.tvId);
            tv.setText(mDeviceId);
        }

        /* Handler for monitoring the chosen save folder unsable space, invoked every 10 seconds. */
        final Handler sizeMonitorHandler = new Handler();
        final Runnable sizeMonitor = new Runnable() {
            @Override
            public void run() {
                TextView tv = findViewById(R.id.tvSpaceAvailable);
                if (mDataPath != null) {
                    tv.setText(humanizeBytes(mDataPath.getUsableSpace(), false));
                }

                sizeMonitorHandler.postDelayed(this, 10000);
            }
        };

        /* Intent filter for the broadcast receiver, used to receive data back from the service. */
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(Messages.STATUS);
        mIntentFilter.addAction(Messages.KILLED);

        /* Broadcast receiver for data back from the service. */
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle bundle = intent.getExtras();

                Log.d(TAG + "/Rx", "received a broadcast intent: " + intent.getAction());
                if (bundle != null) {
                    for (String key : bundle.keySet()) {
                        Object value = bundle.get(key);
                        try {
                            Log.d(TAG + "/Rx",
                                    String.format("got extra: k:[%s] v:[%s] (%s)",
                                            key, value, value.getClass().getName()));
                        } catch (NullPointerException e) {
                            Log.d(TAG + "/Rx", "caught an exception: " + e.toString());
                        }
                    }

                    if (intent.getAction().equals(Messages.STATUS)) {
                        mStatus = (MeterService.ManagerObjectStatus)
                            bundle.getSerializable(Intents.STATUS);
                    } else if (intent.getAction().equals(Messages.KILLED)) {
                        Log.i(TAG + "/Rx", "service killed!");
                        mStatus = MeterService.ManagerObjectStatus.MISSING;
                    }

                    enableButtons(mStatus);

                    if (isMeterServiceRunning()) {
                        forceButton.setEnabled(true);
                    }

                    TextView statusView = findViewById(R.id.tvManagerStatus);
                    statusView.setText(String.format("%s", mStatus));
                }
            }
        };

        registerReceiver(mBroadcastReceiver, mIntentFilter);

        TextView descriptionTextView = findViewById(R.id.tvDescription);
        descriptionTextView.setMovementMethod(new ScrollingMovementMethod());
        descriptionTextView.setText(Html.fromHtml(getString(R.string.description)));

        /* Array for holding save directories. */
        List<File> fileArrayList = new ArrayList<File>();

        /* Get the app directories on both internal and external storage. Since
         * getExternalFilesDirs needs an argument, we get the documents directory, and then
         * simply take the parent /files directory. */
        for (File file : this.getExternalFilesDirs(Environment.DIRECTORY_DOCUMENTS)) {
            try {
                if (file != null && file.exists()) {
                    if (file.canRead() && file.canWrite()) {
                        fileArrayList.add(file);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception thrown: " + e.toString());
            }
        }

        /* The ArrayAdapter is used to present the directories in a Spinner. View methods are
         * overriden to abbreviate the input and display the available space. */
        ArrayAdapter<File> fileArrayAdapter = new ArrayAdapter<File>(this,
                android.R.layout.simple_spinner_dropdown_item, fileArrayList) {
            @Override
            public View getDropDownView(int position,
                                        @Nullable View convertView,
                                        @NonNull ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView textView = (TextView) view;
                TextViewCompat.setTextAppearance(textView,
                        android.R.style.TextAppearance_Material_Small);

                File file = new File(textView.getText().toString());
                String[] splitPath = file.toString().split("/");
                String shortPath = String.format("/%s/%s/", splitPath[1], splitPath[2]);

                String opt = file.toString().contains(getPackageName())
                        ? "Private" : "Public";

                textView.setText(String.format("%s folder on %s (%s free)\n", opt,
                        shortPath, humanizeBytes(file.getUsableSpace(), false)));
                return textView;
            }

            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view;
                TextViewCompat.setTextAppearance(textView,
                        android.R.style.TextAppearance_Material_Small);

                File file = new File(textView.getText().toString());
                String[] splitPath = file.toString().split("/");
                String shortPath = String.format("/%s/%s/", splitPath[1], splitPath[2]);

                String opt = file.toString().contains(getPackageName())
                        ? "Private" : "Public";

                textView.setText(String.format("%s folder on %s\n", opt,
                        shortPath));
                return textView;
            }
        };

        fileArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        /* Configure the drop down selection button. Data path is overriden in the
         * callback methods. */
        mSpinner = findViewById(R.id.spinner);
        mSpinner.setAdapter(fileArrayAdapter);
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mDataPath = (File) parent.getItemAtPosition(position);
                TextView tvPath = (TextView) findViewById(R.id.tvPathDisplay);
                tvPath.setText(mDataPath.toString());
                tvPath.setSelected(true);

                sizeMonitorHandler.removeCallbacks(sizeMonitor);
                sizeMonitorHandler.post(sizeMonitor);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                if (parent.getCount() > 0) {
                    mDataPath = (File) parent.getItemAtPosition(0);
                    TextView tvPath = (TextView) findViewById(R.id.tvPathDisplay);
                    tvPath.setText(mDataPath.toString());
                    tvPath.setSelected(true);

                } else {
                    mDataPath = null;
                    Log.e(TAG, "onNothingSelected(): getCount() <= 0");
                }
            }
        });

        /* Handler for monitoring Android service status. */
        final Handler serviceRunningHandler = new Handler();
        final Runnable serviceRunningRunnable = new Runnable() {
            @Override
            public void run() {
                TextView statusTextView = findViewById(R.id.tvServiceStatus);

                if (isMeterServiceRunning()) {
                    statusTextView.setText(getString(R.string.service_running));
                    forceButton.setEnabled(true);
                } else {
                    statusTextView.setText(getString(R.string.service_not_running));
                    enableButtons(MeterService.ManagerObjectStatus.MISSING);
                    forceButton.setEnabled(false);
                }

                serviceRunningHandler.postDelayed(this, 5000);
            }
        };

        Intent intent = getIntent();
        if (intent.getBooleanExtra(Intents.REOPENED, false)) {
            Log.i(TAG, "got reopened intent");
            Query(null);
        }

        /* Start monitoring service status. */
        serviceRunningHandler.post(serviceRunningRunnable);

        /* Once the mDataPath is set, spawn the directory size monitor. */
        sizeMonitorHandler.post(sizeMonitor);

        Log.i(TAG, "activity created");
    }

    /**
     * Destroys the Activity
     */
    @Override
    protected void onDestroy() {
        Log.i(TAG, "destroying activity");
        super.onDestroy();

        unregisterReceiver(mBroadcastReceiver);
    }

    private final Handler mServiceDestroyHandler = new Handler();

    /**
     * Handles action for 'Force stop' button
     */
    public void Force(View v) {
        Log.i(TAG, "Force() called, stopping the service...");
        stopService(new Intent(UIactivity.this, MeterService.class));

        mServiceDestroyHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(!isMeterServiceRunning()) {
                    Log.i(TAG, "Force() stopped the service successfully!");
                    mNotificationManager.cancel(MeterService.mNotificationId);
                } else {
                    Log.i(TAG, "Force() did not manage to stop the service!");
                }
            }
        }, 500);
    }

    /**
     * Handles action for 'Stop' button
     */
    public void Stop(View v) {
        Log.i(TAG, "Stop called: advanced: " + mUsingAdvancedButtons);

        Intent intent = new Intent(UIactivity.this, MeterService.class);
        if (mUsingAdvancedButtons) {
            intent.setAction(MeterService.ACTION_STOP);
        } else {
            intent.setAction(MeterService.ACTION_NORMAL_STOP);
        }

        startService(intent);
    }

    /**
     * Handles action for 'Init' button
     */
    public void Init(View v) {
        Log.i(TAG, "Init called");
        Intent intent = new Intent(UIactivity.this, MeterService.class);
        intent.setAction(MeterService.ACTION_INIT);

        startService(intent);
    }

    /**
     * Handles action for 'Create' button
     */
    public void Create(View v) {
        Log.i(TAG, "Create called");
        Intent intent = new Intent(UIactivity.this, MeterService.class);
        intent.setAction(MeterService.ACTION_CREATE);

        /* Add data path and device ID to the intent. */
        intent.putExtra(Intents.DATA_PATH, mDataPath.toString());
        intent.putExtra(Intents.UUID, mDeviceId);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    /**
     * Handles action for 'Start' button
     */
    public void Start(View v) {
        Log.i(TAG, "Start called: advanced: " + mUsingAdvancedButtons);
        Intent intent = new Intent(UIactivity.this, MeterService.class);

        if (mUsingAdvancedButtons) {
            intent.setAction(MeterService.ACTION_START);
        } else {
            intent.setAction(MeterService.ACTION_NORMAL_START);
            /* Add data path and device ID to the intent. */
            assert mDataPath != null;
            intent.putExtra(Intents.DATA_PATH, mDataPath.toString());
            assert mDeviceId != null;
            intent.putExtra(Intents.UUID, mDeviceId);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    /**
     * Handles action for 'Reset' button
     */
    public void Reset(View v) {
        Log.i(TAG, "Reset called");
        Intent intent = new Intent(UIactivity.this, MeterService.class);
        intent.setAction(MeterService.ACTION_RESET);
        /* Add data path and device ID to the intent. */
        intent.putExtra(Intents.DATA_PATH, mDataPath);
        intent.putExtra(Intents.UUID, mDeviceId);

        startService(intent);
    }

    /**
     * Handles action for 'Query' button
     */
    public void Query(View v) {
        Log.i(TAG, "Query called");
        Intent intent = new Intent(UIactivity.this, MeterService.class);
        intent.setAction(MeterService.ACTION_QUERY);

        startService(intent);
    }

    /**
     * Handles action for 'Destroy' button
     */
    public void Destroy(View v) {
        Log.i(TAG, "Destroy called");
        Intent intent = new Intent(UIactivity.this, MeterService.class);
        intent.setAction(MeterService.ACTION_DESTROY);

        startService(intent);
    }

    /**
     * Checks if the meter service is running, not to be confused with the native meter object.
     * @return True if running, false otherwise.
     */
    private boolean isMeterServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        assert (manager != null);

        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (MeterService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Converts a value in bytes into a human-readable string, either with SI prefixes
     * (powers of 1000), or powers of 1024 (e.g. kibibytes).
     *
     * @param bytes The value in bytes
     * @param si A flag to indicate if SI unit prefixes are to be used
     * @return A string with a human-readable representation of the size
     */
    private static String humanizeBytes(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format(Locale.ENGLISH,
                "%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }
}
