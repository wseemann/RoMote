package wseemann.media.romote.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import wseemann.media.romote.data.Device
import wseemann.media.romote.data.DeviceDiscovery
import wseemann.media.romote.event.MainScreenUiEvent
import wseemann.media.romote.model.MainScreenUiState
import wseemann.media.romote.utils.BroadcastUtils
import wseemann.media.romote.utils.DBUtils
import wseemann.media.romote.utils.PreferenceUtils
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

@HiltViewModel
class MainScreenViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    val preferenceUtils: PreferenceUtils,
    private val deviceDiscovery: DeviceDiscovery
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainScreenUiState())
    val uiState = _uiState.asStateFlow()
    val uiStateLiveData = uiState.asLiveData()

    /**
     * Incremented for every scan. A scan only publishes its results if it is still the newest
     * one, so a slow scan that started before a device was forgotten can't overwrite the fresh
     * result with its own stale view of what was paired.
     */
    private val scanGeneration = AtomicInteger()

    fun onHandleEvent(event: MainScreenUiEvent) {
        when (event) {
            is MainScreenUiEvent.RefreshEvent -> onRefresh()
            is MainScreenUiEvent.LoadPairedDevicesEvent -> onLoadPairedDevices()
            is MainScreenUiEvent.ForgetDeviceEvent -> onForgetDevice(event.serialNumber)
            is MainScreenUiEvent.RenameDeviceClickedEvent -> onRenameDeviceClicked(event)
            is MainScreenUiEvent.RenameDeviceConfirmedEvent -> onRenameDeviceConfirmed(event.name)
            is MainScreenUiEvent.RenameDeviceDismissedEvent -> onRenameDeviceDismissed()
        }
    }

    private fun onRefresh() {
        val generation = scanGeneration.incrementAndGet()

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val discovered = deviceDiscovery.discoverDevices()

                // Read what is paired *after* the scan: a device forgotten while the scan was
                // running is no longer paired, and has to show up as available again.
                val pairedDevices = DBUtils.getAllDevices(context)
                val pairedSerialNumbers = pairedDevices.map { it.serialNumber }.toSet()

                if (generation != scanGeneration.get()) {
                    // Superseded; the newer scan owns the loading state and the results.
                    return@launch
                }

                refreshConnectedDevice(discovered)

                _uiState.update {
                    it.copy(
                        availableDevices = discovered
                            .filterNot { device -> device.serialNumber in pairedSerialNumbers }
                            .toPersistentList(),
                        pairedDevices = pairedDevices.toPersistentList(),
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
        val connectedSerialNumber = try {
            preferenceUtils.connectedDevice.serialNumber
        } catch (ex: Exception) {
            // getConnectedDevice() throws rather than returning null when nothing is paired.
            Timber.tag(TAG).d("No connected device to update")
            return
        }

        discovered.firstOrNull { it.serialNumber == connectedSerialNumber }?.let { device ->
            DBUtils.updateDevice(context, device)
            BroadcastUtils.sendUpdateDeviceBroadcast(context)
        }
    }

    private fun onLoadPairedDevices() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update {
                it.copy(pairedDevices = DBUtils.getAllDevices(context).toPersistentList())
            }
        }
    }

    /**
     * Unpairs a device. This used to run on the main thread from the fragment's popup menu, and
     * blanked the connected device whichever device was being forgotten - so forgetting a
     * secondary device silently disconnected the active one.
     */
    private fun onForgetDevice(serialNumber: String) {
        viewModelScope.launch(Dispatchers.IO) {
            DBUtils.removeDevice(context, serialNumber)

            val connectedSerialNumber = try {
                preferenceUtils.connectedDevice.serialNumber
            } catch (ex: Exception) {
                null
            }

            if (connectedSerialNumber == null || connectedSerialNumber == serialNumber) {
                preferenceUtils.setConnectedDevice("")
            }

            _uiState.update {
                it.copy(pairedDevices = DBUtils.getAllDevices(context).toPersistentList())
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

        viewModelScope.launch(Dispatchers.IO) {
            DBUtils.getDevice(context, target.serialNumber)?.let { device ->
                device.setCustomUserDeviceName(name)
                DBUtils.updateDevice(context, device)
                BroadcastUtils.sendUpdateDeviceBroadcast(context)
            }

            // The old listener cleared the available devices before reloading the paired ones,
            // because a renamed device is a paired one and has no business in both lists.
            _uiState.update {
                it.copy(
                    availableDevices = persistentListOf(),
                    pairedDevices = DBUtils.getAllDevices(context).toPersistentList()
                )
            }
        }
    }

    private companion object {
        const val TAG = "MainScreenViewModel"
    }
}
