package wseemann.media.romote.service;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.util.Log;

import com.jaku.core.JakuRequest;
import com.jaku.core.KeypressKeyValues;
import com.jaku.request.KeypressRequest;

import wseemann.media.romote.tasks.RequestCallback;
import wseemann.media.romote.tasks.RequestTask;
import wseemann.media.romote.utils.CommandHelper;
import wseemann.media.romote.utils.RokuRequestTypes;

/**
 * Created by wseemann on 6/19/16.
 */
public class CommandService extends IntentService {

    private static final String TAG = CommandService.class.getName();

    public CommandService() {
        super(CommandService.class.getName());
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (hasActiveNetwork()) {
            Log.d(TAG, "onHandleIntent called");

            if (intent != null) {
                //if (intent.getAction() != null) {
                    //Log.d(TAG, "onHandleIntent: " + intent.getAction());
                    performKeypress((KeypressKeyValues) intent.getSerializableExtra("keypress"));
                //}
            }
        }
        else {
            NetworkStateReceiver networkStateReceiver = new NetworkStateReceiver();
            ConnectivityManager connectivityManager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            networkStateReceiver.enable(getApplicationContext());
            networkStateReceiver.setService(this);
            connectivityManager.registerDefaultNetworkCallback(networkStateReceiver);
        }
    }

    private void performKeypress(KeypressKeyValues keypressKeyValue) {
        String url = CommandHelper.getDeviceURL(CommandService.this);

        KeypressRequest keypressRequest = new KeypressRequest(url, keypressKeyValue.getValue());
        JakuRequest request = new JakuRequest(keypressRequest, null);

        new RequestTask(request, new RequestCallback() {
            @Override
            public void requestResult(RokuRequestTypes rokuRequestType, RequestTask.Result result) {

            }

            @Override
            public void onErrorResponse(RequestTask.Result result) {

            }
        }).execute(RokuRequestTypes.keypress);
    }

    //The method hasActiveNetwork() checks whether the network connection is active
    protected boolean hasActiveNetwork() {
        final ConnectivityManager connManager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        Network activeNetwork = connManager.getActiveNetwork();
        return (activeNetwork != null);
    }

    public class NetworkStateReceiver extends ConnectivityManager.NetworkCallback {
        private CommandService service;

        public void setService(CommandService newService) {
            service = newService;
        }

        @Override
        public void onAvailable(Network network) {

            // If there is an active network connection, this method will "turn off" this class and arrange to process the request
            if (service.hasActiveNetwork()) {
                Context context = getApplicationContext();
                disable(context);
                final AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                final Intent innerIntent = new Intent(context, CommandService.class);
                final PendingIntent pendingIntent = PendingIntent.getService(context, 0, innerIntent, 0);

                SharedPreferences preferences = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
                preferences.edit();
                boolean autoRefreshEnabled = preferences.getBoolean("pref_auto_refresh_enabled", false);

                final String hours = preferences.getString("pref_auto_refresh_enabled", "0");
                long hoursLong = Long.parseLong(hours) * 60 * 60 * 1000;

                if (autoRefreshEnabled && hoursLong != 0) {
                    final long alarmTime = preferences.getLong("last_auto_refresh_time", 0) + hoursLong;
                    alarmManager.set(AlarmManager.RTC, alarmTime, pendingIntent);
                } else {
                    alarmManager.cancel(pendingIntent);
                }
            }
        }

        // Method to  "turn on" this class
        public void enable(Context context) {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            connectivityManager.registerDefaultNetworkCallback(this);
        }

        // Method to  "turn off" this class
        public void disable(Context context) {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            connectivityManager.unregisterNetworkCallback(this);
        }

    }
}
