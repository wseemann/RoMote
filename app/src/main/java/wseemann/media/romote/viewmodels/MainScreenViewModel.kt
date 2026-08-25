package wseemann.media.romote.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.wseemann.ecp.api.DeviceRequests
import com.wseemann.ecp.api.QueryRequests
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import wseemann.media.romote.event.MainScreenUiEvent
import wseemann.media.romote.model.Device
import wseemann.media.romote.model.Device.Companion.fromDevice
import wseemann.media.romote.model.MainScreenUiState
import wseemann.media.romote.utils.DBUtils
import wseemann.media.romote.utils.WifiApManager
import javax.inject.Inject

@HiltViewModel
class MainScreenViewModel @Inject constructor(
    @ApplicationContext val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainScreenUiState())
    val uiState = _uiState.asStateFlow()
    val uiStateLiveData = uiState.asLiveData()

    fun onHandleEvent(event: MainScreenUiEvent) {
        when (event) {
            is MainScreenUiEvent.LoadAvailableDevicesEvent -> onLoadAvailableDevices()
        }
    }

    private fun onLoadAvailableDevices() {
        viewModelScope.launch(Dispatchers.IO) {
            // Retrieve all stored Devices.
            val devices: MutableList<Device?> = DBUtils.getAllDevices(context)

            val availableDevices: MutableList<Device> = ArrayList()

            _uiState.update { it.copy(isLoading = true) }

            try {
                val rokuDevices: MutableList<Device> = ArrayList()

                val wifiApManager = WifiApManager(context)

                if (wifiApManager.isWifiApEnabled) {
                    // Scan the mobile access point for devices
                    rokuDevices.addAll(scanAccessPointForDevices())
                } else {
                    for (rokuDevice in DeviceRequests.discoverDevices()) {
                        rokuDevices.add(fromDevice(rokuDevice.queryDeviceInfo()))
                    }
                }

                for (device in rokuDevices) {
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
            } catch (ex: Exception) {
                Timber.e(ex)
                _uiState.update { it.copy(isLoading = false) }
            }
        }
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