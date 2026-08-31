package wseemann.media.romote.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.wseemann.ecp.model.Channel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import wseemann.media.romote.R
import wseemann.media.romote.data.Device
import wseemann.media.romote.device.DeviceManager
import wseemann.media.romote.di.IoDispatcher
import wseemann.media.romote.di.MainDispatcher
import wseemann.media.romote.preferences.AppPreferences
import wseemann.media.romote.utils.Constants
import wseemann.media.romote.utils.NotificationUtils
import javax.inject.Inject
import wseemann.media.romote.device.Device as ConnectedDevice

/**
 * Keeps the playback notification in step with whatever the connected Roku is showing.
 *
 * Every ECP call the wrapper library exposes is blocking OkHttp with a 6s connect and 6s read
 * timeout, and this is an ordinary [Service], so it runs on the app's main thread. The refresh is
 * therefore driven from [serviceScope]: the device read, both queries and the icon decode happen on
 * [ioDispatcher], and only the notification and media-session updates come back to the main thread.
 */
@AndroidEntryPoint
class NotificationService : Service() {

    @Inject
    @MainDispatcher
    lateinit var mainDispatcher: CoroutineDispatcher

    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    @Inject
    lateinit var appPreferences: AppPreferences

    @Inject
    lateinit var deviceManager: DeviceManager

    private lateinit var notificationManager: NotificationManager
    private lateinit var mediaSession: MediaSession

    /**
     * Dispatched on the main thread, so the continuation after each `withContext(ioDispatcher)`
     * resumes there and the notification work needs no further hop.
     */
    private val serviceScope by lazy { CoroutineScope(SupervisorJob() + mainDispatcher) }

    private var statusJob: Job? = null

    private val binder: IBinder = LocalBinder()

    /**
     * Class used for the client Binder. Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    inner class LocalBinder : Binder() {
        val service: NotificationService
            get() = this@NotificationService
    }

    override fun onCreate() {
        super.onCreate()

        setUpMediaSession()

        registerReceiver(
            updateReceiver,
            IntentFilter(Constants.UPDATE_DEVICE_BROADCAST),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            getString(R.string.app_name),
            getString(R.string.app_name),
            NotificationManager.IMPORTANCE_LOW,
        )
        channel.description = TAG
        channel.enableLights(false)
        channel.enableVibration(false)
        notificationManager.createNotificationChannel(channel)

        sharedPreferences.registerOnSharedPreferenceChangeListener(preferencesChangedListener)

        if (appPreferences.isNotificationWidgetEnabled()) {
            refreshStatus()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.tag(TAG).i("Received start id %d: %s", startId, intent)

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        serviceScope.cancel()

        unregisterReceiver(updateReceiver)

        // Cancel the persistent notification.
        notificationManager.cancel(NOTIFICATION)

        sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferencesChangedListener)
        mediaSession.release()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    /**
     * Re-reads what the device is playing and rebuilds the notification around it.
     *
     * Only one refresh is ever in flight: [Constants.UPDATE_DEVICE_BROADCAST] is sent on every
     * device-changing key press, so without the cancel below, holding a button down would queue one
     * pair of six-second requests per press.
     */
    private fun refreshStatus() {
        statusJob?.cancel()
        statusJob = serviceScope.launch {
            val device = withContext(ioDispatcher) { connectedDevice() }

            if (device == null) {
                notificationManager.cancel(NOTIFICATION)
                return@launch
            }

            // Posted before the queries so the notification is up while the Roku is being asked
            // what it is playing, rather than only once it answers.
            if (!showNotification(buildNotification())) {
                return@launch
            }

            val status = withContext(ioDispatcher) { loadStatus(device) } ?: return@launch

            updateMediaSessionMetadata(status.channel, status.icon)
            showNotification(
                buildNotification(
                    title = status.device.modelName,
                    text = status.channel.title,
                    icon = status.icon,
                ),
            )
        }
    }

    /**
     * Runs on [ioDispatcher]: two ECP round trips and the icon decode. Returns null when the device
     * is unreachable or is not reporting an active channel, which leaves the placeholder up.
     */
    private fun loadStatus(device: ConnectedDevice): Status? {
        val channel = device.performQueryActiveApp()?.firstOrNull() ?: return null
        val appId = channel.id ?: return null
        val bytes = device.performQueryIcon(appId) ?: return null

        return try {
            val icon = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
            Status(device.getDeviceInfo(), channel, icon)
        } catch (ex: IllegalArgumentException) {
            Timber.tag(TAG).e(ex, "Failed to decode the channel icon")
            null
        }
    }

    /** Reads the paired device. SharedPreferences plus a SQLite query, so callers stay on IO. */
    @Suppress("TooGenericExceptionCaught")
    private fun connectedDevice(): ConnectedDevice? = try {
        deviceManager.getConnectedDevice()
    } catch (ex: Exception) {
        Timber.tag(TAG).e(ex, "Failed to read the connected device")
        null
    }

    private fun buildNotification(title: String? = null, text: String? = null, icon: Bitmap? = null): Notification =
        NotificationUtils.buildNotification(this, title, text, icon, mediaSession.sessionToken)

    /**
     * Posts the playback notification, unless the user has notifications turned off.
     *
     * NotificationManager drops these silently when POST_NOTIFICATIONS is missing (API 33+) or the
     * channel has been blocked, so asking first keeps the service from polling the Roku for
     * artwork nobody is going to see.
     *
     * @return true when the notification actually went out.
     */
    private fun showNotification(notification: Notification): Boolean {
        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            Timber.tag(TAG).d("Notifications are disabled; not posting the playback notification")
            return false
        }

        notificationManager.notify(NOTIFICATION, notification)
        return true
    }

    private val updateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (appPreferences.isNotificationWidgetEnabled()) {
                refreshStatus()
            }
        }
    }

    private val preferencesChangedListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == AppPreferences.APP_PREFERENCE_NOTIFICATION_WIDGET) {
                if (appPreferences.isNotificationWidgetEnabled()) {
                    refreshStatus()
                } else {
                    notificationManager.cancel(NOTIFICATION)
                }
            }
        }

    private fun setUpMediaSession() {
        mediaSession = MediaSession(this, TAG)
        mediaSession.setActive(true)
        mediaSession.setFlags(
            MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or
                MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS,
        )
        mediaSession.setPlaybackState(
            PlaybackState.Builder()
                .setState(PlaybackState.STATE_PLAYING, 0L, 0F)
                .setActions(
                    PlaybackState.ACTION_PAUSE or
                        PlaybackState.ACTION_PLAY or
                        PlaybackState.ACTION_REWIND or
                        PlaybackState.ACTION_FAST_FORWARD,
                )
                .build(),
        )
        mediaSession.setMetadata(MediaMetadata.Builder().build())
    }

    private fun updateMediaSessionMetadata(channel: Channel, bitmap: Bitmap) {
        val builder = MediaMetadata.Builder()
        builder.putString(MediaMetadata.METADATA_KEY_TITLE, channel.title)
        builder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, bitmap)

        mediaSession.setMetadata(builder.build())
    }

    /** What a single refresh read off the device, assembled on [ioDispatcher]. */
    private data class Status(val device: Device, val channel: Channel, val icon: Bitmap)

    companion object {
        val TAG: String = NotificationService::class.java.name
        const val NOTIFICATION = 100
    }
}
