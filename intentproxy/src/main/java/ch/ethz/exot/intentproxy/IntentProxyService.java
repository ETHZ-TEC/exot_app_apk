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
package ch.ethz.exot.intentproxy;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import ch.ethz.exot.intents.*;

public class IntentProxyService extends Service {
    private static final String TAG = "ExOT/IntentProxy";

    /**
     * Constructor
     */
    public IntentProxyService() {
        super();
    }

    /**
     * Processes service bind events. At the moment binding is not supported,
     * therefore onBind returns null.
     *
     * @param intent The intent
     * @return null
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Creates the service
     */
    @Override
    public void onCreate() {
        Log.i(TAG, "IntentProxyService created!");
    }

    /**
     * Destroys the service
     */
    @Override
    public void onDestroy() {
        Log.i(TAG, "IntentProxyService destroyed!");
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
        /* Check if the intent is present */
        if (intent != null) {
            /* Get the action and make sure it is not null */
            final String action = intent.getAction();
            if (action != null) {
                Log.i(TAG, "Perform " + action + " action.");
                Bundle extras = intent.getExtras();
                switch (action) {
                case IntentProxy.Action.FORWARD_BUNDLE:
                case IntentProxy.Action.BUNDLE_EXTRAS:
                    sendBroadcast(forward(extras, true));
                    break;
                case IntentProxy.Action.START_APPS:
                    for (Intent app_intent : generateAppIntents(extras, ExOTApps.Actions.START)) {
                        sendBroadcast(app_intent);
                    }
                    break;
                case IntentProxy.Action.STOP_APPS:
                    for (Intent app_intent : generateAppIntents(extras, ExOTApps.Actions.STOP)) {
                        stopService(app_intent);
                    }
                    break;
                case IntentProxy.Action.FORWARD_STARTSERVICE:
                    startService(forward(extras, false));
                    break;
                case IntentProxy.Action.FORWARD_STARTACTIVITY:
                    startActivity(forward(extras, false));
                    break;
                case IntentProxy.Action.CONFIGURE_APP:
                    startService(forward(extras, true));
                    break;
                case IntentProxy.Action.FORWARD:
                case IntentProxy.Action.FORWARD_:
                    sendBroadcast(forward(extras, false));
                    break;
                default:
                    Log.i(TAG + "/Tx", "Action " + action + " unkonwn, do nothing.");
                }
            } else {
                Log.i(TAG + "/Tx", "No action specified, do nothing.");
            }
        }
        return START_NOT_STICKY;
    }

    private Intent forward(Bundle extra_bundle, boolean bundle_extras) {
        Intent repackaged_forward = new Intent();
        String key_extra_bundle = IntentProxy.KeysExtras.DEFAULT_BUNDLE;

        if (extra_bundle != null) {
            if (extra_bundle.containsKey(IntentProxy.KeysExtras.INTENT_ACTION)) {
                Log.i(TAG + "/Tx", "Received key " + IntentProxy.KeysExtras.INTENT_ACTION + " with value "
                        + extra_bundle.getString(IntentProxy.KeysExtras.INTENT_ACTION));
                repackaged_forward.setAction(extra_bundle.getString(IntentProxy.KeysExtras.INTENT_ACTION));
                extra_bundle.remove(IntentProxy.KeysExtras.INTENT_ACTION);
            }

            if (extra_bundle.containsKey(IntentProxy.KeysExtras.INTENT_COMPONENT)) {
                Log.i(TAG + "/Tx", "Received key " + IntentProxy.KeysExtras.INTENT_COMPONENT + " with value "
                        + extra_bundle.getString(IntentProxy.KeysExtras.INTENT_COMPONENT));
                String[] component_name = extra_bundle.getString(IntentProxy.KeysExtras.INTENT_COMPONENT).split("/");
                assert component_name.length == 2;
                repackaged_forward.setComponent(new ComponentName(component_name[0], component_name[1]));
                extra_bundle.remove(IntentProxy.KeysExtras.INTENT_COMPONENT);
            }

            if (extra_bundle.containsKey(IntentProxy.KeysExtras.INTENT_FLAGS)) {
                Log.i(TAG + "/Tx", "Received key " + IntentProxy.KeysExtras.INTENT_FLAGS + " with value "
                        + extra_bundle.getInt(IntentProxy.KeysExtras.INTENT_FLAGS));
                repackaged_forward.setFlags(extra_bundle.getInt(IntentProxy.KeysExtras.INTENT_FLAGS));
                extra_bundle.remove(IntentProxy.KeysExtras.INTENT_FLAGS);
            }

            if (extra_bundle.containsKey(IntentProxy.KeysExtras.INTENT_EXTRAS_KEY)) {
                Log.i(TAG + "/Tx", "Received key " + IntentProxy.KeysExtras.INTENT_EXTRAS_KEY + " with value "
                        + extra_bundle.getString(IntentProxy.KeysExtras.INTENT_EXTRAS_KEY));
                key_extra_bundle = extra_bundle.getString(IntentProxy.KeysExtras.INTENT_EXTRAS_KEY);
                extra_bundle.remove(IntentProxy.KeysExtras.INTENT_EXTRAS_KEY);
            }

            for (String key : extra_bundle.keySet()) {
                Object value = extra_bundle.get(key);
                if (value != null) {
                    Log.d(TAG + "/Tx", String.format("Forwarding value pair: k:[%s] v:[%s] (%s)", key, value,
                            value.getClass().getName()));
                } else {
                    Log.e(TAG + "/Tx", String.format("Forwarding value pair: k:[%s] v:[null]!", key));
                }
            }
            if (bundle_extras) {
                repackaged_forward.putExtra(key_extra_bundle, extra_bundle);
            } else {
                repackaged_forward.putExtras(extra_bundle);
            }
        } else {
            Log.d(TAG + "/Tx", "Repackage Bundle: No extras given, nothing to repackage and forward");
        }

        Log.d(TAG + "/Tx", "Repackaged intent " + (bundle_extras ? "and bundled extras" : "") + ": "
                + repackaged_forward.toString());

        return repackaged_forward;
    }

    private List<Intent> generateAppIntents(Bundle extras, String action) {
        List<Intent> app_intents = new ArrayList<Intent>();

        if (extras.containsKey(IntentProxy.KeysExtras.APPS_ARRAY)) {
            for (String app : extras.getStringArrayList(IntentProxy.KeysExtras.APPS_ARRAY)) {
                Intent tmp = new Intent();
                String[] component_name = app.split("/");
                tmp.setComponent(new ComponentName(component_name[0], component_name[1]));
                tmp.setAction(action);
                // TODO add intent other extras if necessary
                app_intents.add(tmp);
            }
        }
        return app_intents;
    }
}
