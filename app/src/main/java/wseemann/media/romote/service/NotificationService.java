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
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import com.jaku.core.JakuRequest;
import com.jaku.parser.AppsParser;
import com.jaku.parser.IconParser;
import com.jaku.request.QueryActiveAppRequest;
import com.jaku.request.QueryIconRequest;

import java.util.List;
import java.util.Random;

import com.jaku.model.Channel;
import com.jaku.model.Device;

import wseemann.media.romote.tasks.RequestCallback;
import wseemann.media.romote.tasks.RequestTask;
import wseemann.media.romote.utils.CommandHelper;
import wseemann.media.romote.utils.Constants;
import wseemann.media.romote.utils.NotificationUtils;
import wseemann.media.romote.utils.PreferenceUtils;
import wseemann.media.romote.utils.RokuRequestTypes;

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

    private MediaSessionCompat mediaSession;

    private int mServiceStartId;
    private boolean mServiceInUse = true;

    private final Random mGenerator = new Random();

    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();

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
                sendStatusCommand();
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

    private void sendStatusCommand() {
        String url = CommandHelper.getDeviceURL(this);

        QueryActiveAppRequest queryActiveAppRequest = new QueryActiveAppRequest(url);
        JakuRequest request = new JakuRequest(queryActiveAppRequest, new AppsParser());

        new RequestTask(request, new RequestCallback() {
            @Override
            public void requestResult(RokuRequestTypes rokuRequestType, RequestTask.Result result) {
                List<Channel> channels = (List<Channel>) result.mResultValue;

                if (channels.size() > 0) {
                    mChannel = channels.get(0);
                    getAppIcon(mChannel.getId());
                }
            }

            @Override
            public void onErrorResponse(RequestTask.Result result) {
                Log.d(TAG, "That didn't work!");
            }
        }).execute(RokuRequestTypes.query_active_app);
    }

    private void getAppIcon(String appId) {
        String url = CommandHelper.getDeviceURL(this);

        QueryIconRequest queryIconRequest = new QueryIconRequest(url, appId);
        JakuRequest request = new JakuRequest(queryIconRequest, new IconParser());

        new RequestTask(request, new RequestCallback() {
            @Override
            public void requestResult(RokuRequestTypes rokuRequestType, RequestTask.Result result) {
                try {
                    byte [] data = (byte []) result.mResultValue;

                    Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);

                    mDevice = PreferenceUtils.getConnectedDevice(NotificationService.this);
                    notification = NotificationUtils.buildNotification(NotificationService.this, mDevice.getModelName(), mChannel.getTitle(), bitmap);
                    mNM.notify(NOTIFICATION, notification);
                } catch (Exception ex) {
                }
            }

            @Override
            public void onErrorResponse(RequestTask.Result result) {
                Log.d(TAG, "That didn't work!");
            }
        }).execute(RokuRequestTypes.query_icon);
    }

    private BroadcastReceiver mUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mPreferences = PreferenceManager.getDefaultSharedPreferences(NotificationService.this);
            boolean enableNotification = mPreferences.getBoolean("notification_checkbox_preference", false);

            if (enableNotification) {
                notification = NotificationUtils.buildNotification(NotificationService.this, null, null, null);

                if (enableNotification && mDevice != null) {
                    mNM.notify(NOTIFICATION, notification);
                    sendStatusCommand();
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
                    sendStatusCommand();
                } else {
                    mNM.cancel(NOTIFICATION);
                }
            }
        }
    };

    private void setUpMediaSession() {
        mediaSession = new MediaSessionCompat(this, TAG, null, null);

        try {
            //mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
            mediaSession.setActive(true);
        } catch (NullPointerException ex) {
        }
    }

    private void updateMediaSessionMetadata() {
        MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder();
        builder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "artist");
        builder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, "album");
        builder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, "Track name");
        builder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, 100);

        //getArtwork();

        mediaSession.setMetadata(builder.build());
    }

    private void getArtwork(String url) {
        /*synchronized(this) {
            // Instantiate the RequestQueue.
            RequestQueue queue = Volley.newRequestQueue(this);

            // Retrieves an image specified by the URL, displays it in the UI.
            ImageRequest request = new ImageRequest(url,
                    new Response.Listener<Bitmap>() {
                        @Override
                        public void onResponse(Bitmap bitmap) {
                            MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder();
                            builder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "artist");
                            builder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, "album");
                            builder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, "Track name");
                            builder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, 100);
                            builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap);
                            mediaSession.setMetadata(builder.build());

                            Log.d(TAG, "Loaded Bitmap for mediaSession");
                        }
                    }, 0, 0, null,
                    new Response.ErrorListener() {
                        public void onErrorResponse(VolleyError error) {
                            Log.e(TAG, "Error loading Bitmap for mediaSession metadata");
                        }
                    });

            queue.add(request);
        }*/
    }
}