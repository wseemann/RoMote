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
import wseemann.media.romote.utils.BroadcastUtils
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

@HiltViewModel
class MainScreenViewModel @Inject constructor(
    @param:ApplicationContext val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val deviceManager: DeviceManager,
    private val deviceRepository: DeviceRepository,
    private val deviceDiscovery: DeviceDiscovery
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainScreenUiState())
    val uiState = _uiState.asStateFlow()

    /**
     * Incremented for every scan. A scan only publishes its results if it is still the newest
     * one, so a slow scan that started before a device was forgotten can't overwrite the fresh
     * result with its own stale view of what was paired.
     */
    private val scanGeneration = AtomicInteger()

    init {
        // Nothing else kicks off the first scan. MainFragment used to do this from onCreate, behind
        // a guard against re-scanning after a rotation; the ViewModel is created once for the
        // screen either way, so the scan belongs here and the guard is no longer needed.
        onRefresh()
    }

    fun onHandleEvent(event: MainScreenUiEvent) {
        when (event) {
            is MainScreenUiEvent.RefreshEvent -> onRefresh()
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

    private fun onRefresh() {
        val generation = scanGeneration.incrementAndGet()

        viewModelScope.launch(ioDispatcher) {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val discovered = deviceDiscovery.discoverDevices()

                // Read what is paired *after* the scan: a device forgotten while the scan was
                // running is no longer paired, and has to show up as available again.
                val pairedDevices = deviceRepository.getAllDevices()
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
     * Refreshes the stored record of the connected device, in case its details (host, name, ...)
     * changed since it was paired. It reuses the scan the caller already ran rather than starting
     * a second one alongside it.
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
     * Gives paired devices the image the scan just found for them.
     *
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

    /**
     * Pairs with the tapped device and makes it the connected one. This ran on the main thread
     * from the list's click listener, database writes and all; the toast and the widget update it
     * also did stay with DevicesTab, which needs its own Context for them.
     */
    private fun onDeviceSelected(device: Device) {
        viewModelScope.launch(ioDispatcher) {
            deviceRepository.insertDevice(device)
            deviceManager.setConnectedDevice(device.serialNumber)

            BroadcastUtils.sendUpdateDeviceBroadcast(context)

            // A device that was just paired belongs under "Paired devices" and nowhere else, so
            // the available list is dropped rather than filtered - the next scan repopulates it.
            _uiState.update {
                it.copy(
                    availableDevices = persistentListOf(),
                    pairedDevices = deviceRepository.getAllDevices().toPersistentList(),
                    connectedSerialNumber = device.serialNumber
                )
            }
        }
    }

    /**
     * Unpairs a device. This used to run on the main thread from the fragment's popup menu, and
     * blanked the connected device whichever device was being forgotten - so forgetting a
     * secondary device silently disconnected the active one.
     */
    private fun onForgetDevice(serialNumber: String) {
        viewModelScope.launch(ioDispatcher) {
            deviceRepository.removeDevice(serialNumber)

            val connectedSerialNumber = connectedSerialNumber()

            if (connectedSerialNumber == null || connectedSerialNumber == serialNumber) {
                deviceManager.setConnectedDevice(null)

                // Nothing else announces this: onRefresh below reaches refreshConnectedDevice,
                // which now returns early because there is no connected device left to refresh.
                // Without the broadcast the remote and channels tabs keep showing the device that
                // was just unpaired until the process restarts.
                BroadcastUtils.sendUpdateDeviceBroadcast(context)
            }

            _uiState.update {
                it.copy(
                    pairedDevices = deviceRepository.getAllDevices().toPersistentList(),
                    connectedSerialNumber = connectedSerialNumber()
                )
            }

            // Only rescan once the row is gone, so the scan can see the device as unpaired.
            onRefresh()
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

    /**
     * Stores the name the rename dialog collected. EditDeviceNameDialog used to do this itself, on
     * the main thread, and tell the list to refresh through a listener a configuration change threw
     * away - so a rename confirmed after a rotation reached the database but never the list.
     */
    private fun onRenameDeviceConfirmed(name: String) {
        val target = _uiState.value.renameTarget ?: return

        _uiState.update { it.copy(renameTarget = null) }

        viewModelScope.launch(ioDispatcher) {
            deviceRepository.getDevice(target.serialNumber)?.let { device ->
                device.setCustomUserDeviceName(name)
                deviceRepository.updateDevice(device)
                BroadcastUtils.sendUpdateDeviceBroadcast(context)
            }

            // The old listener cleared the available devices before reloading the paired ones,
            // because a renamed device is a paired one and has no business in both lists.
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
