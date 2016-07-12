package wseemann.media.romote.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.squareup.picasso.Picasso;

import java.util.List;
import java.util.Random;

import wseemann.media.romote.model.Channel;
import wseemann.media.romote.model.Device;
import wseemann.media.romote.parser.ActiveAppParser;
import wseemann.media.romote.utils.CommandHelper;
import wseemann.media.romote.utils.Constants;
import wseemann.media.romote.utils.NotificationUtils;
import wseemann.media.romote.utils.PreferenceUtils;

import com.squareup.picasso.Target;

/**
 * Created by wseemann on 6/19/16.
 */
public class NotificationService extends Service {

    public static final String TAG = NotificationService.class.getName();

    public static final int NOTIFICATION = 100;

    private NotificationManager mNM;
    private Notification notification;

    private Channel mChannel;
    private Device mDevice;

    private SharedPreferences mPreferences;

    private int mServiceStartId;
    private boolean mServiceInUse = true;

    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();
    // Random number generator
    private final Random mGenerator = new Random();

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public NotificationService getService() {
            // Return this instance of LocalService so clients can call public methods
            return NotificationService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.UPDATE_DEVICE_BROADCAST);
        registerReceiver(mUpdateReceiver, intentFilter);

        mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        //startForeground(NotificationService.NOTIFICATION, notification);

        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean enableNotification = mPreferences.getBoolean("notification_checkbox_preference", false);

        mPreferences.registerOnSharedPreferenceChangeListener(mPreferencesChangedListener);

        try {
            mDevice = PreferenceUtils.getConnectedDevice(this);

            if (enableNotification && mDevice != null) {
                notification = NotificationUtils.buildNotification(NotificationService.this, null, null, null);

                mNM.notify(NOTIFICATION, notification);
                sendStatusCommand("");
            }
        } catch (Exception ex) {
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Received start id " + startId + ": " + intent);

        mServiceStartId = startId;

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        unregisterReceiver(mUpdateReceiver);

        // Cancel the persistent notification.
        mNM.cancel(NOTIFICATION);

        mPreferences.unregisterOnSharedPreferenceChangeListener(mPreferencesChangedListener);
    }

    @Override
    public IBinder onBind(Intent intent) {
        mServiceInUse = true;
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        mServiceInUse = true;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mServiceInUse = false;
        return true;
    }

    private void sendStatusCommand(String command) {
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = CommandHelper.getActiveAppURL(this);

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        ActiveAppParser parser = new ActiveAppParser();

                        List<Channel> channels = parser.parse(response);

                        if (channels.size() > 0) {
                            mChannel = channels.get(0);
                            Picasso.with(getApplicationContext()).load(CommandHelper.getIconURL(NotificationService.this, channels.get(0).getId())).into(target);
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "That didn't work!");
            }
        });
        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    private Target target = new Target() {
        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            notification = NotificationUtils.buildNotification(NotificationService.this, mDevice.getModelName(), mChannel.getTitle(), bitmap);
            mNM.notify(NOTIFICATION, notification);
        }

        @Override
        public void onBitmapFailed(Drawable errorDrawable) {
        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {
        }
    };

    private BroadcastReceiver mUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mPreferences = PreferenceManager.getDefaultSharedPreferences(NotificationService.this);
            boolean enableNotification = mPreferences.getBoolean("notification_checkbox_preference", false);

            if (enableNotification) {
                notification = NotificationUtils.buildNotification(NotificationService.this, null, null, null);

                if (enableNotification && mDevice != null) {
                    mNM.notify(NOTIFICATION, notification);
                    sendStatusCommand("");
                } else {
                    mNM.cancel(NOTIFICATION);
                }
            }
        }
    };

    private SharedPreferences.OnSharedPreferenceChangeListener mPreferencesChangedListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            if (key.equals("notification_checkbox_preference")) {
                mPreferences = PreferenceManager.getDefaultSharedPreferences(NotificationService.this);
                boolean enableNotification = mPreferences.getBoolean("notification_checkbox_preference", false);

                mPreferences.registerOnSharedPreferenceChangeListener(mPreferencesChangedListener);

                if (notification == null) {
                    notification = NotificationUtils.buildNotification(NotificationService.this, null, null, null);
                }

                if (enableNotification && mDevice != null) {
                    mNM.notify(NOTIFICATION, notification);
                    sendStatusCommand("");
                } else {
                    mNM.cancel(NOTIFICATION);
                }
            }
        }
    };
}