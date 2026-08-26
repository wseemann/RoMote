package wseemann.media.romote.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.wseemann.ecp.api.DeviceRequests
import com.wseemann.ecp.api.QueryRequests
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import wseemann.media.romote.event.MainScreenUiEvent
import wseemann.media.romote.data.Device
import wseemann.media.romote.data.Device.Companion.fromDevice
import wseemann.media.romote.model.MainScreenUiState
import wseemann.media.romote.utils.BroadcastUtils
import wseemann.media.romote.utils.DBUtils
import wseemann.media.romote.utils.PreferenceUtils
import wseemann.media.romote.utils.WifiApManager
import javax.inject.Inject

@HiltViewModel
class MainScreenViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    val preferenceUtils: PreferenceUtils
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainScreenUiState())
    val uiState = _uiState.asStateFlow()
    val uiStateLiveData = uiState.asLiveData()

    private var loadJob: Job? = null

    fun onHandleEvent(event: MainScreenUiEvent) {
        when (event) {
            is MainScreenUiEvent.LoadAvailableDevicesEvent -> onLoadAvailableDevices()
            is MainScreenUiEvent.LoadPairedDevicesEvent -> onLoadPairedDevices()
            is MainScreenUiEvent.UpdatePairedDeviceEvent -> onUpdatePairedDevice()
        }
    }

    private fun onLoadAvailableDevices() {
        // A new request supersedes any scan that is still running.
        loadJob?.cancel()
        loadJob = viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true) }

            // Retrieve all stored Devices.
            val devices: MutableList<Device?> = DBUtils.getAllDevices(context)

            val availableDevices: MutableList<Device> = ArrayList()

            try {
                for (device in discoverDevices()) {
                    var exists = false

                    for (j in devices.indices) {
                        if (devices[j]!!.serialNumber == device.serialNumber) {
                            exists = true
                            break
                        }
                    }

                    if (!exists) {
                        availableDevices.add(device)
                    }
                }

                _uiState.update {
                    it.copy(availableDevices = availableDevices.toPersistentList(), isLoading = false)
                }
            } catch (ex: CancellationException) {
                // Superseded by a newer scan, which owns the loading state now.
                throw ex
            } catch (ex: Exception) {
                Timber.e(ex)
                _uiState.update {
                    it.copy(availableDevices = persistentListOf(), isLoading = false)
                }
            }
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
     * Refreshes the stored record of the connected device, in case its details
     * (host, name, ...) changed since it was paired. Runs independently of the
     * available device scan so a refresh doesn't cancel it.
     */
    private fun onUpdatePairedDevice() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val connectedDevice = preferenceUtils.connectedDevice

                discoverDevices()
                    .firstOrNull { it.serialNumber == connectedDevice.serialNumber }
                    ?.let { device ->
                        DBUtils.updateDevice(context, device)
                        BroadcastUtils.sendUpdateDeviceBroadcast(context)
                    }
            } catch (ex: CancellationException) {
                throw ex
            } catch (ex: Exception) {
                Timber.tag(TAG).e(ex, "Device not found")
            }
        }
    }

    private fun discoverDevices(): List<Device> {
        val rokuDevices = ArrayList<Device>()

        val wifiApManager = WifiApManager(context)

        if (wifiApManager.isWifiApEnabled) {
            // Scan the mobile access point for devices
            rokuDevices.addAll(scanAccessPointForDevices())
        } else {
            for (rokuDevice in DeviceRequests.discoverDevices()) {
                rokuDevices.add(fromDevice(rokuDevice.queryDeviceInfo()))
            }
        }

        return rokuDevices
    }

    private fun scanAccessPointForDevices(): List<Device> {
        val availableDevices = ArrayList<Device>()

        val wifiApManager = WifiApManager(context)

        if (wifiApManager.isWifiApEnabled) {
            val clients = wifiApManager.getClientList(false, 3000)

            Timber.tag(TAG).d("Access point scan completed.")

            if (clients != null) {
                Timber.tag(TAG).d("Found %s connected devices.", clients.size)

                for (clientScanResult in clients) {
                    Timber.tag(TAG).d(
                        "Device: " + clientScanResult.getDevice() +
                                " HW Address: " + clientScanResult.getHWAddr() +
                                " IP Address:  " + clientScanResult.getIpAddr()
                    )

                    try {
                        val device =
                            fromDevice(QueryRequests.queryDeviceInfo("http://" + clientScanResult.getIpAddr() + ":8060"))
                        device.host = "http://" + clientScanResult.getIpAddr() + ":8060"
                        availableDevices.add(device)
                    } catch (ex: java.lang.Exception) {
                        Timber.tag(TAG).e("Invalid device: %s", ex.message)
                    }
                }
            }
        }

        return availableDevices
    }

    private companion object {
        const val TAG = "MainFragmentViewModel"
    }
}
