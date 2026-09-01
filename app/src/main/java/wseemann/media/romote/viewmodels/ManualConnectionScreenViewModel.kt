package wseemann.media.romote.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import wseemann.media.romote.data.Device
import wseemann.media.romote.data.Device.Companion.fromDevice
import wseemann.media.romote.device.DeviceManager
import wseemann.media.romote.device.DeviceRepository
import wseemann.media.romote.di.IoDispatcher
import wseemann.media.romote.event.ManualConnectionScreenUiEvent
import wseemann.media.romote.model.ManualConnectionScreenUiState
import javax.inject.Inject
import wseemann.media.romote.device.Device as DeviceConnection

@HiltViewModel
class ManualConnectionScreenViewModel @Inject constructor(
    @param:ApplicationContext val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val deviceManager: DeviceManager,
    private val deviceRepository: DeviceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ManualConnectionScreenUiState())
    val uiState = _uiState.asStateFlow()

    fun onHandleEvent(event: ManualConnectionScreenUiEvent) {
        when (event) {
            is ManualConnectionScreenUiEvent.IpAddressChangedEvent -> onIpAddressChanged(event.ipAddress)
            is ManualConnectionScreenUiEvent.ConnectClickedEvent -> onConnectClicked()
        }
    }

    private fun onIpAddressChanged(ipAddress: String) {
        // Editing the address is the user's answer to the error, so retract it as they type.
        _uiState.update { it.copy(ipAddress = ipAddress, hasError = false) }
    }

    private fun onConnectClicked() {
        val deviceUrl = "http://" + _uiState.value.ipAddress + ":8060"

        _uiState.update { it.copy(isLoading = true, hasError = false) }

        viewModelScope.launch(ioDispatcher) {
            val device = DeviceConnection(
                Device().apply {
                    host = deviceUrl
                },
            )

            device.queryDeviceInfo()?.let { deviceInfo ->
                // The device only knows the host it was reached at because we tell it: the
                // response itself carries no address.
                deviceInfo.host = deviceUrl
                storeDevice(fromDevice(deviceInfo))
            } ?: onConnectionFailed()
        }
    }

    private fun onConnectionFailed() {
        _uiState.update { it.copy(isLoading = false, hasError = true) }
    }

    private fun storeDevice(device: Device) {
        deviceRepository.insertDevice(device)
        deviceManager.setConnectedDevice(device.serialNumber)

        _uiState.update { it.copy(isLoading = false, isConnected = true) }
    }
}
