package wseemann.media.romote.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import wseemann.media.romote.data.Device
import wseemann.media.romote.data.DeviceDiscovery
import wseemann.media.romote.device.DeviceManager
import wseemann.media.romote.device.DeviceRepository
import wseemann.media.romote.di.IoDispatcher
import wseemann.media.romote.event.MainScreenUiEvent
import wseemann.media.romote.model.MainScreenUiState
import wseemann.media.romote.recents.RecentChannelsRepository
import wseemann.media.romote.utils.BroadcastUtils
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

@HiltViewModel
class MainScreenViewModel @Inject constructor(
    @param:ApplicationContext val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val deviceManager: DeviceManager,
    private val deviceRepository: DeviceRepository,
    private val deviceDiscovery: DeviceDiscovery,
    private val recentChannelsRepository: RecentChannelsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainScreenUiState())
    val uiState = _uiState.asStateFlow()

    /**
     * Incremented for every scan. A scan only publishes its results if it is still the newest
     * one, so a slow scan that started before a device was forgotten can't overwrite the fresh
     * result with its own stale view of what was paired.
     */
    private val scanGeneration = AtomicInteger()

    /** The scan started by the most recent [onRefresh]; what [onTabSelected] checks before starting its own. */
    private var scanJob: Job? = null

    init {
        onRefresh()
    }

    fun onHandleEvent(event: MainScreenUiEvent) {
        when (event) {
            is MainScreenUiEvent.RefreshEvent -> onRefresh()
            is MainScreenUiEvent.TabSelectedEvent -> onTabSelected()
            is MainScreenUiEvent.DeviceSelectedEvent -> onDeviceSelected(event.device)
            is MainScreenUiEvent.ForgetDeviceEvent -> onForgetDevice(event.serialNumber)
            is MainScreenUiEvent.RenameDeviceClickedEvent -> onRenameDeviceClicked(event)
            is MainScreenUiEvent.RenameDeviceConfirmedEvent -> onRenameDeviceConfirmed(event.name)
            is MainScreenUiEvent.RenameDeviceDismissedEvent -> onRenameDeviceDismissed()
            // DevicesTab handles these two: starting an activity needs its Context.
            is MainScreenUiEvent.DeviceInfoClickedEvent,
            is MainScreenUiEvent.AddDeviceClickedEvent -> Unit
        }
    }

    /**
     * A scan already in flight is the fresh result this would have asked for, so re-entering the tab while
     * one runs - including the scan [init] starts before the tab is first drawn - is a no-op. The job is
     * checked rather than isLoading, which the scan only sets once it reaches the IO dispatcher.
     */
    private fun onTabSelected() {
        if (scanJob?.isActive == true) {
            return
        }

        onRefresh()
    }

    private fun onRefresh() {
        val generation = scanGeneration.incrementAndGet()

        scanJob = viewModelScope.launch(ioDispatcher) {
            _uiState.update { it.copy(isLoading = true) }

            val pairedDevices = deviceRepository.getAllDevices()

            _uiState.update {
                it.copy(
                    availableDevices = persistentListOf(),
                    pairedDevices = pairedDevices.toPersistentList(),
                    connectedSerialNumber = connectedSerialNumber()
                )
            }

            try {
                val discovered = deviceDiscovery.discoverDevices()

                // Read what is paired *after* the scan: a device forgotten while the scan was
                // running is no longer paired, and has to show up as available again.
                val pairedSerialNumbers = pairedDevices.map { it.serialNumber }.toSet()

                if (generation != scanGeneration.get()) {
                    // Superseded; the newer scan owns the loading state and the results.
                    return@launch
                }

                refreshConnectedDevice(discovered)
                backfillDeviceImages(pairedDevices, discovered)

                _uiState.update {
                    it.copy(
                        availableDevices = discovered
                            .filterNot { device -> device.serialNumber in pairedSerialNumbers }
                            .toPersistentList(),
                        pairedDevices = pairedDevices.toPersistentList(),
                        connectedSerialNumber = connectedSerialNumber(),
                        isLoading = false
                    )
                }
            } catch (ex: CancellationException) {
                throw ex
            } catch (ex: Exception) {
                Timber.tag(TAG).e(ex)

                if (generation != scanGeneration.get()) {
                    return@launch
                }

                _uiState.update {
                    it.copy(availableDevices = persistentListOf(), isLoading = false)
                }
            }
        }
    }

    /**
     * Refreshes the stored record of the connected device, in case its details changed since it was
     * paired. Reuses the scan the caller already ran rather than starting a second one alongside it.
     */
    private fun refreshConnectedDevice(discovered: List<Device>) {
        val connectedSerialNumber = connectedSerialNumber()

        if (connectedSerialNumber == null) {
            Timber.tag(TAG).d("No connected device to update")
            return
        }

        discovered.firstOrNull { it.serialNumber == connectedSerialNumber }?.let { device ->
            deviceRepository.updateDevice(device)
            BroadcastUtils.sendUpdateDeviceBroadcast(context)
        }
    }

    /**
     * refreshConnectedDevice only writes back the connected device, and a device paired before the
     * app knew about device images has no image url stored at all, so without this a paired but
     * unconnected device would keep drawing the placeholder for good.
     */
    private fun backfillDeviceImages(pairedDevices: List<Device>, discovered: List<Device>) {
        val discoveredBySerialNumber = discovered.associateBy { it.serialNumber }

        pairedDevices
            .filter { it.deviceImageUrl.isNullOrEmpty() }
            .forEach { pairedDevice ->
                val imageUrl = discoveredBySerialNumber[pairedDevice.serialNumber]?.deviceImageUrl

                if (!imageUrl.isNullOrEmpty()) {
                    pairedDevice.deviceImageUrl = imageUrl
                    deviceRepository.updateDevice(pairedDevice)
                }
            }
    }

    /** Reads SQLite, so every caller is already on the IO dispatcher. */
    private fun connectedSerialNumber(): String? = try {
        deviceManager.getConnectedDevice()?.getDeviceInfo()?.serialNumber
    } catch (ignored: Exception) {
        // connectedDevice throws rather than returning null when nothing is paired.
        null
    }

    /** The toast and the widget update stay with DevicesTab, which needs its own Context. */
    private fun onDeviceSelected(device: Device) {
        viewModelScope.launch(ioDispatcher) {
            deviceRepository.insertDevice(device)
            deviceManager.setConnectedDevice(device.serialNumber)

            BroadcastUtils.sendUpdateDeviceBroadcast(context)

            // Read outside the update lambda: update re-runs its lambda if another coroutine wins
            // the race to publish, and the database should not be read twice for that.
            val pairedDevices = deviceRepository.getAllDevices()
            val pairedSerialNumbers = pairedDevices.map { it.serialNumber }.toSet()

            // A device that was just paired belongs under "Paired devices" and nowhere else, so it
            // is filtered out of the available list by the same rule onRefresh applies to a scan.
            _uiState.update {
                it.copy(
                    availableDevices = it.availableDevices
                        .filterNot { available -> available.serialNumber in pairedSerialNumbers }
                        .toPersistentList(),
                    pairedDevices = pairedDevices.toPersistentList(),
                    connectedSerialNumber = device.serialNumber
                )
            }
        }
    }

    private fun onForgetDevice(serialNumber: String) {
        viewModelScope.launch(ioDispatcher) {
            // Read while the row still exists: this is the record that moves back to the available
            // list, and removeDevice below leaves nothing to read it from.
            val forgottenDevice = deviceRepository.getDevice(serialNumber)?.apply {
                // The name the user gave it went with the pairing, so the row reverts to the name
                // the device reports for itself.
                setCustomUserDeviceName(null)
            }

            deviceRepository.removeDevice(serialNumber)
            recentChannelsRepository.clearForDevice(serialNumber)

            val connectedSerialNumber = connectedSerialNumber()

            if (connectedSerialNumber == null || connectedSerialNumber == serialNumber) {
                deviceManager.setConnectedDevice(null)

                // Nothing else announces this: refreshConnectedDevice returns early once there is
                // no connected device left to refresh. Without the broadcast the remote and
                // channels tabs keep showing the device that was just unpaired until the process
                // restarts.
                BroadcastUtils.sendUpdateDeviceBroadcast(context)
            }

            val pairedDevices = deviceRepository.getAllDevices()
            val updatedConnectedSerialNumber = connectedSerialNumber()

            _uiState.update {
                it.copy(
                    // The mirror of onDeviceSelected: a device that is no longer paired is one the
                    // last scan found and nothing more, so it goes back under "Available devices"
                    // rather than disappearing until the next scan.
                    availableDevices = if (
                        forgottenDevice != null &&
                        it.availableDevices.none { available ->
                            available.serialNumber == serialNumber
                        }
                    ) {
                        (it.availableDevices + forgottenDevice).toPersistentList()
                    } else {
                        it.availableDevices
                    },
                    pairedDevices = pairedDevices.toPersistentList(),
                    connectedSerialNumber = updatedConnectedSerialNumber
                )
            }
        }
    }

    private fun onRenameDeviceClicked(event: MainScreenUiEvent.RenameDeviceClickedEvent) {
        _uiState.update {
            it.copy(
                renameTarget = MainScreenUiState.RenameTarget(
                    serialNumber = event.serialNumber,
                    currentName = event.currentName
                )
            )
        }
    }

    private fun onRenameDeviceDismissed() {
        _uiState.update { it.copy(renameTarget = null) }
    }

    private fun onRenameDeviceConfirmed(name: String) {
        val target = _uiState.value.renameTarget ?: return

        _uiState.update { it.copy(renameTarget = null) }

        viewModelScope.launch(ioDispatcher) {
            deviceRepository.getDevice(target.serialNumber)?.let { device ->
                device.setCustomUserDeviceName(name)
                deviceRepository.updateDevice(device)
                BroadcastUtils.sendUpdateDeviceBroadcast(context)
            }

            _uiState.update {
                it.copy(
                    availableDevices = persistentListOf(),
                    pairedDevices = deviceRepository.getAllDevices().toPersistentList()
                )
            }
        }
    }

    private companion object {
        const val TAG = "MainScreenViewModel"
    }
}
