package wseemann.media.romote.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import timber.log.Timber
import wseemann.media.romote.audio.IRemoteAudioInterface

/**
 * The binding to the private listening companion app (`wseemann.media.romote.audio`), which streams
 * the device's audio to the phone over an AIDL interface.
 *
 * This used to live in RemoteFragment, which never unbound - the binding leaked for the life of the
 * process. It is now a plain object the remote tab holds across recompositions and releases in a
 * DisposableEffect.
 *
 * [onStateChanged] reports whether audio is playing; it is what picks the button's icon.
 */
class RemoteAudioConnection(private val context: Context, private val onStateChanged: (Boolean) -> Unit) {

    private var service: IRemoteAudioInterface? = null

    private var isBound = false

    /** The device the service should stream from, remembered until the binding comes up. */
    private var pendingHost: String? = null

    /**
     * Starts or stops private listening. The first call binds the service, which toggles audio on
     * as soon as it connects; every later call toggles the service that is already bound.
     */
    fun toggle(deviceHost: String?) {
        val service = service

        if (service == null) {
            pendingHost = deviceHost
            bind()
            return
        }

        try {
            service.toggleRemoteAudio()
            reportState()
        } catch (ex: RemoteException) {
            Timber.tag(TAG).e(ex, "Failed to toggle private listening")
        }
    }

    fun release() {
        if (isBound) {
            try {
                context.unbindService(connection)
            } catch (ex: IllegalArgumentException) {
                Timber.tag(TAG).e(ex, "Failed to unbind private listening")
            }

            isBound = false
        }

        service = null
    }

    private fun bind() {
        val intent = Intent().apply { component = REMOTE_AUDIO_COMPONENT }

        try {
            // Bound even when bindService reports failure: the connection still has to be released.
            isBound = true
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        } catch (ex: SecurityException) {
            isBound = false
            Timber.tag(TAG).e(ex, "Failed to start private listening service")
        }
    }

    /** Tells the caller what the service is doing, which is what picks the button's icon. */
    private fun reportState() {
        val isActive = try {
            service?.isRemoteAudioActive == true
        } catch (ex: RemoteException) {
            Timber.tag(TAG).e(ex, "Failed to read the private listening state")
            false
        }

        onStateChanged(isActive)
    }

    private val connection = object : ServiceConnection {

        override fun onServiceConnected(componentName: ComponentName?, binder: IBinder?) {
            Timber.tag(TAG).d("onServiceConnected")
            service = IRemoteAudioInterface.Stub.asInterface(binder)

            try {
                service?.setDevice(pendingHost)
                service?.toggleRemoteAudio()
            } catch (ex: Exception) {
                Timber.tag(TAG).e(ex, "Failed to start private listening")
            }

            reportState()
        }

        override fun onServiceDisconnected(componentName: ComponentName?) {
            Timber.tag(TAG).d("onServiceDisconnected")
            service = null
            reportState()
        }

        override fun onBindingDied(name: ComponentName?) {
            Timber.tag(TAG).d("onBindingDied")
            service = null
            reportState()
        }

        override fun onNullBinding(name: ComponentName?) {
            Timber.tag(TAG).d("onNullBinding")
            service = null
            reportState()
        }
    }

    private companion object {
        const val TAG = "RemoteAudioConnection"

        val REMOTE_AUDIO_COMPONENT = ComponentName(
            "wseemann.media.romote.audio",
            "wseemann.media.romote.audio.remoteaudio.RemoteAudio",
        )
    }
}
